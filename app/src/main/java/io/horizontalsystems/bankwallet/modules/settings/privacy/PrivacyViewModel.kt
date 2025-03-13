package io.horizontalsystems.bankwallet.modules.settings.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.stats.StatsManager
import kotlinx.coroutines.launch
import java.util.Calendar

class PrivacyViewModel(private val statsManager: StatsManager) : ViewModelUiState<PrivacyUiState>() {
    private var uiStatsEnabled = statsManager.uiStatsEnabledFlow.value
    private val currentYear: Int = Calendar.getInstance().get(Calendar.YEAR)
    private var isDetectCrash = statsManager.isDetectCrashEnabledFlow.value


    init {
        viewModelScope.launch {
            statsManager.uiStatsEnabledFlow.collect {
                uiStatsEnabled = it
                emitState()
            }
        }
        viewModelScope.launch {
            statsManager.isDetectCrashEnabledFlow.collect {
                isDetectCrash = it
                emitState()
            }
        }
    }

    override fun createState() = PrivacyUiState(
        uiStatsEnabled = uiStatsEnabled,
        currentYear = currentYear,
        isEnableDetectCrash = isDetectCrash
    )

    fun toggleUiStats(enabled: Boolean) {
        statsManager.toggleUiStats(enabled)
    }

    fun toggleDetectCrash(enabled: Boolean) {
        statsManager.toggleDetectCrash(enabled)
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PrivacyViewModel(App.statsManager) as T
        }
    }

}

data class PrivacyUiState(
    val uiStatsEnabled: Boolean,
    val currentYear: Int,
    val isEnableDetectCrash: Boolean
)
