package com.duoc.huertify_app.ui

import android.content.Context
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.onFirst
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duoc.huertify_app.MainActivity
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifica que el estado sobrevive a un cambio de configuración (rotación).
 * `scenario.recreate()` destruye y vuelve a crear la Activity, igual que al girar el teléfono.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class RotationTest {

    @get:Rule
    val rule = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        // Partimos siempre sin sesión guardada.
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("huertify_session", Context.MODE_PRIVATE)
            .edit().clear().commit()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        loginAsAdmin()
    }

    @After
    fun tearDown() = scenario.close()

    private fun waitForText(text: String) = rule.waitUntil(10_000) {
        rule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()
    }

    private fun loginAsAdmin() {
        waitForText("docente@huertify.cl / admin123")
        rule.onNodeWithText("docente@huertify.cl / admin123").performClick()
        rule.onNodeWithText("Iniciar sesión").performClick()
        waitForText("Panel docente")
    }

    private fun openSectorDetail() {
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Sectores y cultivos"))
        rule.onNodeWithText("Sectores y cultivos").performClick()
        waitForText("Bancal Norte")
        rule.onNodeWithText("Bancal Norte").performClick()
        waitForText("Cultivos (2)")
    }

    private fun rotate() {
        scenario.recreate()
        rule.waitForIdle()
    }

    @Test
    fun navigationBackStackSurvivesRotation() {
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Sectores y cultivos"))
        rule.onNodeWithText("Sectores y cultivos").performClick()
        waitForText("Sectores del huerto")

        rotate()

        // Antes del arreglo la app volvía a "Panel docente" y se perdía la pila.
        waitForText("Sectores del huerto")
        rule.onNodeWithText("Sectores del huerto").assertIsDisplayed()
    }

    @Test
    fun deleteSectorDialogSurvivesRotation() {
        openSectorDetail()
        rule.onNodeWithContentDescription("Eliminar sector").performClick()
        rule.onNodeWithText("Eliminar sector").assertIsDisplayed()

        rotate()

        waitForText("Eliminar sector")
        rule.onNodeWithText("Eliminar sector").assertIsDisplayed()
        rule.onNodeWithText("Cancelar").performClick()
    }

    @Test
    fun deleteCropDialogSurvivesRotation() {
        openSectorDetail()
        rule.onAllNodesWithContentDescription("Eliminar cultivo").onFirst().performClick()
        rule.onNodeWithText("Eliminar cultivo").assertIsDisplayed()

        rotate()

        waitForText("Eliminar cultivo")
        rule.onNodeWithText("Eliminar cultivo").assertIsDisplayed()
        rule.onNodeWithText("Cancelar").performClick()
        rule.onNodeWithText("Cultivos (2)").assertIsDisplayed()
    }
}
