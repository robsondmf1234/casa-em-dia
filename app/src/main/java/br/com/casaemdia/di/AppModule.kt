package br.com.casaemdia.di

import android.content.Context
import androidx.room.Room
import br.com.casaemdia.data.local.AppDatabase
import br.com.casaemdia.data.local.MIGRATION_1_2
import br.com.casaemdia.data.local.MIGRATION_2_3
import br.com.casaemdia.data.remote.FirestoreDataSource
import br.com.casaemdia.data.remote.FirestoreSync
import br.com.casaemdia.data.repository.AuthRepositoryImpl
import br.com.casaemdia.data.repository.CredentialManagerGoogleProvider
import br.com.casaemdia.data.repository.InventoryRepositoryImpl
import br.com.casaemdia.data.repository.SettingsRepositoryImpl
import br.com.casaemdia.data.repository.ShoppingRepositoryImpl
import br.com.casaemdia.domain.repository.AuthRepository
import br.com.casaemdia.domain.repository.GoogleCredentialProvider
import br.com.casaemdia.domain.repository.InventoryRepository
import br.com.casaemdia.domain.repository.SettingsRepository
import br.com.casaemdia.domain.repository.ShoppingRepository
import br.com.casaemdia.domain.usecase.AddSuggestionToShoppingList
import br.com.casaemdia.domain.usecase.SetItemPurchased
import br.com.casaemdia.ui.viewmodel.AuthViewModel
import br.com.casaemdia.ui.viewmodel.EditItemViewModel
import br.com.casaemdia.ui.viewmodel.HomeViewModel
import br.com.casaemdia.ui.viewmodel.InventoryViewModel
import br.com.casaemdia.ui.viewmodel.ItemDetailsViewModel
import br.com.casaemdia.ui.viewmodel.SettingsViewModel
import br.com.casaemdia.ui.viewmodel.ShoppingViewModel
import br.com.casaemdia.ui.navigation.Routes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, "casa_em_dia.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
    }
    single { get<AppDatabase>().inventoryDao() }
    single { get<AppDatabase>().shoppingDao() }
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }
    single { FirestoreDataSource(get(), get()) }
    single { FirestoreSync(get(), get(), get(), CoroutineScope(SupervisorJob() + Dispatchers.IO)) }
    single<InventoryRepository> { InventoryRepositoryImpl(get(), get(), get()) }
    single<ShoppingRepository> { ShoppingRepositoryImpl(get(), get()) }
    single<AuthRepository> { AuthRepositoryImpl(get()) }
    factory<GoogleCredentialProvider> { params -> CredentialManagerGoogleProvider(params.get()) }
    single<SettingsRepository> {
        SettingsRepositoryImpl(androidContext().getSharedPreferences(SettingsRepositoryImpl.PREFS_NAME, Context.MODE_PRIVATE))
    }
    single<Clock> { Clock.systemDefaultZone() }
    factory { AddSuggestionToShoppingList(get()) }
    factory { SetItemPurchased(get(), get()) }

    viewModel { AuthViewModel(get()) }
    viewModel { HomeViewModel(get(), get()) }
    viewModel { InventoryViewModel(get(), get()) }
    viewModel { params -> EditItemViewModel(params.get<String>().takeIf { it != Routes.NEW_ITEM_ID }, get(), get()) }
    viewModel { params -> ItemDetailsViewModel(params.get<String>(), get(), get(), get()) }
    viewModel { ShoppingViewModel(get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get()) }
}
