package br.com.casaemdia.testing

import br.com.casaemdia.domain.model.Category
import br.com.casaemdia.domain.model.InventoryItem
import br.com.casaemdia.domain.model.ShoppingItem
import br.com.casaemdia.domain.model.UnitType
import br.com.casaemdia.domain.repository.InventoryRepository
import br.com.casaemdia.domain.repository.ShoppingRepository
import br.com.casaemdia.domain.repository.TransactionRunner
import java.io.IOException
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

val TODAY: LocalDate = LocalDate.of(2025, 1, 10)

val FIXED_CLOCK: Clock = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

fun inventoryItem(
    id: Long = 1,
    name: String = "Arroz",
    category: Category = Category.FOOD,
    quantity: Double = 5.0,
    unit: UnitType = UnitType.KG,
    min: Double = 1.0,
    expires: LocalDate? = null,
    notes: String = "",
) = InventoryItem(
    id = id, name = name, category = category, quantity = quantity,
    unit = unit, minQuantity = min, expirationDate = expires, notes = notes,
)

/** Replaces Dispatchers.Main so viewModelScope runs on a test dispatcher. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

private fun <T> failingFlow(): Flow<T> = flow { throw IOException("db error") }

class FakeInventoryRepository(initial: List<InventoryItem> = emptyList()) : InventoryRepository {
    val items = MutableStateFlow(initial)
    var failObserve = false
    var failWrites = false
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0) + 1

    override fun observeItems(): Flow<List<InventoryItem>> = if (failObserve) failingFlow() else items

    override fun observeItem(id: Long): Flow<InventoryItem?> =
        if (failObserve) failingFlow() else items.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun getItem(id: Long): InventoryItem? = items.value.firstOrNull { it.id == id }

    override suspend fun save(item: InventoryItem): Long {
        if (failWrites) throw IOException("write error")
        val stored = if (item.id == 0L) item.copy(id = nextId++) else item
        items.value = items.value.filter { it.id != stored.id } + stored
        return stored.id
    }

    override suspend fun delete(id: Long) {
        if (failWrites) throw IOException("write error")
        items.value = items.value.filter { it.id != id }
    }
}

class FakeShoppingRepository(initial: List<ShoppingItem> = emptyList()) : ShoppingRepository {
    val items = MutableStateFlow(initial)
    var failWrites = false
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0) + 1

    override fun observeItems(): Flow<List<ShoppingItem>> = items

    override suspend fun getItem(id: Long): ShoppingItem? = items.value.firstOrNull { it.id == id }

    override suspend fun add(item: ShoppingItem): Long {
        if (failWrites) throw IOException("write error")
        val stored = if (item.id == 0L) item.copy(id = nextId++) else item
        items.value = items.value.filter { it.id != stored.id } + stored
        return stored.id
    }

    override suspend fun setPurchased(id: Long, purchased: Boolean) {
        if (failWrites) throw IOException("write error")
        items.value = items.value.map { if (it.id == id) it.copy(purchased = purchased) else it }
    }

    override suspend fun delete(id: Long) {
        if (failWrites) throw IOException("write error")
        items.value = items.value.filter { it.id != id }
    }

    override suspend fun clearPurchased() {
        if (failWrites) throw IOException("write error")
        items.value = items.value.filter { !it.purchased }
    }
}

/** Runs the block directly; the in-memory fakes need no real transaction. */
object ImmediateTransactionRunner : TransactionRunner {
    override suspend fun <R> run(block: suspend () -> R): R = block()
}

class FakeAuthRepository(
    initialState: br.com.casaemdia.domain.model.AuthState = br.com.casaemdia.domain.model.AuthState.Unauthenticated
) : br.com.casaemdia.domain.repository.AuthRepository {
    val authState = MutableStateFlow(initialState)
    var shouldFailSignIn = false

    override fun observeAuthState(): Flow<br.com.casaemdia.domain.model.AuthState> = authState

    override suspend fun signInWithGoogle(
        provider: br.com.casaemdia.domain.repository.GoogleCredentialProvider,
    ): Result<br.com.casaemdia.domain.model.UserSession> {
        return if (shouldFailSignIn) {
            Result.failure(RuntimeException("Erro ao realizar login"))
        } else {
            val user = br.com.casaemdia.domain.model.UserSession(
                id = "user_123",
                email = "teste@casaemdia.com",
                displayName = "Usuário Teste"
            )
            authState.value = br.com.casaemdia.domain.model.AuthState.Authenticated(user)
            Result.success(user)
        }
    }

    override suspend fun signOut() {
        authState.value = br.com.casaemdia.domain.model.AuthState.Unauthenticated
    }
}
