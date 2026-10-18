package com.duoc.huertify_app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duoc.huertify_app.ui.screens.login.LoginScreen
import com.duoc.huertify_app.ui.theme.Huertify_AppTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val rule = createComposeRule()

    @Before
    fun setUp() {
        rule.setContent { Huertify_AppTheme { LoginScreen() } }
    }

    @Test
    fun emptyFormShowsFieldErrors() {
        rule.onNodeWithText("Iniciar sesión").performClick()
        rule.onNodeWithText("El correo es obligatorio").assertIsDisplayed()
        rule.onNodeWithText("La contraseña es obligatoria").assertIsDisplayed()
        rule.onNodeWithText("⚠ Revisa los campos marcados").assertIsDisplayed()
    }

    @Test
    fun invalidEmailFormatAndShortPassword() {
        rule.onNodeWithText("Correo electrónico").performTextInput("docente@")
        rule.onNodeWithText("Contraseña").performTextInput("123")
        rule.onNodeWithText("Iniciar sesión").performClick()
        rule.onNodeWithText("Formato de correo inválido (ej: nombre@huertify.cl)").assertIsDisplayed()
        rule.onNodeWithText("La contraseña debe tener al menos 6 caracteres").assertIsDisplayed()
    }

    @Test
    fun errorDisappearsWhenUserTypes() {
        rule.onNodeWithText("Iniciar sesión").performClick()
        rule.onNodeWithText("El correo es obligatorio").assertIsDisplayed()
        rule.onNodeWithText("Correo electrónico").performTextInput("a")
        rule.onNodeWithText("El correo es obligatorio").assertDoesNotExist()
    }
}
