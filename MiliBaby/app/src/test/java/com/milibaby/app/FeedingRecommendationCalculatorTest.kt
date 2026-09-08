package com.milibaby.app

import org.junit.Assert.*
import org.junit.Test

class FeedingRecommendationCalculatorTest {
    @Test fun sixKgTwoMonths(){ val r=FeedingRecommendationCalculator.calculate(60,6000,"Только смесь"); assertEquals(900,r.minMl);assertEquals(990,r.avgMl);assertEquals(1200,r.maxMl) }
    @Test fun fivePointNine(){ val r=FeedingRecommendationCalculator.calculate(60,5900,"Только смесь"); assertEquals(885,r.minMl);assertEquals(1180,r.maxMl) }
    @Test fun newbornNoStrictDaily(){ val r=FeedingRecommendationCalculator.calculate(3,3500,"Только смесь"); assertFalse(r.strictDaily);assertNull(r.minMl) }
    @Test fun complementaryNoStrictDaily(){ val r=FeedingRecommendationCalculator.calculate(220,8000,"Смесь + прикорм"); assertFalse(r.strictDaily) }
}
