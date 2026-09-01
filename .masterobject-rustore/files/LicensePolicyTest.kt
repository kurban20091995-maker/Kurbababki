package ru.furniturecrm.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LicensePolicyTest {
    @Test fun trialIsAvailableForThreeDays() {
        val first = 1_000_000L
        val access = LicensePolicy.evaluate(first, first + 2 * LicensePolicy.DAY_MS, purchased = false)
        assertTrue(access is LicenseAccess.Trial)
        assertEquals(LicensePolicy.DAY_MS, (access as LicenseAccess.Trial).remainingMs)
    }

    @Test fun trialLocksAtExactEnd() {
        val first = 1_000_000L
        assertTrue(LicensePolicy.evaluate(first, first + LicensePolicy.TRIAL_MS, false) is LicenseAccess.Locked)
    }

    @Test fun purchaseUnlocksAfterTrialForever() {
        val first = 1_000_000L
        assertTrue(LicensePolicy.evaluate(first, first + 500 * LicensePolicy.DAY_MS, true) is LicenseAccess.Full)
    }

    @Test fun movingPhoneClockBackCannotExtendSeenTime() {
        assertEquals(2_000L, LicensePolicy.trustedNow(1_000L, 2_000L))
        assertEquals(3_000L, LicensePolicy.trustedNow(3_000L, 2_000L))
    }
}
