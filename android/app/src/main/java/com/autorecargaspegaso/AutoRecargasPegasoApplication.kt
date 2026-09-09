package com.autorecargaspegaso

import android.app.Application

class AutoRecargasPegasoApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
