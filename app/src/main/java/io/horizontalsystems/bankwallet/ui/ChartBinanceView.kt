package io.horizontalsystems.bankwallet.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.asLiveData
import com.applovin.sdk.AppLovinSdkUtils.dpToPx
import com.tradingview.lightweightcharts.api.chart.models.color.IntColor
import com.tradingview.lightweightcharts.api.chart.models.color.surface.SolidColor
import com.tradingview.lightweightcharts.api.chart.models.color.toIntColor
import com.tradingview.lightweightcharts.api.interfaces.ChartApi
import com.tradingview.lightweightcharts.api.interfaces.SeriesApi
import com.tradingview.lightweightcharts.api.options.models.CandlestickSeriesOptions
import com.tradingview.lightweightcharts.api.options.models.HistogramSeriesOptions
import com.tradingview.lightweightcharts.api.options.models.crosshairOptions
import com.tradingview.lightweightcharts.api.options.models.gridLineOptions
import com.tradingview.lightweightcharts.api.options.models.gridOptions
import com.tradingview.lightweightcharts.api.options.models.layoutOptions
import com.tradingview.lightweightcharts.api.options.models.priceScaleMargins
import com.tradingview.lightweightcharts.api.options.models.priceScaleOptions
import com.tradingview.lightweightcharts.api.options.models.timeScaleOptions
import com.tradingview.lightweightcharts.api.series.common.SeriesData
import com.tradingview.lightweightcharts.api.series.enums.CrosshairMode
import com.tradingview.lightweightcharts.api.series.models.BarData
import com.tradingview.lightweightcharts.api.series.models.HistogramData
import com.tradingview.lightweightcharts.api.series.models.PriceFormat
import com.tradingview.lightweightcharts.api.series.models.PriceScaleId
import com.tradingview.lightweightcharts.api.series.models.Time
import com.tradingview.lightweightcharts.view.ChartsView
import com.tradingview.lightweightcharts.view.gesture.TouchDelegate
import com.wallet.blockchain.bitcoin.R
import com.wallet.blockchain.bitcoin.databinding.ViewChartBinanceBinding
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.model.DataChart
import io.horizontalsystems.bankwallet.ui.compose.ListenerChart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ChartBinanceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private val binding = ViewChartBinanceBinding.inflate(LayoutInflater.from(context), this)
    private lateinit var areaSeries: SeriesApi
    private var realtimeDataJob: Job? = null
    private var realtimeVolumeDataJob: Job? = null
    private lateinit var listener: ListenerChart
    private lateinit var lifecycleScope: LifecycleOwner
    private lateinit var seriesFlow: Flow<SeriesData>
    private lateinit var volumeFlow: Flow<SeriesData>
    private var isSystemInDarkTheme = false
    private lateinit var onClear: MutableStateFlow<Boolean>
    private lateinit var onRemove: MutableStateFlow<Boolean>

    fun setOnClear(onClear: MutableStateFlow<Boolean>) {
        this.onClear = onClear
    }

    fun setOnRemove(onClear: MutableStateFlow<Boolean>) {
        this.onRemove = onClear
    }

    fun setListener(listener: ListenerChart) {
        this.listener = listener
    }

    fun setSystemInDarkTheme(isSystemInDarkTheme: Boolean) {
        this.isSystemInDarkTheme = isSystemInDarkTheme
    }

    fun bindDataView(
        data: DataChart?,
        dataVolume: DataChart?
    ) {
        if (data != null) {
            createSeriesWithData(data, PriceScaleId.RIGHT, binding.chart.api) { series ->
                loadRealTimeChard(series)
            }
        }
        if (dataVolume != null) {
            createVolumeSeriesWithData(
                dataVolume,
                PriceScaleId.LEFT,
                binding.chart.api
            ) { series ->
                loadRealTimeVolume(series)
            }
        }
    }

    private fun loadRealTimeVolume(series: SeriesApi) {
        if (::lifecycleScope.isInitialized)
            volumeFlow.asLiveData().observe(lifecycleScope) {
                series.update(it)
            }
    }

    private fun loadRealTimeChard(series: SeriesApi) {
        if (::lifecycleScope.isInitialized)
            seriesFlow.asLiveData().observe(lifecycleScope) {
                series.update(it)
            }
    }

    fun bindView() {
        subscribeOnChartReady(binding.chart)
        applyChartOptions()
        attachTooltipToCrosshair()
        binding.chart.addTouchDelegate(object : TouchDelegate {
            override fun beforeTouchEvent(view: ViewGroup) {

            }

            override fun onTouchEvent(view: ViewGroup, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        listener.onChartTouchDown()
                    }

                    MotionEvent.ACTION_MOVE -> {}

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        listener.onChartTouchUp()
                    }
                }

                return false
            }

        })

        onSubClear()
    }

    private fun subscribeOnChartReady(view: ChartsView) {
        view.subscribeOnChartStateChange { state ->
            when (state) {
                is ChartsView.State.Preparing -> {
                    view.isInvisible = true
                }

                is ChartsView.State.Ready -> {
                    view.isVisible = true
                }

                is ChartsView.State.Error -> {
                    view.isInvisible = true
                    Toast.makeText(context, state.exception.localizedMessage, Toast.LENGTH_LONG)
                        .show()
                }
            }
        }
    }

    private fun applyChartOptions() {
        binding.chart.api.applyOptions {
            crosshair = crosshairOptions {
                mode = CrosshairMode.NORMAL
            }
            layout = layoutOptions {
                textColor =
                    if (!isSystemInDarkTheme) IntColor(Color.BLACK) else IntColor(Color.WHITE)
                background = SolidColor(ContextCompat.getColor(context, R.color.transparent))
            }
            timeScale = timeScaleOptions {
                timeVisible = true
                borderColor = "#D1D4DC".toIntColor()
                secondsVisible = true
            }
            rightPriceScale = priceScaleOptions {
                scaleMargins = priceScaleMargins {
                    top = 0.1f
                    bottom = 0.4f
                }
                borderVisible = false
            }
            leftPriceScale = priceScaleOptions {
                scaleMargins = priceScaleMargins {
                    top = 0.7f
                    bottom = 0.05f
                }
                borderVisible = false
            }
            grid = gridOptions {
                horzLines = gridLineOptions {
                    color = "#F0F3FA".toIntColor()
                    visible = false
                }
                vertLines = gridLineOptions {
                    color = "#F0F3FA".toIntColor()
                    visible = false
                }
            }
        }
        val timeScale = binding.chart.api.timeScale
        timeScale.subscribeVisibleTimeRangeChange {
            timeScale.scrollPosition {
                binding.btnRealtime.isVisible = it < 0
            }
        }
        binding.btnRealtime?.setOnClickListener {
            timeScale.scrollToRealTime()
        }
    }

    private fun createSeriesWithData(
        data: DataChart,
        priceScale: PriceScaleId,
        chartApi: ChartApi,
        onSeriesCreated: (SeriesApi) -> Unit
    ) {
        val count =
            (if (data.list.first() is BarData) (data.list.first() as BarData).open.countDigits() else 2)
        chartApi.addCandlestickSeries(
            options = CandlestickSeriesOptions(
                borderVisible = false,
                lastValueVisible = true,
                priceScaleId = priceScale,
                priceFormat = PriceFormat.priceFormatBuiltIn(
                    type = PriceFormat.Type.PRICE,
                    precision = count,
                    minMove = 0.00000001f,
                ),
                wickVisible = true,
                priceLineVisible = true
            ),
            onSeriesCreated = { api ->
                areaSeries = api
                api.setData(data.list)
                onSeriesCreated(api)
            }
        )
    }

    private fun createVolumeSeriesWithData(
        data: DataChart,
        priceScale: PriceScaleId,
        chartApi: ChartApi,
        onSeriesCreated: (SeriesApi) -> Unit
    ) {
        val count =
            (if (data.list.first() is HistogramData) (data.list.first() as HistogramData).value.countDigits() else 0)
        chartApi.addHistogramSeries(
            options = HistogramSeriesOptions(
                color = Color.parseColor("#26a69a").toIntColor(),
                priceFormat = PriceFormat.priceFormatBuiltIn(
                    type = PriceFormat.Type.VOLUME,
                    precision = count,
                    minMove = 1f,
                ),
                priceScaleId = priceScale,
                lastValueVisible = false,
                priceLineVisible = true
            ),
            onSeriesCreated = { api ->
                api.setData(data.list)
                onSeriesCreated(api)
            }
        )
    }


    private fun attachTooltipToCrosshair() {
        val tooltip = TooltipChart(context)
        val layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        val displayMetrics = context.resources.displayMetrics
        val dpScreenHeight = displayMetrics.heightPixels / displayMetrics.density
        binding.chart.api.unsubscribeCrosshairMove {
            this.removeView(tooltip)
        }

        binding.chart.api.subscribeCrosshairMove { event ->

            if (event.seriesData.isNullOrEmpty()) {
                this.removeView(tooltip)
                return@subscribeCrosshairMove
            }

            val high = event.seriesData?.firstOrNull()?.prices?.high ?: 0f
            val open = event.seriesData?.firstOrNull()?.prices?.open ?: 0f
            val close = event.seriesData?.firstOrNull()?.prices?.close ?: 0f
            val low = event.seriesData?.firstOrNull()?.prices?.low ?: 0f
            val volume = event.seriesData?.lastOrNull()?.prices?.value ?: 0f
            val time = (event.time as Time.Utc).date.convertToTimeString()
            if (::areaSeries.isInitialized) {
                areaSeries.priceToCoordinate(high) { coordinate ->

                    if (coordinate == null) {
                        return@priceToCoordinate
                    }

                    if (this.children.last() is TooltipChart) {
                        this.removeViewAt(this.childCount - 1)
                    }

                    val coordinateY =
                        0f.coerceAtLeast((dpScreenHeight - tooltip.height).coerceAtMost(coordinate))

                    layoutParams.leftMargin = dpToPx(context, event.point?.x?.toInt() ?: 0)
                    layoutParams.topMargin = dpToPx(context, coordinateY.toInt())
                    tooltip.layoutParams = layoutParams
                    tooltip.requestLayout()
                    tooltip.setDate(time)
                    tooltip.setClose(close.toValueString(close.countDigits()))
                    tooltip.setOpen(open.toValueString(open.countDigits()))
                    tooltip.setLow(low.toValueString(low.countDigits()))
                    tooltip.setHigh(high.toValueString(high.countDigits()))
                    tooltip.setVolume(volume.toValueString())
                    this.addView(tooltip, this.childCount)
                }
            }
        }
    }

    fun setLifecycleScope(lifecycleScope: LifecycleOwner) {
        this.lifecycleScope = lifecycleScope
    }

    fun setSeriesFlow(seriesFlow: Flow<SeriesData>) {
        this.seriesFlow = seriesFlow
    }

    fun setVolumeFlow(volumeFlow: Flow<SeriesData>) {
        this.volumeFlow = volumeFlow
    }


    private fun onSubClear() {
        onClear.asLiveData().observe(lifecycleScope) {
            if (it) {
                binding.chart.removeAllViews()
                binding.chart.dispatchFinishTemporaryDetach()
                realtimeDataJob?.cancel()
                realtimeVolumeDataJob?.cancel()
            }
        }
    }
}

fun Float.countDigits(): Int = when {
    this.toBigDecimal().toString().contains(".") -> this.toBigDecimal().toString()
        .substring(this.toBigDecimal().toString().indexOf(".") + 1).count()

    this.toBigDecimal().toString().contains(",") -> this.toBigDecimal().toString()
        .substring(this.toBigDecimal().toString().indexOf(",") + 1).count()

    else -> 2
}

fun Date.convertToTimeString(): String {
    val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).apply {
        this.timeZone = TimeZone.getDefault()
    }
    val timeZone: TimeZone = TimeZone.getTimeZone("UTC")
    val formatTo = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).apply {
        this.timeZone = timeZone
    }
    return formatTo.format(this)
}

fun Float.toValueString(maxDigit: Int = 2): String {
    return App.numberFormatter.format(
        this.toBigDecimal(),
        0,
        maxDigit
    )
}

fun String.toSymbolTickerUseSocketBinance(): String {
    val mCurrency = "USD"
    return this.plus("${mCurrency}t@ticker").lowercase()
}

fun String.toSymbolKlineUseSocketBinance(time: String = "5m"): String {
    val mCurrency = "USD"
    return this.plus("${mCurrency}t@kline_$time").lowercase()
}
