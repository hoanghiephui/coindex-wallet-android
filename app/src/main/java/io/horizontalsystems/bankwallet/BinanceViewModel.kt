package io.horizontalsystems.bankwallet

import android.graphics.Color
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billing.network.AppDispatcher
import com.android.billing.network.Dispatcher
import com.tinder.scarlet.WebSocket
import com.tradingview.lightweightcharts.api.chart.models.color.toIntColor
import com.tradingview.lightweightcharts.api.series.common.SeriesData
import com.tradingview.lightweightcharts.api.series.enums.SeriesType
import com.tradingview.lightweightcharts.api.series.models.BarData
import com.tradingview.lightweightcharts.api.series.models.HistogramData
import com.tradingview.lightweightcharts.api.series.models.Time
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.model.BnTimePeriod
import io.horizontalsystems.bankwallet.model.DataChart
import io.horizontalsystems.bankwallet.model.Subscribe
import io.horizontalsystems.bankwallet.repository.BinanceRepository
import io.horizontalsystems.bankwallet.repository.convertToTimeUTC
import io.horizontalsystems.bankwallet.ui.toSymbolKlineUseSocketBinance
import io.horizontalsystems.bankwallet.uiv3.components.tabs.TabItem
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class BinanceViewModel @Inject constructor(
    private val repository: BinanceRepository,
    @Dispatcher(AppDispatcher.Default)
    private val defaultDispatcher: CoroutineDispatcher
) : ViewModel() {
    private var id = 1
    val onClear: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val remove: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val tabItemsLiveData = MutableLiveData<List<TabItem<BnTimePeriod>>>()
    private val chartTypeObservable = BehaviorSubject.create<BnTimePeriod>()
    private var chartInterval: BnTimePeriod? = null
        set(value) {
            field = value
            value?.let { chartTypeObservable.onNext(it) }
        }

    private val _binanceState = MutableStateFlow<BinanceAvailable>(BinanceAvailable.Loading)
    val binanceState = _binanceState.asStateFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BinanceAvailable.Loading,
    )
    private val _binanceAvailable = MutableSharedFlow<Boolean>()
    val binanceAvailable = _binanceAvailable.asSharedFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )
    private val _candlestickSeriesState =
        MutableStateFlow<CandlestickSeries>(CandlestickSeries.Loading)
    val candlestickSeriesState = _candlestickSeriesState.asStateFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CandlestickSeries.Loading
    )
    val symbolTickerStream
        get() = repository.getSymbolTickerStreams

    val seriesFlow: Flow<SeriesData>
        get() = repository.getListSeriesData
    val volumeFlow: Flow<SeriesData>
        get() = repository.getListVolumeData

    fun onBinanceAvailable(symbol: String) {
        viewModelScope.launch(defaultDispatcher) {
            asFlowResult {
                repository.getSymbolPriceTicker(symbol)
            }.safeCollect(
                onEach = {
                    val available = it.data?.price != null
                    _binanceState.emit(BinanceAvailable.StateBinance(available))
                    _binanceAvailable.emit(available)
                },
                onError = {
                    _binanceState.emit(BinanceAvailable.StateBinance(false))
                }
            )
        }
    }

    fun loadCandlestickSeriesData(
        symbol: String,
        interval: String,
        callBack: (() -> Unit)? = null
    ) {
        viewModelScope.launch(defaultDispatcher) {
            asFlowResult {
                repository.getCandlestickData(symbol, interval)
            }.safeCollect(
                onEach = {
                    val dataCandlesTick = it.data?.map { list ->
                        val time = (list[0] as Double).toLong()
                            .convertToTimeUTC()
                        BarData(
                            time = Time.Utc(time),
                            high = (list[2] as String).toFloat(),
                            low = (list[3] as String).toFloat(),
                            open = (list[1] as String).toFloat(),
                            close = (list[4] as String).toFloat()
                        )
                    } ?: return@safeCollect
                    val dataVolume = it.data?.map { list ->
                        val time = (list[0] as Double).toLong()
                            .convertToTimeUTC()
                        HistogramData(
                            time = Time.Utc(time),
                            value = (list[5] as String).toFloat(),
                            color = if ((list[4] as String).toFloat() > (list[1] as String).toFloat())
                                Color.argb(204, 0, 150, 136).toIntColor()
                            else Color.argb(204, 255, 82, 82).toIntColor()
                        )
                    } ?: return@safeCollect
                    _candlestickSeriesState.emit(
                        CandlestickSeries.CandlestickSeriesData(
                            seriesData = DataChart(dataCandlesTick, SeriesType.CANDLESTICK),
                            volumeData = DataChart(dataVolume, SeriesType.HISTOGRAM)
                        )
                    )
                    callBack?.invoke()
                },
                onError = {
                    Timber.e(it)
                }
            )
        }
    }

    init {
        onConnectStream()
        chartInterval = BnTimePeriod.Minutes5
        viewModelScope.launch {
            chartTypeObservable.asFlow().collect { chartType ->
                val tabItems = BnTimePeriod.entries.map {
                    TabItem(it.value, it == chartType, it)
                }
                tabItemsLiveData.postValue(tabItems)
            }
        }

    }

    override fun onCleared() {
        super.onCleared()
        sendUnSubscribe(
            paramsSendSub
        )
        onClear.update { true }
    }

    private var paramsSendSub = mutableListOf<String>()
    fun sendSubscribe(params: List<String>) {
        repository.sendSubscribe(
            Subscribe(
                method = "SUBSCRIBE",
                id = id,
                params = params//listOf("btcusdt@kline_5m", "btcusdt@ticker")
            )
        )
        paramsSendSub.addAll(params)
    }

    private fun sendUnSubscribe(params: List<String>) {
        id++
        repository.sendSubscribe(
            Subscribe(
                method = "UNSUBSCRIBE",
                id = id,
                params = params
            )
        )
    }

    fun onSelectChartInterval(
        time: BnTimePeriod,
        coinSymbol: String,
        coinCode: String,
    ) {
        val oldChartInterval = chartInterval
        this.chartInterval = time
        val tmpChartInterval = chartInterval ?: return

        remove.update { true }
        loadCandlestickSeriesData(
            coinSymbol,
            tmpChartInterval.value
        ) {
            id++
            sendUnSubscribe(
                listOf(
                    coinCode.toSymbolKlineUseSocketBinance(oldChartInterval!!.value)
                )
            )
            sendSubscribe(
                listOf(
                    coinCode.toSymbolKlineUseSocketBinance(tmpChartInterval.value)
                )
            )
        }
    }

    private fun onConnectStream() {
        viewModelScope.launch {
            repository.observeWebSocket.collect {
                when (it) {
                    is WebSocket.Event.OnConnectionOpened<*> -> {
                        Timber.d("Connection open")
                        //webSocketOpened.postValue(EventLiveData(Unit))
                    }

                    is WebSocket.Event.OnMessageReceived -> {
                        Timber.d("Message received $it")
                    }//Timber.d("Message received")
                    is WebSocket.Event.OnConnectionClosing -> {
                        Timber.d("Connection closing")
                    }

                    is WebSocket.Event.OnConnectionClosed -> {
                        Timber.d("Connection closed")
                    }

                    is WebSocket.Event.OnConnectionFailed -> {
                        Timber.d("Connection failed")
                    }
                }
            }
        }
    }
}


sealed interface BinanceAvailable {
    data object Loading : BinanceAvailable
    data class StateBinance(val available: Boolean) : BinanceAvailable
}

sealed interface CandlestickSeries {
    data object Loading : CandlestickSeries

    data class CandlestickSeriesData(
        val seriesData: DataChart,
        val volumeData: DataChart
    ) : CandlestickSeries
}
