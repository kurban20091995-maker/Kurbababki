package ru.furniturecrm.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class SchedulerTest {
    private val workDays = setOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
    )

    @Test fun sequentialJobsAndHalfDayAreCalculated() {
        val result = Scheduler.calculate(listOf(
            Project(id=1,title="Кухня 1",clientName="A",durationHalfDays=4,queuePosition=1),
            Project(id=2,title="Кухня 2",clientName="B",durationHalfDays=6,queuePosition=2),
            Project(id=3,title="Шкаф",clientName="C",durationHalfDays=1,queuePosition=3),
        ), workDays, LocalDate.of(2026,8,31))
        assertEquals(LocalDate.of(2026,9,5), result.items[2].startDate)
        assertEquals(LocalDate.of(2026,9,5), result.nextFreeDate)
        assertEquals(1, result.nextFreeHalf)
    }

    @Test fun sundayIsSkipped() {
        val result = Scheduler.calculate(listOf(
            Project(id=1,title="Кухня",clientName="A",durationHalfDays=4,queuePosition=1),
        ), workDays, LocalDate.of(2026,9,5))
        assertEquals(LocalDate.of(2026,9,7), result.items.first().endDate)
    }

    @Test fun fixedFutureDateCreatesFreeWindow() {
        val result = Scheduler.calculate(listOf(
            Project(id=1,title="Кухня",clientName="A",plannedStart=LocalDate.of(2026,9,10),durationHalfDays=2,queuePosition=1),
        ), workDays, LocalDate.of(2026,9,1))
        assertTrue(result.gaps.isNotEmpty())
        assertEquals(LocalDate.of(2026,9,10), result.items.first().startDate)
    }

    @Test fun afternoonStartUsesSecondHalfOfDay() {
        val result = Scheduler.calculate(listOf(
            Project(id=1,title="Монтаж",clientName="A",plannedStart=LocalDate.of(2026,9,1),startTime="15:00",durationHalfDays=2,queuePosition=1),
        ), workDays, LocalDate.of(2026,9,1))
        val item = result.items.first()
        assertEquals(LocalDate.of(2026,9,1), item.startDate)
        assertEquals(1, item.startHalf)
        assertEquals(LocalDate.of(2026,9,2), item.endDate)
        assertEquals(0, item.endHalf)
    }

    @Test fun inProgressJobAlwaysComesFirst() {
        val result = Scheduler.calculate(listOf(
            Project(id=1,title="Очередь",clientName="A",durationHalfDays=2,queuePosition=1),
            Project(id=2,title="В работе",clientName="B",durationHalfDays=4,queuePosition=9,status=ProjectStatus.IN_PROGRESS,actualStart=LocalDate.of(2026,9,1)),
        ), workDays, LocalDate.of(2026,9,1))
        assertEquals(2L, result.items.first().projectId)
    }

    @Test fun inProgressAfternoonStartSubtractsOnlyWorkedHalfDay() {
        val result = Scheduler.calculate(listOf(
            Project(id=2,title="В работе",clientName="B",startTime="15:00",durationHalfDays=4,queuePosition=1,status=ProjectStatus.IN_PROGRESS,actualStart=LocalDate.of(2026,9,1)),
        ), workDays, LocalDate.of(2026,9,2))
        val item = result.items.first()
        assertEquals(LocalDate.of(2026,9,2), item.startDate)
        assertEquals(LocalDate.of(2026,9,3), item.endDate)
        assertEquals(0, item.endHalf)
    }

    @Test fun estimateNewJobUsesFreeGapBeforeFutureInstallation() {
        val result = Scheduler.calculate(listOf(
            Project(id=1,title="Будущий монтаж",clientName="A",plannedStart=LocalDate.of(2026,9,10),durationHalfDays=2,queuePosition=1),
        ), workDays, LocalDate.of(2026,9,1))
        val estimate = Scheduler.estimateNewJob(result, 4, workDays)
        assertEquals(LocalDate.of(2026,9,1), estimate.first)
        assertEquals(LocalDate.of(2026,9,2), estimate.second)
    }
}
