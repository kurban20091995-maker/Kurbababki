package ru.furniturecrm.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek

data class AppUiState(
    val projects: List<ProjectSummary> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val schedule: ScheduleResult = Scheduler.calculate(emptyList(), AppSettings().workDays),
    val finance: FinanceStats = FinanceStats(),
    val selected: ProjectDetails? = null,
    val loading: Boolean = true,
    val message: String? = null,
)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = CrmRepository(app)
    private val _state = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh(selectedId: Long? = _state.value.selected?.summary?.project?.id) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            runCatching {
                withContext(Dispatchers.IO) {
                    val settings = repo.getSettings()
                    val projects = repo.listProjects()
                    val schedule = Scheduler.calculate(projects.map { it.project }, settings.workDays)
                    val finance = repo.getFinanceStats()
                    val selected = selectedId?.let(repo::getProjectDetails)
                    Five(projects, settings, schedule, finance, selected)
                }
            }.onSuccess { r ->
                _state.value = AppUiState(r.a, r.b, r.c, r.d, r.e, loading = false)
            }.onFailure { e ->
                _state.value = _state.value.copy(loading = false, message = e.message ?: "Ошибка")
            }
        }
    }

    fun selectProject(id: Long) = refresh(id)
    fun clearSelected() { _state.value = _state.value.copy(selected = null) }
    fun consumeMessage() { _state.value = _state.value.copy(message = null) }

    fun saveProject(project: Project, onSaved: (Long) -> Unit = {}) = mutate {
        val id = repo.saveProject(project)
        withContext(Dispatchers.Main) { onSaved(id) }
    }

    fun deleteProject(id: Long, onDone: () -> Unit = {}) = mutate {
        repo.deleteProject(id); withContext(Dispatchers.Main) { onDone() }
    }
    fun startProject(id: Long) = mutate { repo.startProject(id) }
    fun completeProject(id: Long, finalBasePriceCents: Long? = null) = mutate { repo.completeProject(id, finalBasePriceCents) }
    fun completeProjectWithPayment(id: Long, finalBasePriceCents: Long?, paymentCents: Long, method: PaymentMethod, comment: String) = mutate { repo.completeProjectWithPayment(id, finalBasePriceCents, paymentCents, method, comment) }
    fun addPayment(id: Long, amount: Long, method: PaymentMethod, comment: String) = mutate { repo.addPayment(id, amount, method, comment) }
    fun addExpense(id: Long, amount: Long, category: String, comment: String) = mutate { repo.addExpense(id, amount, category, comment) }
    fun addExtra(id: Long, name: String, price: Long) = mutate { repo.addExtra(id, name, price) }
    fun addChecklist(id: Long, text: String) = mutate { repo.addChecklist(id, text) }
    fun toggleChecklist(id: Long, done: Boolean) = mutate { repo.toggleChecklist(id, done) }
    fun addAttachment(id: Long, uri: String, name: String, mimeType: String = "") = mutate { repo.addAttachment(id, uri, name, mimeType) }
    fun removeExtra(id: Long) = mutate { repo.removeExtra(id) }
    fun removePayment(id: Long) = mutate { repo.removePayment(id) }
    fun removeExpense(id: Long) = mutate { repo.removeExpense(id) }
    fun removeAttachment(id: Long) = mutate { repo.removeAttachment(id) }
    fun moveProject(id: Long, delta: Int) = mutate { repo.moveProject(id, delta) }
    fun setWorkDays(days: Set<DayOfWeek>) = mutate { repo.setWorkDays(days) }
    fun setReminderDays(days: Set<Int>) = mutate { repo.setReminderDays(days) }
    fun seedDemo() = mutate { repo.seedDemo() }

    suspend fun exportJson(): String = withContext(Dispatchers.IO) { repo.exportJson() }
    fun importJson(json: String) = mutate { repo.importJson(json) }

    private fun mutate(block: suspend () -> Unit) {
        val selectedId = _state.value.selected?.summary?.project?.id
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { block() } }
                .onSuccess { refresh(selectedId) }
                .onFailure { e -> _state.value = _state.value.copy(message = e.message ?: "Не удалось выполнить действие") }
        }
    }

    private data class Five<A,B,C,D,E>(val a:A,val b:B,val c:C,val d:D,val e:E)
}
