package br.com.casaemdia

import android.app.Application
import br.com.casaemdia.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class CasaEmDiaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@CasaEmDiaApp)
            modules(appModule)
        }
    }
}
