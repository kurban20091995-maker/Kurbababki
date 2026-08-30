package ru.furniturecrm.app

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

object Scheduler {
    private data class Slot(val date: LocalDate, val half: Int)

    fun calculate(
        projects: List<Project>,
        workDays: Set<DayOfWeek>,
        from: LocalDate = LocalDate.now(),
    ): ScheduleResult {
        val active = projects
            .filter { it.status !in setOf(ProjectStatus.COMPLETED, ProjectStatus.CANCELLED) }
            .map { p ->
                if (p.status == ProjectStatus.IN_PROGRESS && p.actualStart != null && p.actualStart.isBefore(from)) {
                    val elapsedHalfDays = countElapsedHalfSlots(
                        startDate = p.actualStart,
                        startHalf = startHalfFromTime(p.startTime),
                        untilDateExclusive = from,
                        workDays = workDays,
                    )
                    p.copy(
                        durationHalfDays = (p.durationHalfDays - elapsedHalfDays).coerceAtLeast(1),
                        plannedStart = from,
                    )
                } else p
            }
            .sortedWith(
                compareBy<Project> { if (it.status == ProjectStatus.IN_PROGRESS) 0 else 1 }
                    .thenBy { it.queuePosition }
                    .thenBy { it.id }
            )

        if (active.isEmpty()) {
            val d = nextWorkDay(from, workDays)
            return ScheduleResult(emptyList(), d, 0, 0, emptyList())
        }

        var cursor = Slot(nextWorkDay(from, workDays), 0)
        val items = mutableListOf<ScheduledProject>()
        val gaps = mutableListOf<FreeWindow>()
        var total = 0

        active.forEach { p ->
            val requested = requestedStartSlot(p, workDays)

            val effectiveRequested = if (p.status == ProjectStatus.IN_PROGRESS) {
                Slot(nextWorkDay(from, workDays), if (p.actualStart == from) startHalfFromTime(p.startTime) else 0)
            } else requested

            if (effectiveRequested != null && compareSlots(effectiveRequested, cursor) > 0) {
                val gapEnd = previousSlot(effectiveRequested, workDays)
                if (compareSlots(cursor, gapEnd) <= 0) {
                    gaps += FreeWindow(cursor.date, cursor.half, gapEnd.date, gapEnd.half)
                }
                cursor = effectiveRequested
            }

            val start = cursor
            val delayed = if (requested != null && compareSlots(start, requested) > 0) {
                ChronoUnit.DAYS.between(requested.date, start.date).coerceAtLeast(0)
            } else 0

            var remaining = p.durationHalfDays.coerceAtLeast(1)
            var end = cursor
            while (remaining > 0) {
                end = cursor
                remaining--
                total++
                if (remaining > 0) cursor = nextSlot(cursor, workDays)
            }
            cursor = nextSlot(end, workDays)

            items += ScheduledProject(
                projectId = p.id,
                title = p.title,
                furnitureType = p.furnitureType,
                startDate = start.date,
                startHalf = start.half,
                endDate = end.date,
                endHalf = end.half,
                delayedFromFixedDateByDays = delayed,
            )
        }

        return ScheduleResult(
            items = items,
            nextFreeDate = cursor.date,
            nextFreeHalf = cursor.half,
            totalHalfDays = total,
            gaps = gaps,
        )
    }

    fun estimateNewJob(
        result: ScheduleResult,
        durationHalfDays: Int,
        workDays: Set<DayOfWeek>,
    ): Pair<LocalDate, LocalDate> {
        val duration = durationHalfDays.coerceAtLeast(1)
        val fittingGap = result.gaps.firstOrNull { gapCapacity(it, workDays) >= duration }
        val start = fittingGap?.let { Slot(it.startDate, it.startHalf) }
            ?: Slot(result.nextFreeDate, result.nextFreeHalf)
        val end = advanceForDuration(start, duration, workDays)
        return start.date to end.date
    }

    fun startHalfFromTime(value: String): Int {
        val time = runCatching { LocalTime.parse(value.trim()) }.getOrNull() ?: return 0
        return if (time >= LocalTime.of(13, 0)) 1 else 0
    }

    private fun requestedStartSlot(project: Project, workDays: Set<DayOfWeek>): Slot? {
        val date = project.plannedStart ?: return null
        return Slot(nextWorkDay(date, workDays), startHalfFromTime(project.startTime))
    }

    private fun advanceForDuration(start: Slot, duration: Int, workDays: Set<DayOfWeek>): Slot {
        var slot = start
        repeat(duration - 1) { slot = nextSlot(slot, workDays) }
        return slot
    }

    private fun gapCapacity(gap: FreeWindow, workDays: Set<DayOfWeek>): Int {
        var slot = Slot(gap.startDate, gap.startHalf)
        val end = Slot(gap.endDate, gap.endHalf)
        var count = 0
        while (compareSlots(slot, end) <= 0 && count < 10000) {
            count++
            if (slot == end) break
            slot = nextSlot(slot, workDays)
        }
        return count
    }

    private fun countElapsedHalfSlots(
        startDate: LocalDate,
        startHalf: Int,
        untilDateExclusive: LocalDate,
        workDays: Set<DayOfWeek>,
    ): Int {
        if (!startDate.isBefore(untilDateExclusive)) return 0
        var d = startDate
        var count = 0
        while (d.isBefore(untilDateExclusive)) {
            val countsAsWorkDay = d == startDate || workDays.isEmpty() || d.dayOfWeek in workDays
            if (countsAsWorkDay) count += if (d == startDate) 2 - startHalf.coerceIn(0, 1) else 2
            d = d.plusDays(1)
        }
        return count
    }

    private fun compareSlots(a: Slot, b: Slot): Int {
        val dateCmp = a.date.compareTo(b.date)
        return if (dateCmp != 0) dateCmp else a.half.compareTo(b.half)
    }

    private fun nextSlot(slot: Slot, workDays: Set<DayOfWeek>): Slot =
        if (slot.half == 0) Slot(slot.date, 1)
        else Slot(nextWorkDay(slot.date.plusDays(1), workDays), 0)

    private fun previousSlot(slot: Slot, workDays: Set<DayOfWeek>): Slot =
        if (slot.half == 1) Slot(slot.date, 0)
        else Slot(previousWorkDay(slot.date.minusDays(1), workDays), 1)

    fun nextWorkDay(start: LocalDate, workDays: Set<DayOfWeek>): LocalDate {
        if (workDays.isEmpty()) return start
        var d = start
        repeat(31) {
            if (d.dayOfWeek in workDays) return d
            d = d.plusDays(1)
        }
        return start
    }

    private fun previousWorkDay(start: LocalDate, workDays: Set<DayOfWeek>): LocalDate {
        if (workDays.isEmpty()) return start
        var d = start
        repeat(31) {
            if (d.dayOfWeek in workDays) return d
            d = d.minusDays(1)
        }
        return start
    }
}
