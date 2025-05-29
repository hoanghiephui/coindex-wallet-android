package io.horizontalsystems.bankwallet.core.managers

import android.annotation.SuppressLint
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.IMarketStorage
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.AppVersion
import io.horizontalsystems.bankwallet.entities.LaunchPage
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.horizontalsystems.bankwallet.modules.amount.AmountInputType
import io.horizontalsystems.bankwallet.modules.balance.BalanceSortType
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewType
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.modules.market.MarketModule
import io.horizontalsystems.bankwallet.modules.market.TimeDuration
import io.horizontalsystems.bankwallet.modules.market.favorites.WatchlistSorting
import io.horizontalsystems.bankwallet.modules.settings.appearance.AppIcon
import io.horizontalsystems.bankwallet.modules.settings.appearance.PriceChangeInterval
import io.horizontalsystems.bankwallet.modules.settings.security.autolock.AutoLockInterval
import io.horizontalsystems.bankwallet.modules.theme.ThemeType
import io.horizontalsystems.core.ILockoutStorage
import io.horizontalsystems.core.IPinSettingsStorage
import io.horizontalsystems.core.IThirdKeyboard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class LocalStorageManager(
    private val preferences: SharedPreferences
) : ILocalStorage, IPinSettingsStorage, ILockoutStorage, IThirdKeyboard, IMarketStorage {
    companion object {
        private const val THIRD_KEYBOARD_WARNING_MSG = "third_keyboard_warning_msg"
        private const val SEND_INPUT_TYPE = "send_input_type"
        private const val BASE_CURRENCY_CODE = "base_currency_code"
        private const val AUTH_TOKEN = "auth_token"
        private const val FAILED_ATTEMPTS = "failed_attempts"
        private const val LOCKOUT_TIMESTAMP = "lockout_timestamp"
        private const val BASE_BITCOIN_PROVIDER = "base_bitcoin_provider"
        private const val BASE_LITECOIN_PROVIDER = "base_litecoin_provider"
        private const val BASE_ETHEREUM_PROVIDER = "base_ethereum_provider"
        private const val BASE_DASH_PROVIDER = "base_dash_provider"
        private const val BASE_ZCASH_PROVIDER = "base_zcash_provider"
        private const val SYNC_MODE = "sync_mode"
        private const val SORT_TYPE = "balance_sort_type"
        private const val APP_VERSIONS = "app_versions"
        private const val ALERT_NOTIFICATION_ENABLED = "alert_notification"
        private const val ENCRYPTION_CHECKER_TEXT = "encryption_checker_text"
        private const val BITCOIN_DERIVATION = "bitcoin_derivation"
        private const val TOR_ENABLED = "tor_enabled"
        private const val APP_LAUNCH_COUNT = "app_launch_count"
        private const val RATE_APP_LAST_REQ_TIME = "rate_app_last_req_time"
        private const val BALANCE_HIDDEN = "balance_hidden"
        private const val TERMS_AGREED = "terms_agreed"
        private const val MARKET_CURRENT_TAB = "market_current_tab"
        private const val BIOMETRIC_ENABLED = "biometric_auth_enabled"
        private const val PIN = "lock_pin"
        private const val MAIN_SHOWED_ONCE = "main_showed_once"
        private const val NOTIFICATION_ID = "notification_id"
        private const val NOTIFICATION_SERVER_TIME = "notification_server_time"
        private const val CURRENT_THEME = "current_theme"
        private const val CHANGELOG_SHOWN_FOR_APP_VERSION = "changelog_shown_for_app_version"
        private const val IGNORE_ROOTED_DEVICE_WARNING = "ignore_rooted_device_warning"
        private const val LAUNCH_PAGE = "launch_page"
        private const val APP_ICON = "app_icon"
        private const val MAIN_TAB = "main_tab"
        private const val MARKET_FAVORITES_SORTING = "market_favorites_sorting"
        private const val MARKET_FAVORITES_SHOW_SIGNALS = "market_favorites_show_signals"
        private const val MARKET_FAVORITES_TIME_DURATION = "market_favorites_time_duration"
        private const val MARKET_FAVORITES_MANUAL_SORTING_ORDER =
            "market_favorites_manual_sorting_order"
        private const val RELAUNCH_BY_SETTING_CHANGE = "relaunch_by_setting_change"
        private const val MARKETS_TAB_ENABLED = "markets_tab_enabled"
        private const val BALANCE_AUTO_HIDE_ENABLED = "balance_auto_hide_enabled"
        private const val NON_RECOMMENDED_ACCOUNT_ALERT_DISMISSED_ACCOUNTS =
            "non_recommended_account_alert_dismissed_accounts"
        private const val PERSONAL_SUPPORT_ENABLED = "personal_support_enabled"
        private const val APP_ID = "app_id"
        private const val APP_AUTO_LOCK_INTERVAL = "app_auto_lock_interval"
        private const val HIDE_SUSPICIOUS_TX = "hide_suspicious_tx"
        private const val PIN_RANDOMIZED = "pin_randomized"
        private const val UTXO_EXPERT_MODE = "utxo_expert_mode"
        private const val RBF_ENABLED = "rbf_enabled"
        private const val STATS_SYNC_TIME = "stats_sync_time"
        private const val PRICE_CHANGE_INTERVAL = "price_change_interval"
        private const val UI_STATS_ENABLED = "ui_stats_enabled"

        private const val ANALYTIC = "ANALYTIC"
        private const val DETECT_CRASH = "DETECT_CRASH"
        private const val NOTIFICATION_PRICE = "NOTIFICATION_PRICE"
        private const val NOTIFICATION_NEWS = "NOTIFICATION_NEWS"
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val _utxoExpertModeEnabledFlow = MutableStateFlow(false)
    override val utxoExpertModeEnabledFlow = _utxoExpertModeEnabledFlow

    private val _marketSignalsStateChangedFlow = MutableSharedFlow<Boolean>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val marketSignalsStateChangedFlow = _marketSignalsStateChangedFlow

    private val gson by lazy { Gson() }

    override var chartIndicatorsEnabled: Boolean
        get() = preferences.getBoolean("chartIndicatorsEnabled", false)
        set(enabled) {
            preferences.edit { putBoolean("chartIndicatorsEnabled", enabled) }
        }

    override var amountInputType: AmountInputType?
        get() = preferences.getString(SEND_INPUT_TYPE, null)?.let {
            try {
                AmountInputType.valueOf(it)
            } catch (_: IllegalArgumentException) {
                null
            }
        }
        set(value) {
            val editor = preferences.edit()
            when (value) {
                null -> editor.remove(SEND_INPUT_TYPE).apply()
                else -> editor.putString(SEND_INPUT_TYPE, value.name).apply()
            }
        }

    override var marketSearchRecentCoinUids: List<String>
        get() = preferences.getString("marketSearchRecentCoinUids", null)?.split(",") ?: listOf()
        set(value) {
            preferences.edit { putString("marketSearchRecentCoinUids", value.joinToString(",")) }
        }

    override var zcashAccountIds: Set<String>
        get() = preferences.getStringSet("zcashAccountIds", setOf()) ?: setOf()
        set(value) {
            preferences.edit { putStringSet("zcashAccountIds", value) }
        }

    override var baseCurrencyCode: String?
        get() = preferences.getString(BASE_CURRENCY_CODE, null)
        set(value) {
            preferences.edit { putString(BASE_CURRENCY_CODE, value) }
        }

    override var authToken: String?
        get() = preferences.getString(AUTH_TOKEN, null)
        set(value) {
            preferences.edit { putString(AUTH_TOKEN, value) }
        }

    override val appId: String?
        get() {
            return when (val id = preferences.getString(APP_ID, null)) {
                null -> {
                    val newId = UUID.randomUUID().toString()
                    preferences.edit { putString(APP_ID, newId) }
                    newId
                }

                else -> id
            }
        }

    override var baseBitcoinProvider: String?
        get() = preferences.getString(BASE_BITCOIN_PROVIDER, null)
        set(value) {
            preferences.edit { putString(BASE_BITCOIN_PROVIDER, value) }
        }

    override var baseLitecoinProvider: String?
        get() = preferences.getString(BASE_LITECOIN_PROVIDER, null)
        set(value) {
            preferences.edit { putString(BASE_LITECOIN_PROVIDER, value) }
        }

    override var baseEthereumProvider: String?
        get() = preferences.getString(BASE_ETHEREUM_PROVIDER, null)
        set(value) {
            preferences.edit { putString(BASE_ETHEREUM_PROVIDER, value) }
        }

    override var baseDashProvider: String?
        get() = preferences.getString(BASE_DASH_PROVIDER, null)
        set(value) {
            preferences.edit { putString(BASE_DASH_PROVIDER, value) }
        }

    override var baseZcashProvider: String?
        get() = preferences.getString(BASE_ZCASH_PROVIDER, null)
        set(value) {
            preferences.edit { putString(BASE_ZCASH_PROVIDER, value) }
        }

    override var sortType: BalanceSortType
        get() {
            val sortString = preferences.getString(SORT_TYPE, null)
                ?: BalanceSortType.Value.getAsString()
            return BalanceSortType.getTypeFromString(sortString)
        }
        set(sortType) {
            preferences.edit { putString(SORT_TYPE, sortType.getAsString()) }
        }

    override var appVersions: List<AppVersion>
        get() {
            val versionsString = preferences.getString(APP_VERSIONS, null) ?: return listOf()
            val type = object : TypeToken<ArrayList<AppVersion>>() {}.type
            return gson.fromJson(versionsString, type)
        }
        set(value) {
            val versionsString = gson.toJson(value)
            preferences.edit { putString(APP_VERSIONS, versionsString) }
        }

    override var isAlertNotificationOn: Boolean
        get() = preferences.getBoolean(ALERT_NOTIFICATION_ENABLED, true)
        set(enabled) {
            preferences.edit { putBoolean(ALERT_NOTIFICATION_ENABLED, enabled) }
        }

    override var encryptedSampleText: String?
        get() = preferences.getString(ENCRYPTION_CHECKER_TEXT, null)
        set(encryptedText) {
            preferences.edit { putString(ENCRYPTION_CHECKER_TEXT, encryptedText) }
        }

    override fun clear() {
        preferences.edit { clear() }
    }

    override var currentTheme: ThemeType
        get() = preferences.getString(CURRENT_THEME, null)?.let { ThemeType.valueOf(it) }
            ?: ThemeType.System
        set(themeType) {
            preferences.edit { putString(CURRENT_THEME, themeType.value) }
        }

    override var balanceViewType: BalanceViewType?
        get() = preferences.getString("balanceViewType", null)?.let {
            try {
                BalanceViewType.valueOf(it)
            } catch (_: IllegalArgumentException) {
                null
            }
        }
        set(value) {
            if (value != null) {
                preferences.edit { putString("balanceViewType", value.name) }
            } else {
                preferences.edit { remove("balanceViewType") }
            }
        }

    //  IKeyboardStorage

    override var isThirdPartyKeyboardAllowed: Boolean
        get() = preferences.getBoolean(THIRD_KEYBOARD_WARNING_MSG, false)
        set(enabled) {
            preferences.edit { putBoolean(THIRD_KEYBOARD_WARNING_MSG, enabled) }
        }

    //  IPinStorage

    override var failedAttempts: Int?
        get() {
            val attempts = preferences.getInt(FAILED_ATTEMPTS, 0)
            return when (attempts) {
                0 -> null
                else -> attempts
            }
        }
        set(value) {
            value?.let {
                preferences.edit { putInt(FAILED_ATTEMPTS, it) }
            } ?: preferences.edit { remove(FAILED_ATTEMPTS) }
        }

    override var lockoutUptime: Long?
        get() {
            val timestamp = preferences.getLong(LOCKOUT_TIMESTAMP, 0L)
            return when (timestamp) {
                0L -> null
                else -> timestamp
            }
        }
        set(value) {
            value?.let {
                preferences.edit { putLong(LOCKOUT_TIMESTAMP, it) }
            } ?: preferences.edit { remove(LOCKOUT_TIMESTAMP) }
        }

    override var biometricAuthEnabled: Boolean
        get() = preferences.getBoolean(BIOMETRIC_ENABLED, false)
        set(value) {
            preferences.edit { putBoolean(BIOMETRIC_ENABLED, value) }
        }

    override var pin: String?
        get() = preferences.getString(PIN, null)
        set(value) {
            preferences.edit { putString(PIN, value) }
        }

    //used only in db migration
    override var syncMode: SyncMode?
        get() = preferences.getString(SYNC_MODE, null)?.let {
            try {
                SyncMode.valueOf(it)
            } catch (_: IllegalArgumentException) {
                null
            }
        }
        set(syncMode) {
            preferences.edit { putString(SYNC_MODE, syncMode?.value) }
        }

    //used only in db migration
    override var bitcoinDerivation: AccountType.Derivation?
        get() {
            val derivationString = preferences.getString(BITCOIN_DERIVATION, null)
            return derivationString?.let { AccountType.Derivation.valueOf(it) }
        }
        set(derivation) {
            preferences.edit { putString(BITCOIN_DERIVATION, derivation?.value) }
        }

    //  IChartTypeStorage

    override var torEnabled: Boolean
        get() = preferences.getBoolean(TOR_ENABLED, false)
        @SuppressLint("ApplySharedPref")
        set(enabled) {
            //keep using commit() for synchronous storing
            preferences.edit(commit = true) { putBoolean(TOR_ENABLED, enabled) }
        }

    override var appLaunchCount: Int
        get() = preferences.getInt(APP_LAUNCH_COUNT, 0)
        set(value) {
            preferences.edit { putInt(APP_LAUNCH_COUNT, value) }
        }

    override var rateAppLastRequestTime: Long
        get() = preferences.getLong(RATE_APP_LAST_REQ_TIME, 0)
        set(value) {
            preferences.edit { putLong(RATE_APP_LAST_REQ_TIME, value) }
        }

    override var balanceHidden: Boolean
        get() = preferences.getBoolean(BALANCE_HIDDEN, false)
        set(value) {
            preferences.edit { putBoolean(BALANCE_HIDDEN, value) }
        }

    override var balanceAutoHideEnabled: Boolean
        get() = preferences.getBoolean(BALANCE_AUTO_HIDE_ENABLED, false)
        set(value) {
            preferences.edit(commit = true) { putBoolean(BALANCE_AUTO_HIDE_ENABLED, value) }
        }

    override var balanceTotalCoinUid: String?
        get() = preferences.getString("balanceTotalCoinUid", null)
        set(value) {
            preferences.edit { putString("balanceTotalCoinUid", value) }
        }

    override var termsAccepted: Boolean
        get() = preferences.getBoolean(TERMS_AGREED, false)
        set(value) {
            preferences.edit { putBoolean(TERMS_AGREED, value) }
        }

    override var currentMarketTab: MarketModule.Tab?
        get() = preferences.getString(MARKET_CURRENT_TAB, null)?.let {
            MarketModule.Tab.fromString(it)
        }
        set(value) {
            preferences.edit { putString(MARKET_CURRENT_TAB, value?.name) }
        }

    override var mainShowedOnce: Boolean
        get() = preferences.getBoolean(MAIN_SHOWED_ONCE, false)
        set(value) {
            preferences.edit { putBoolean(MAIN_SHOWED_ONCE, value) }
        }

    override var notificationId: String?
        get() = preferences.getString(NOTIFICATION_ID, null)
        set(value) {
            preferences.edit { putString(NOTIFICATION_ID, value) }
        }

    override var notificationServerTime: Long
        get() = preferences.getLong(NOTIFICATION_SERVER_TIME, 0)
        set(value) {
            preferences.edit { putLong(NOTIFICATION_SERVER_TIME, value) }
        }

    override var changelogShownForAppVersion: String?
        get() = preferences.getString(CHANGELOG_SHOWN_FOR_APP_VERSION, null)
        set(value) {
            preferences.edit { putString(CHANGELOG_SHOWN_FOR_APP_VERSION, value) }
        }

    override var donateAppVersion: String?
        get() = preferences.getString("donate_app_version", null)
        set(value) {
            preferences.edit().putString("donate_app_version", value).apply()
        }

    override var ignoreRootedDeviceWarning: Boolean
        get() = preferences.getBoolean(IGNORE_ROOTED_DEVICE_WARNING, false)
        set(value) {
            preferences.edit { putBoolean(IGNORE_ROOTED_DEVICE_WARNING, value) }
        }

    override var launchPage: LaunchPage?
        get() = preferences.getString(LAUNCH_PAGE, null)?.let {
            LaunchPage.fromString(it)
        }
        set(value) {
            preferences.edit { putString(LAUNCH_PAGE, value?.name) }
        }

    override var appIcon: AppIcon?
        get() = preferences.getString(APP_ICON, null)?.let {
            AppIcon.fromString(it)
        }
        set(value) {
            preferences.edit { putString(APP_ICON, value?.name) }
        }

    override var mainTab: MainModule.MainNavigation?
        get() = preferences.getString(MAIN_TAB, null)?.let {
            MainModule.MainNavigation.fromString(it)
        }
        set(value) {
            preferences.edit { putString(MAIN_TAB, value?.name) }
        }

    override var marketFavoritesSorting: WatchlistSorting?
        get() = preferences.getString(MARKET_FAVORITES_SORTING, null)?.let {
            WatchlistSorting.valueOf(it)
        }
        set(value) {
            preferences.edit { putString(MARKET_FAVORITES_SORTING, value?.name) }
        }

    override var marketFavoritesShowSignals: Boolean
        get() = preferences.getBoolean(MARKET_FAVORITES_SHOW_SIGNALS, false)
        set(value) {
            preferences.edit { putBoolean(MARKET_FAVORITES_SHOW_SIGNALS, value) }
            coroutineScope.launch {
                _marketSignalsStateChangedFlow.emit(value)
            }
        }

    override var marketFavoritesManualSortingOrder: List<String>
        get() = preferences.getString(MARKET_FAVORITES_MANUAL_SORTING_ORDER, null)?.split(",")
            ?: listOf()
        set(value) {
            preferences.edit {
                putString(
                    MARKET_FAVORITES_MANUAL_SORTING_ORDER,
                    value.joinToString(",")
                )
            }
        }

    override var marketFavoritesPeriod: TimeDuration?
        get() = preferences.getString(MARKET_FAVORITES_TIME_DURATION, null)?.let {
            TimeDuration.entries.find { period -> period.name == it }
        }
        set(value) {
            preferences.edit { putString(MARKET_FAVORITES_TIME_DURATION, value?.name) }
        }

    override var relaunchBySettingChange: Boolean
        get() = preferences.getBoolean(RELAUNCH_BY_SETTING_CHANGE, false)
        set(value) {
            preferences.edit(commit = true) { putBoolean(RELAUNCH_BY_SETTING_CHANGE, value) }
        }

    override var marketsTabEnabled: Boolean
        get() = preferences.getBoolean(MARKETS_TAB_ENABLED, true)
        set(value) {
            preferences.edit(commit = true) { putBoolean(MARKETS_TAB_ENABLED, value) }
            _marketsTabEnabledFlow.update {
                value
            }
        }

    override var balanceTabButtonsEnabled: Boolean
        get() = preferences.getBoolean("balanceTabButtonsEnabled", true)
        set(value) {
            preferences.edit { putBoolean("balanceTabButtonsEnabled", value) }
            balanceTabButtonsEnabledFlow.update { value }
        }

    override val balanceTabButtonsEnabledFlow = MutableStateFlow(balanceTabButtonsEnabled)

    override var personalSupportEnabled: Boolean
        get() = preferences.getBoolean(PERSONAL_SUPPORT_ENABLED, false)
        set(enabled) {
            preferences.edit { putBoolean(PERSONAL_SUPPORT_ENABLED, enabled) }
        }

    override var hideSuspiciousTransactions: Boolean
        get() = preferences.getBoolean(HIDE_SUSPICIOUS_TX, true)
        set(value) {
            preferences.edit { putBoolean(HIDE_SUSPICIOUS_TX, value) }
        }

    override var pinRandomized: Boolean
        get() = preferences.getBoolean(PIN_RANDOMIZED, false)
        set(value) {
            preferences.edit { putBoolean(PIN_RANDOMIZED, value) }
        }

    override var isAnalytic: Boolean
        get() = preferences.getBoolean(ANALYTIC, false)
        set(value) {
            preferences.edit { putBoolean(ANALYTIC, value) }
        }
    override var isDetectCrash: Boolean
        get() = preferences.getBoolean(DETECT_CRASH, true)
        set(value) {
            preferences.edit { putBoolean(DETECT_CRASH, value) }
        }

    private val _marketsTabEnabledFlow = MutableStateFlow(marketsTabEnabled)
    override val marketsTabEnabledFlow = _marketsTabEnabledFlow.asStateFlow()

    override var nonRecommendedAccountAlertDismissedAccounts: Set<String>
        get() = preferences.getStringSet(NON_RECOMMENDED_ACCOUNT_ALERT_DISMISSED_ACCOUNTS, setOf())
            ?: setOf()
        set(value) {
            preferences.edit {
                putStringSet(
                    NON_RECOMMENDED_ACCOUNT_ALERT_DISMISSED_ACCOUNTS,
                    value
                )
            }
        }

    override var autoLockInterval: AutoLockInterval
        get() = preferences.getString(APP_AUTO_LOCK_INTERVAL, null)?.let {
            AutoLockInterval.fromRaw(it)
        } ?: AutoLockInterval.AFTER_1_MIN
        set(value) {
            preferences.edit { putString(APP_AUTO_LOCK_INTERVAL, value.raw) }
        }

    override var utxoExpertModeEnabled: Boolean
        get() = preferences.getBoolean(UTXO_EXPERT_MODE, false)
        set(value) {
            preferences.edit { putBoolean(UTXO_EXPERT_MODE, value) }
            _utxoExpertModeEnabledFlow.update {
                value
            }
        }

    override var rbfEnabled: Boolean
        get() = preferences.getBoolean(RBF_ENABLED, true)
        set(value) {
            preferences.edit { putBoolean(RBF_ENABLED, value) }
        }

    override var statsLastSyncTime: Long
        get() = preferences.getLong(STATS_SYNC_TIME, 0)
        set(value) {
            preferences.edit { putLong(STATS_SYNC_TIME, value) }
        }

    override var priceChangeInterval: PriceChangeInterval
        get() = preferences.getString(PRICE_CHANGE_INTERVAL, null)?.let {
            PriceChangeInterval.fromRaw(it)
        } ?: PriceChangeInterval.LAST_24H
        set(value) {
            preferences.edit { putString(PRICE_CHANGE_INTERVAL, value.raw) }

            priceChangeIntervalFlow.update { value }
        }

    override val priceChangeIntervalFlow = MutableStateFlow(priceChangeInterval)

    override var uiStatsEnabled: Boolean?
        get() = when {
            preferences.contains(UI_STATS_ENABLED) -> {
                preferences.getBoolean(UI_STATS_ENABLED, true)
            }

            else -> true
        }
        set(value) {
            val editor = preferences.edit()
            if (value == null) {
                editor.remove(UI_STATS_ENABLED).apply()
            } else {
                editor.putBoolean(UI_STATS_ENABLED, value).apply()
            }
        }

    override var isShowNotificationPrice: Boolean
        get() = preferences.getBoolean(NOTIFICATION_PRICE, true)
        set(value) {
            preferences.edit { putBoolean(NOTIFICATION_PRICE, value) }
        }
    override var isShowNotificationNews: Boolean
        get() = preferences.getBoolean(NOTIFICATION_NEWS, true)
        set(value) {
            preferences.edit { putBoolean(NOTIFICATION_NEWS, value) }
        }
}
