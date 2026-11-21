package com.duoc.huertify_app

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Prueba instrumentada de ejemplo (plantilla de Android Studio): se ejecuta en un
 * dispositivo o emulador Android.
 *
 * Ver la [documentación de pruebas](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Contexto de la app que se está probando.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.duoc.huertify_app", appContext.packageName)
    }
}