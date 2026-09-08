package com.milibaby.app

object FeedingRecommendationCalculator {
    fun calculate(ageDays: Long, weightGrams: Int, feedingType: String): Recommendation {
        if (ageDays < 7) return Recommendation(null, null, null, "Первые дни: ориентир 30–60 мл за кормление каждые 2–3 часа. Следуйте сигналам голода и рекомендациям врача.", false)
        if (ageDays > 183 || feedingType == "Смесь + прикорм") return Recommendation(null, null, null, "После начала прикорма потребность в смеси индивидуальна и зависит от другой пищи.", false)
        val kg = weightGrams / 1000.0
        val min = kotlin.math.round(kg * 150).toInt()
        val avg = kotlin.math.round(kg * 165).toInt()
        val max = kotlin.math.round(kg * 200).toInt()
        val extra = if (feedingType == "Смешанное питание") " При смешанном питании приложение учитывает только внесённую смесь." else ""
        return Recommendation(min, avg, max, "Ориентировочный диапазон 150–200 мл/кг/24 ч.$extra Потребности малыша индивидуальны.", true)
    }
    fun status(totalMl:Int, rec:Recommendation):String = when {
        !rec.strictDaily || rec.minMl == null || rec.maxMl == null -> "Ориентировочная оценка"
        totalMl < rec.minMl -> "Ниже ориентировочного диапазона"
        totalMl > rec.maxMl -> "Выше ориентировочного диапазона"
        else -> "В ориентировочном диапазоне"
    }
}
