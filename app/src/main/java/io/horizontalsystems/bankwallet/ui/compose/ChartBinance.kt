package io.horizontalsystems.bankwallet.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import com.tradingview.lightweightcharts.api.series.common.SeriesData
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.model.DataChart
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.ChartTab
import io.horizontalsystems.bankwallet.modules.theme.ThemeType
import io.horizontalsystems.bankwallet.ui.ChartBinanceView
import io.horizontalsystems.bankwallet.uiv3.components.tabs.TabItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun <T> ChartBinance(
    data: DataChart?,
    dataVolume: DataChart?,
    seriesFlow: Flow<SeriesData>,
    volumeFlow: Flow<SeriesData>,
    lifecycleScope: LifecycleOwner,
    listener: ListenerChart,
    isClear: MutableStateFlow<Boolean>,
    tabItems: List<TabItem<T>>,
    onSelectTab: (T) -> Unit,
    remove: MutableStateFlow<Boolean>
) {
    Column {
        var updateEnabled by remember { mutableStateOf(true) }

        AndroidView(
            modifier = Modifier.fillMaxSize(), // Occupy the max size in the Compose UI tree
            factory = { context ->
                val theme = ThemeType.valueOf(App.localStorage.currentTheme.value) == ThemeType.Dark
                ChartBinanceView(context).apply {
                    updateEnabled = data == null
                    setOnClear(isClear)
                    setListener(listener)
                    setSystemInDarkTheme(theme)
                    setLifecycleScope(lifecycleScope)
                    setSeriesFlow(seriesFlow)
                    setVolumeFlow(volumeFlow)
                    bindView()
                    bindDataView(data, dataVolume)
                    setOnRemove(remove)
                }
            },
            update = { view ->
                if (updateEnabled) {
                    view.bindDataView(data, dataVolume)
                }
                updateEnabled = data == null
            }
        )
        ChartTab(
            tabItems = tabItems,
            onSelect = onSelectTab
        )
    }
}

interface ListenerChart {
    fun onChartTouchDown()
    fun onChartTouchUp()
}
