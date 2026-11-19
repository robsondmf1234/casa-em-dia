package br.com.casaemdia.di

import androidx.room.Room
import br.com.casaemdia.data.local.AppDatabase
import br.com.casaemdia.data.local.MIGRATION_1_2
import br.com.casaemdia.data.repository.AuthRepositoryImpl
import br.com.casaemdia.data.repository.InventoryRepositoryImpl
import br.com.casaemdia.data.repository.ShoppingRepositoryImpl
import br.com.casaemdia.domain.repository.AuthRepository
import br.com.casaemdia.domain.repository.InventoryRepository
import br.com.casaemdia.domain.repository.ShoppingRepository
import br.com.casaemdia.domain.usecase.AddSuggestionToShoppingList
import br.com.casaemdia.domain.usecase.SetItemPurchased
import br.com.casaemdia.ui.viewmodel.AuthViewModel
import br.com.casaemdia.ui.viewmodel.EditItemViewModel
import br.com.casaemdia.ui.viewmodel.HomeViewModel
import br.com.casaemdia.ui.viewmodel.InventoryViewModel
import br.com.casaemdia.ui.viewmodel.ItemDetailsViewModel
import br.com.casaemdia.ui.viewmodel.ShoppingViewModel
import java.time.Clock
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, "casa_em_dia.db")
            .addMigrations(MIGRATION_1_2)
            .build()
    }
    single { get<AppDatabase>().inventoryDao() }
    single { get<AppDatabase>().shoppingDao() }
    single<InventoryRepository> { InventoryRepositoryImpl(get()) }
    single<ShoppingRepository> { ShoppingRepositoryImpl(get()) }
    single<AuthRepository> { AuthRepositoryImpl() }
    single<Clock> { Clock.systemDefaultZone() }
    factory { AddSuggestionToShoppingList(get()) }
    factory { SetItemPurchased(get(), get()) }

    viewModel { AuthViewModel(get()) }
    viewModel { HomeViewModel(get(), get()) }
    viewModel { InventoryViewModel(get(), get()) }
    viewModel { params -> EditItemViewModel(params.get<Long>().takeIf { it >= 0 }, get(), get()) }
    viewModel { params -> ItemDetailsViewModel(params.get<Long>(), get(), get(), get()) }
    viewModel { ShoppingViewModel(get(), get(), get(), get()) }
}
