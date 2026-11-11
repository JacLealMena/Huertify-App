package com.duoc.huertify_app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.duoc.huertify_app.data.local.User
import com.duoc.huertify_app.di.AppContainer
import com.duoc.huertify_app.domain.model.Role
import com.duoc.huertify_app.ui.common.EmptyState
import com.duoc.huertify_app.ui.screens.activity.HistoryScreen
import com.duoc.huertify_app.ui.screens.activity.NotificationsScreen
import com.duoc.huertify_app.ui.screens.garden.CropFormScreen
import com.duoc.huertify_app.ui.screens.garden.SectorDetailScreen
import com.duoc.huertify_app.ui.screens.garden.SectorFormScreen
import com.duoc.huertify_app.ui.screens.garden.SectorListScreen
import com.duoc.huertify_app.ui.screens.home.HomeScreen
import com.duoc.huertify_app.ui.screens.login.LoginScreen
import com.duoc.huertify_app.ui.screens.reports.ReportFormScreen
import com.duoc.huertify_app.ui.screens.reports.ReportListScreen
import com.duoc.huertify_app.ui.screens.tasks.ReviewsScreen
import com.duoc.huertify_app.ui.screens.tasks.TaskDetailScreen
import com.duoc.huertify_app.ui.screens.tasks.TaskFormScreen
import com.duoc.huertify_app.ui.screens.tasks.TaskListScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object Routes {
    const val ARG_SECTOR_ID = "sectorId"
    const val ARG_CROP_ID = "cropId"
    const val ARG_TASK_ID = "taskId"

    const val LOGIN = "login"
    const val HOME = "home"
    const val SECTORS = "sectors"
    const val SECTOR_DETAIL = "sector/{$ARG_SECTOR_ID}"
    const val SECTOR_FORM = "sector_form?$ARG_SECTOR_ID={$ARG_SECTOR_ID}"
    const val CROP_FORM = "crop_form/{$ARG_SECTOR_ID}?$ARG_CROP_ID={$ARG_CROP_ID}"
    const val TASKS = "tasks"
    const val TASK_FORM = "task_form?$ARG_TASK_ID={$ARG_TASK_ID}"
    const val TASK_DETAIL = "task/{$ARG_TASK_ID}"
    const val REVIEWS = "reviews"
    const val REPORTS = "reports"
    const val REPORT_FORM = "report_form"
    const val HISTORY = "history"
    const val NOTIFICATIONS = "notifications"

    fun sectorDetail(id: Long) = "sector/$id"
    fun sectorForm(id: Long = 0) = "sector_form?$ARG_SECTOR_ID=$id"
    fun cropForm(sectorId: Long, cropId: Long = 0) = "crop_form/$sectorId?$ARG_CROP_ID=$cropId"
    fun taskForm(id: Long = 0) = "task_form?$ARG_TASK_ID=$id"
    fun taskDetail(id: Long) = "task/$id"
}

private fun longArg(name: String, optional: Boolean = false) = navArgument(name) {
    type = NavType.LongType
    if (optional) defaultValue = 0L
}

/**
 * Inicializa la app (datos semilla + sesión) una sola vez.
 * Al ser un ViewModel sobrevive a la rotación: el flag `ready` ya está en true cuando la
 * Activity se recrea, así el NavHost se compone en el primer frame y restaura su pila
 * (y con ella los ViewModels de cada pantalla). Con `remember` se volvía a false y se perdía todo.
 */
class AppViewModel(container: AppContainer) : ViewModel() {
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    init {
        viewModelScope.launch {
            container.initialize()
            _ready.value = true
        }
    }
}

@Composable
fun HuertifyNavHost(container: AppContainer) {
    val appVm: AppViewModel = viewModel(factory = viewModelFactory { initializer { AppViewModel(container) } })
    val ready by appVm.ready.collectAsStateWithLifecycle()
    if (!ready) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val user by container.authRepository.currentUser.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val start = remember { if (user == null) Routes.LOGIN else Routes.HOME }

    // Login/Logout: al cambiar la sesión se reinicia la pila de navegación.
    // Este efecto también se ejecuta al rotar (la Activity se recrea), así que solo se
    // redirige si la pantalla actual no corresponde a la sesión: sin usuario fuera del
    // login, o con usuario todavía en el login. Si no, se borraría la pila al girar.
    LaunchedEffect(user?.id) {
        val current = nav.currentDestination?.route ?: return@LaunchedEffect
        val onLogin = current == Routes.LOGIN
        val target = when {
            user == null && !onLogin -> Routes.LOGIN
            user != null && onLogin -> Routes.HOME
            else -> return@LaunchedEffect
        }
        nav.navigate(target) { popUpTo(0) { inclusive = true } }
    }

    val back: () -> Unit = { nav.popBackStack() }
    val go: (String) -> Unit = { nav.navigate(it) }

    NavHost(navController = nav, startDestination = start) {
        composable(Routes.LOGIN) { LoginScreen() }
        composable(Routes.HOME) { HomeScreen(go) }

        // Rutas exclusivas del administrador
        composable(Routes.SECTORS) { AdminOnly(user) { SectorListScreen(back, go) } }
        composable(Routes.SECTOR_FORM, listOf(longArg(Routes.ARG_SECTOR_ID, optional = true))) {
            AdminOnly(user) { SectorFormScreen(back) }
        }
        composable(Routes.SECTOR_DETAIL, listOf(longArg(Routes.ARG_SECTOR_ID))) {
            AdminOnly(user) { SectorDetailScreen(back, go) }
        }
        composable(Routes.CROP_FORM, listOf(longArg(Routes.ARG_SECTOR_ID), longArg(Routes.ARG_CROP_ID, optional = true))) {
            AdminOnly(user) { CropFormScreen(back) }
        }
        composable(Routes.TASKS) { AdminOnly(user) { TaskListScreen(back, go) } }
        composable(Routes.TASK_FORM, listOf(longArg(Routes.ARG_TASK_ID, optional = true))) {
            AdminOnly(user) { TaskFormScreen(back) }
        }
        composable(Routes.REVIEWS) { AdminOnly(user) { ReviewsScreen(back, go) } }
        composable(Routes.REPORTS) { AdminOnly(user) { ReportListScreen(back, go) } }

        // Rutas compartidas (el contenido se filtra según el rol)
        composable(Routes.TASK_DETAIL, listOf(longArg(Routes.ARG_TASK_ID))) { TaskDetailScreen(back, go) }
        composable(Routes.REPORT_FORM) { ReportFormScreen(back) }
        composable(Routes.HISTORY) { HistoryScreen(back) }
        composable(Routes.NOTIFICATIONS) { NotificationsScreen(back, go) }
    }
}

@Composable
private fun AdminOnly(user: User?, content: @Composable () -> Unit) {
    if (user?.role == Role.ADMIN) {
        content()
    } else {
        EmptyState(Icons.Filled.Block, "Acceso restringido.\nEsta sección es solo para administradores.")
    }
}
