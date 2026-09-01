package ru.furniturecrm.app

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.rustore.sdk.pay.RuStorePayClient
import ru.rustore.sdk.pay.model.PreferredPurchaseType
import ru.rustore.sdk.pay.model.ProductId
import ru.rustore.sdk.pay.model.ProductPurchase
import ru.rustore.sdk.pay.model.ProductPurchaseParams
import ru.rustore.sdk.pay.model.ProductPurchaseStatus
import ru.rustore.sdk.pay.model.ProductType

/**
 * 3-day local trial + one non-consumable RuStore entitlement.
 * Debug builds do not hard-lock the CRM before a RuStore Console application id exists.
 */
class RuStoreLicenseManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(buildLocalState())
    val state: StateFlow<LicenseUiState> = _state.asStateFlow()

    val configured: Boolean
        get() = BuildConfig.RUSTORE_CONSOLE_APP_ID.isNotBlank() && BuildConfig.RUSTORE_CONSOLE_APP_ID != "0"

    init {
        initializeFirstLaunch()
    }

    /**
     * Explicit restore only. getPurchases can ask the user to authorize in RuStore,
     * therefore it is deliberately not called on every app start.
     */
    fun refreshPurchases(silent: Boolean = false) {
        val localPurchased = prefs.getBoolean(KEY_PURCHASED, false)
        if (!configured) {
            _state.value = buildLocalState(
                processing = false,
                message = if (!silent && !localPurchased) "Оплата будет доступна после привязки приложения к RuStore." else null,
            )
            return
        }

        _state.value = buildLocalState(processing = true)
        runCatching {
            RuStorePayClient.instance.getPurchaseInteractor()
                .getPurchases(
                    productType = ProductType.NON_CONSUMABLE_PRODUCT,
                    purchaseStatus = ProductPurchaseStatus.CONFIRMED,
                )
                .addOnSuccessListener { purchases ->
                    val found = purchases
                        .filterIsInstance<ProductPurchase>()
                        .any { purchase -> purchase.productId.value == BuildConfig.RUSTORE_PRODUCT_ID }
                    if (found) prefs.edit().putBoolean(KEY_PURCHASED, true).apply()
                    _state.value = buildLocalState(
                        processing = false,
                        message = if (!silent && !found) "Покупка полного доступа не найдена в RuStore." else null,
                    )
                }
                .addOnFailureListener { error ->
                    _state.value = buildLocalState(
                        processing = false,
                        message = if (!silent) friendlyError(error) else null,
                    )
                }
        }.onFailure { error ->
            _state.value = buildLocalState(
                processing = false,
                message = if (!silent) friendlyError(error) else null,
            )
        }
    }

    fun purchase() {
        if (!configured) {
            _state.value = buildLocalState(
                message = "Сначала нужно создать приложение в RuStore и подставить его ID в release-сборку."
            )
            return
        }

        _state.value = buildLocalState(processing = true, message = null)
        val params = ProductPurchaseParams(
            productId = ProductId(BuildConfig.RUSTORE_PRODUCT_ID),
            quantity = null,
            orderId = null,
            developerPayload = null,
            appUserId = null,
            appUserEmail = null,
        )

        runCatching {
            RuStorePayClient.instance.getPurchaseInteractor()
                .purchase(params = params, preferredPurchaseType = PreferredPurchaseType.ONE_STEP)
                .addOnSuccessListener {
                    prefs.edit().putBoolean(KEY_PURCHASED, true).apply()
                    _state.value = buildLocalState(
                        processing = false,
                        message = "Доступ открыт навсегда. Спасибо!",
                    )
                }
                .addOnFailureListener { error ->
                    _state.value = buildLocalState(processing = false, message = friendlyError(error))
                }
        }.onFailure { error ->
            _state.value = buildLocalState(processing = false, message = friendlyError(error))
        }
    }

    /** Hand payment deeplinks back to Pay SDK. */
    fun proceedIntent(intent: Intent?) {
        if (!configured || intent == null) return
        runCatching { RuStorePayClient.instance.getIntentInteractor().proceedIntent(intent) }
    }

    fun onResume() {
        touchTrustedClock()
        _state.value = buildLocalState()
    }

    private fun initializeFirstLaunch() {
        val now = System.currentTimeMillis()
        if (!prefs.contains(KEY_FIRST_LAUNCH)) {
            prefs.edit()
                .putLong(KEY_FIRST_LAUNCH, now)
                .putLong(KEY_MAX_SEEN_WALL, now)
                .apply()
        } else {
            touchTrustedClock()
        }
        _state.value = buildLocalState()
    }

    private fun touchTrustedClock(): Long {
        val wall = System.currentTimeMillis()
        val seen = prefs.getLong(KEY_MAX_SEEN_WALL, wall)
        val trusted = LicensePolicy.trustedNow(wall, seen)
        if (trusted > seen) prefs.edit().putLong(KEY_MAX_SEEN_WALL, trusted).apply()
        return trusted
    }

    private fun buildLocalState(processing: Boolean = false, message: String? = null): LicenseUiState {
        val wall = System.currentTimeMillis()
        val seen = prefs.getLong(KEY_MAX_SEEN_WALL, wall)
        val now = LicensePolicy.trustedNow(wall, seen)
        val first = prefs.getLong(KEY_FIRST_LAUNCH, wall)
        val purchased = prefs.getBoolean(KEY_PURCHASED, false)
        val access = LicensePolicy.evaluate(first, now, purchased)
        return LicenseUiState(
            access = access,
            processing = processing,
            configured = configured,
            enforcePaywall = BuildConfig.ENFORCE_PAYWALL,
            priceLabel = "199 ₽",
            message = message,
        )
    }

    private fun friendlyError(error: Throwable): String {
        val raw = error.message.orEmpty()
        return when {
            raw.contains("cancel", ignoreCase = true) || raw.contains("отмен", ignoreCase = true) -> "Оплата отменена."
            raw.isNotBlank() -> "Не удалось проверить оплату: $raw"
            else -> "Не удалось связаться с RuStore. Проверь интернет и попробуй ещё раз."
        }
    }

    companion object {
        private const val PREFS = "master_object_license"
        private const val KEY_FIRST_LAUNCH = "first_launch_ms"
        private const val KEY_MAX_SEEN_WALL = "max_seen_wall_ms"
        private const val KEY_PURCHASED = "purchased_full_access"
    }
}

data class LicenseUiState(
    val access: LicenseAccess,
    val processing: Boolean = false,
    val configured: Boolean = false,
    val enforcePaywall: Boolean = true,
    val priceLabel: String = "199 ₽",
    val message: String? = null,
) {
    val hasFullAccess: Boolean get() = access is LicenseAccess.Full
    val isTrial: Boolean get() = access is LicenseAccess.Trial
    val canUseApp: Boolean get() = hasFullAccess || isTrial || !enforcePaywall

    val remainingMs: Long get() = (access as? LicenseAccess.Trial)?.remainingMs ?: 0L
    val remainingText: String
        get() {
            val totalMinutes = (remainingMs / 60_000L).coerceAtLeast(0L)
            val days = totalMinutes / (24L * 60L)
            val hours = (totalMinutes % (24L * 60L) + 59L) / 60L
            return when {
                days >= 1L -> "$days дн. ${hours.coerceAtMost(23)} ч."
                hours >= 1L -> "$hours ч."
                totalMinutes > 0L -> "$totalMinutes мин."
                else -> "меньше минуты"
            }
        }
}
