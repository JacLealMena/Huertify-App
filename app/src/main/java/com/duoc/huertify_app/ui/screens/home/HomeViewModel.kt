package com.duoc.huertify_app.ui.screens.home

import androidx.lifecycle.viewModelScope
import com.duoc.huertify_app.data.repository.AuthRepository
import com.duoc.huertify_app.data.repository.GardenRepository
import com.duoc.huertify_app.data.repository.ReportRepository
import com.duoc.huertify_app.data.repository.TaskRepository
import com.duoc.huertify_app.data.repository.WeatherRepository
import com.duoc.huertify_app.domain.AlertBuilder
import com.duoc.huertify_app.domain.model.Role
import com.duoc.huertify_app.domain.model.TaskStatus
import com.duoc.huertify_app.ui.common.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val auth: AuthRepository,
    garden: GardenRepository,
    tasks: TaskRepository,
    reports: ReportRepository,
    private val weatherRepository: WeatherRepository
) : BaseViewModel() {

    private val _weather = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val weather: StateFlow<WeatherUiState> = _weather.asStateFlow()

    init {
        loadWeather()
    }

    fun loadWeather() {
        _weather.value = WeatherUiState.Loading
        viewModelScope.launch {
            _weather.value = weatherRepository.getForecast().fold(
                onSuccess = { WeatherUiState.Success(it) },
                onFailure = { WeatherUiState.Error(it.message ?: "No se pudo obtener el clima") }
            )
        }
    }

    val state: StateFlow<HomeUiState> = auth.currentUser.filterNotNull().flatMapLatest { user ->
        val taskFlow = if (user.role == Role.ADMIN) tasks.observeAll() else tasks.observeForUser(user.id)
        combine(
            taskFlow,
            garden.observeSectors(),
            garden.observeCropCount(),
            reports.observeAll()
        ) { taskList, sectors, crops, reportList ->
            HomeUiState(
                user = user,
                sectorCount = sectors.size,
                cropCount = crops,
                tasks = taskList,
                pendingValidation = taskList.count { it.task.status == TaskStatus.PENDIENTE_VALIDACION },
                overdue = taskList.count { it.task.isOverdue() },
                openReports = reportList.count { !it.report.resolved },
                alertCount = AlertBuilder.build(user.role, taskList, reportList).size
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun logout() = launchSafe { auth.logout() }
}
