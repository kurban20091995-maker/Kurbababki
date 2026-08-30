package ru.furniturecrm.app

import java.time.DayOfWeek
import java.time.LocalDate

enum class FurnitureType(val label: String) {
    KITCHEN("Кухня"), WARDROBE("Шкаф"), DRESSING_ROOM("Гардеробная"),
    HALLWAY("Прихожая"), BEDROOM("Спальня"), CHILDREN("Детская"),
    BATHROOM("Мебель в ванную"), OFFICE("Офисная мебель"), OTHER("Другое")
}

enum class ProjectStatus(val label: String) {
    NEW("Новый"), QUEUED("В очереди"), DATE_CONFIRMED("Дата согласована"),
    SOON("Скоро установка"), IN_PROGRESS("В работе"), PAUSED("Приостановлен"),
    COMPLETED("Завершён"), CANCELLED("Отменён")
}

enum class PaymentMethod(val label: String) { CASH("Наличные"), TRANSFER("Перевод"), OTHER("Другое") }

data class Project(
    val id: Long = 0,
    val title: String,
    val clientName: String,
    val phone: String = "",
    val address: String = "",
    val furnitureType: FurnitureType = FurnitureType.KITCHEN,
    val customType: String = "",
    val basePriceCents: Long = 0,
    val plannedStart: LocalDate? = null,
    val startTime: String = "09:00",
    val durationHalfDays: Int = 2,
    val status: ProjectStatus = ProjectStatus.QUEUED,
    val notes: String = "",
    val queuePosition: Int = 0,
    val actualStart: LocalDate? = null,
    val actualFinish: LocalDate? = null,
)

data class ProjectSummary(
    val project: Project,
    val totalPriceCents: Long,
    val paidCents: Long,
    val expenseCents: Long,
    val photoCount: Int = 0,
) {
    val debtCents: Long get() = (totalPriceCents - paidCents).coerceAtLeast(0)
    val netCents: Long get() = paidCents - expenseCents
}

data class Payment(val id: Long, val projectId: Long, val amountCents: Long, val date: LocalDate, val method: PaymentMethod, val comment: String)
data class Expense(val id: Long, val projectId: Long, val amountCents: Long, val date: LocalDate, val category: String, val comment: String)
data class ExtraWork(val id: Long, val projectId: Long, val name: String, val priceCents: Long)
data class ChecklistItem(val id: Long, val projectId: Long, val text: String, val done: Boolean)
data class Attachment(
    val id: Long,
    val projectId: Long,
    val uri: String,
    val name: String,
    val mimeType: String = "",
    val createdAt: String = "",
) {
    val isImage: Boolean get() = mimeType.startsWith("image/") || name.lowercase().let { it.endsWith(".jpg") || it.endsWith(".jpeg") || it.endsWith(".png") || it.endsWith(".webp") || it.endsWith(".heic") }
}

data class ProjectDetails(
    val summary: ProjectSummary,
    val payments: List<Payment>,
    val expenses: List<Expense>,
    val extras: List<ExtraWork>,
    val checklist: List<ChecklistItem>,
    val attachments: List<Attachment>,
)

data class ScheduledProject(
    val projectId: Long,
    val title: String,
    val furnitureType: FurnitureType,
    val startDate: LocalDate,
    val startHalf: Int,
    val endDate: LocalDate,
    val endHalf: Int,
    val delayedFromFixedDateByDays: Long = 0,
)

data class FreeWindow(val startDate: LocalDate, val startHalf: Int, val endDate: LocalDate, val endHalf: Int)

data class ScheduleResult(
    val items: List<ScheduledProject>,
    val nextFreeDate: LocalDate,
    val nextFreeHalf: Int,
    val totalHalfDays: Int,
    val gaps: List<FreeWindow>,
)

data class FinanceStats(
    val todayReceivedCents: Long = 0,
    val weekReceivedCents: Long = 0,
    val monthReceivedCents: Long = 0,
    val yearReceivedCents: Long = 0,
    val allReceivedCents: Long = 0,
    val allExpensesCents: Long = 0,
    val totalDebtCents: Long = 0,
    val expectedActiveCents: Long = 0,
    val completedCount: Int = 0,
    val completedKitchens: Int = 0,
    val averageKitchenActualDays: Double = 0.0,
)

data class AppSettings(
    val workDays: Set<DayOfWeek> = setOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
    ),
    val reminderDays: Set<Int> = setOf(7, 3, 1, 0),
)

fun Long.asRubles(): String = "%,d ₽".format(this / 100).replace(',', ' ')
fun Double.toHalfDays(): Int = (this * 2.0).toInt().coerceAtLeast(1)
fun Int.halfDaysLabel(): String = if (this % 2 == 0) "${this / 2} дн." else if (this == 1) "0,5 дня" else "${this / 2},5 дн."
