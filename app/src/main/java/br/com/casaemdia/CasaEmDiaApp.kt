package br.com.casaemdia

import android.app.Application
import br.com.casaemdia.data.remote.FirestoreSync
import br.com.casaemdia.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class CasaEmDiaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@CasaEmDiaApp)
            modules(appModule)
        }
        // Espelho Firestore → Room: segue o login e mantém a cópia local atualizada.
        GlobalContext.get().get<FirestoreSync>().start()
    }
}
