package com.duoc.huertify_app

import android.app.Application
import com.duoc.huertify_app.di.AppContainer
import java.util.Locale

class HuertifyApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    companion object {
        /** Idioma de la app: español de Chile. */
        val SPANISH: Locale = Locale.forLanguageTag("es-CL")
    }
}
