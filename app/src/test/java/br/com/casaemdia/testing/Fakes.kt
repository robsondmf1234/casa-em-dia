package br.com.casaemdia.testing

import br.com.casaemdia.domain.model.Category
import br.com.casaemdia.domain.model.InventoryItem
import br.com.casaemdia.domain.model.ShoppingItem
import br.com.casaemdia.domain.model.UnitType
import br.com.casaemdia.domain.repository.InventoryRepository
import br.com.casaemdia.domain.repository.ShoppingRepository
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
    id: String = "1",
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
    private var nextId = initial.size + 1

    override fun observeItems(): Flow<List<InventoryItem>> = if (failObserve) failingFlow() else items

    override fun observeItem(id: String): Flow<InventoryItem?> =
        if (failObserve) failingFlow() else items.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun getItem(id: String): InventoryItem? = items.value.firstOrNull { it.id == id }

    override suspend fun save(item: InventoryItem): String {
        if (failWrites) throw IOException("write error")
        val stored = if (item.id.isEmpty()) item.copy(id = "inv-${nextId++}") else item
        items.value = items.value.filter { it.id != stored.id } + stored
        return stored.id
    }

    override suspend fun delete(id: String) {
        if (failWrites) throw IOException("write error")
        items.value = items.value.filter { it.id != id }
    }

    /** Usado pelo [FakeShoppingRepository] para simular o incremento atômico do Firestore. */
    fun addToQuantity(id: String, delta: Double) {
        items.value = items.value.map { if (it.id == id) it.copy(quantity = it.quantity + delta) else it }
    }
}

/**
 * Lista de compras em memória. Com [inventory], [setPurchased] também aplica o ajuste de estoque,
 * como o lote do Firestore faz em produção.
 */
class FakeShoppingRepository(
    initial: List<ShoppingItem> = emptyList(),
    private val inventory: FakeInventoryRepository? = null,
) : ShoppingRepository {
    val items = MutableStateFlow(initial)
    var failWrites = false
    var setPurchasedCalls = 0
        private set
    private var nextId = initial.size + 1

    override fun observeItems(): Flow<List<ShoppingItem>> = items

    override suspend fun getItem(id: String): ShoppingItem? = items.value.firstOrNull { it.id == id }

    override suspend fun add(item: ShoppingItem): String {
        if (failWrites) throw IOException("write error")
        val stored = if (item.id.isEmpty()) item.copy(id = "shop-${nextId++}") else item
        items.value = items.value.filter { it.id != stored.id } + stored
        return stored.id
    }

    override suspend fun setPurchased(id: String, purchased: Boolean, inventoryItemId: String?, stockDelta: Double) {
        if (failWrites) throw IOException("write error")
        setPurchasedCalls++
        items.value = items.value.map { if (it.id == id) it.copy(purchased = purchased) else it }
        if (inventoryItemId != null) inventory?.addToQuantity(inventoryItemId, stockDelta)
    }

    override suspend fun delete(id: String) {
        if (failWrites) throw IOException("write error")
        items.value = items.value.filter { it.id != id }
    }

    override suspend fun clearPurchased() {
        if (failWrites) throw IOException("write error")
        items.value = items.value.filter { !it.purchased }
    }
}

class FakeAuthRepository(
    initialState: br.com.casaemdia.domain.model.AuthState = br.com.casaemdia.domain.model.AuthState.Unauthenticated,
) : br.com.casaemdia.domain.repository.AuthRepository {
    private val state = MutableStateFlow(initialState)
    override val authState: kotlinx.coroutines.flow.StateFlow<br.com.casaemdia.domain.model.AuthState> = state

    /** Erro devolvido no próximo login; null = sucesso. */
    var signInError: Throwable? = null

    override suspend fun signInWithGoogle(
        provider: br.com.casaemdia.domain.repository.GoogleCredentialProvider,
    ): Result<br.com.casaemdia.domain.model.UserSession> {
        signInError?.let { return Result.failure(it) }
        val user = br.com.casaemdia.domain.model.UserSession(
            id = "user_123",
            email = "teste@casaemdia.com",
            displayName = "Usuário Teste",
        )
        state.value = br.com.casaemdia.domain.model.AuthState.Authenticated(user)
        return Result.success(user)
    }

    override suspend fun signOut() {
        state.value = br.com.casaemdia.domain.model.AuthState.Unauthenticated
    }
}

class FakeSettingsRepository(
    initial: br.com.casaemdia.domain.model.ThemeMode = br.com.casaemdia.domain.model.ThemeMode.SYSTEM,
) : br.com.casaemdia.domain.repository.SettingsRepository {
    private val state = MutableStateFlow(initial)
    override val themeMode: kotlinx.coroutines.flow.StateFlow<br.com.casaemdia.domain.model.ThemeMode> = state

    override suspend fun setThemeMode(mode: br.com.casaemdia.domain.model.ThemeMode) {
        state.value = mode
    }
}
