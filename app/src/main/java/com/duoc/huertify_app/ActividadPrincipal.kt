package com.duoc.huertify_app

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.duoc.huertify_app.ui.navigation.HuertifyNavHost
import com.duoc.huertify_app.ui.theme.Huertify_AppTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    // La app siempre se muestra en español, aunque el teléfono esté en otro idioma
    // (afecta, por ejemplo, al calendario del selector de fechas).
    override fun attachBaseContext(newBase: Context) {
        Locale.setDefault(HuertifyApp.SPANISH) // formatos de números (ej: 4,9 mm)
        val config = Configuration(newBase.resources.configuration).apply { setLocale(HuertifyApp.SPANISH) }
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as HuertifyApp).container
        setContent {
            Huertify_AppTheme {
                HuertifyNavHost(container)
            }
        }
    }
}
