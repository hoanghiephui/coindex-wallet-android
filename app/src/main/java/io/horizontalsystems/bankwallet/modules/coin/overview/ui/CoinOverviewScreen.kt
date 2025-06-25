package io.horizontalsystems.bankwallet.modules.coin.overview.ui

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.tradingview.lightweightcharts.api.series.common.SeriesData
import com.wallet.blockchain.bitcoin.BuildConfig
import com.wallet.blockchain.bitcoin.R
import io.horizontalsystems.bankwallet.core.alternativeImageUrl
import io.horizontalsystems.bankwallet.BinanceAvailable
import io.horizontalsystems.bankwallet.BinanceViewModel
import io.horizontalsystems.bankwallet.CandlestickSeries
import io.horizontalsystems.bankwallet.core.AdType
import io.horizontalsystems.bankwallet.core.MaxTemplateNativeAdViewComposable
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.slideFromBottomForResult
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.stats.StatEntity
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.model.BnTimePeriod
import io.horizontalsystems.bankwallet.model.TickerStreamRepository
import io.horizontalsystems.bankwallet.modules.chart.ChartViewModel
import io.horizontalsystems.bankwallet.modules.coin.CoinLink
import io.horizontalsystems.bankwallet.modules.coin.overview.CoinOverviewModule
import io.horizontalsystems.bankwallet.modules.coin.overview.CoinOverviewViewModel
import io.horizontalsystems.bankwallet.modules.coin.overview.HudMessageType
import io.horizontalsystems.bankwallet.modules.coin.ui.CoinScreenTitle
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.RestoreSettingsViewModel
import io.horizontalsystems.bankwallet.modules.managewallets.ManageWalletsModule
import io.horizontalsystems.bankwallet.modules.managewallets.ManageWalletsViewModel
import io.horizontalsystems.bankwallet.modules.markdown.MarkdownFragment
import io.horizontalsystems.bankwallet.modules.zcashconfigure.ZcashConfigure
import io.horizontalsystems.bankwallet.rememberAdNativeView
import io.horizontalsystems.bankwallet.ui.compose.ChartBinance
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.HSSwipeRefresh
import io.horizontalsystems.bankwallet.ui.compose.ListenerChart
import io.horizontalsystems.bankwallet.ui.compose.TitlePrice
import io.horizontalsystems.bankwallet.ui.compose.animations.CrossSlide
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.CellFooter
import io.horizontalsystems.bankwallet.ui.compose.components.CellSingleLineClear
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.TabItem
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.bankwallet.ui.toSymbolKlineUseSocketBinance
import io.horizontalsystems.bankwallet.ui.toSymbolTickerUseSocketBinance
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.LinkType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

@Composable
fun CoinOverviewScreen(
    fullCoin: FullCoin,
    navController: NavController,
    binanceViewModel: BinanceViewModel = hiltViewModel()
) {
    val vmFactory by lazy { CoinOverviewModule.Factory(fullCoin) }
    val viewModel = viewModel<CoinOverviewViewModel>(factory = vmFactory)
    val chartViewModel = viewModel<ChartViewModel>(factory = vmFactory)

    val refreshing by viewModel.isRefreshingLiveData.observeAsState(false)
    val overview by viewModel.overviewLiveData.observeAsState()
    val viewState by viewModel.viewStateLiveData.observeAsState()
    val chartIndicatorsState = viewModel.chartIndicatorsState

    val view = LocalView.current
    val context = LocalContext.current

    val coinSymbol = "${fullCoin.coin.code}USDT"
    val (adState, reloadAd) = rememberAdNativeView(BuildConfig.HOME_MARKET_NATIVE,
        adPlacements = "CoinOverviewScreen", viewModel)

    LaunchedEffect(key1 = Unit, block = {
        binanceViewModel.onBinanceAvailable(coinSymbol)
    })
    val binanceAvailable by binanceViewModel.binanceState.collectAsStateWithLifecycle()
    val isSendStream by binanceViewModel.binanceAvailable.collectAsStateWithLifecycle()
    val priceTitle by binanceViewModel.symbolTickerStream.asLiveData().observeAsState()
    val seriesFlow = binanceViewModel.seriesFlow
    val volumeFlow = binanceViewModel.volumeFlow
    val candlestickSeries by binanceViewModel.candlestickSeriesState.collectAsStateWithLifecycle()
    val chartTabs by binanceViewModel.tabItemsLiveData.observeAsState(listOf())

    LoadStream(isSendStream, binanceViewModel, coinSymbol, fullCoin)

    viewModel.showHudMessage?.let {
        when (it.type) {
            HudMessageType.Error -> HudHelper.showErrorMessage(
                contenView = view,
                resId = it.text,
                icon = it.iconRes,
                iconTint = R.color.white
            )

            HudMessageType.Success -> HudHelper.showSuccessMessage(
                contenView = view,
                resId = it.text,
                icon = it.iconRes,
                iconTint = R.color.white
            )
        }

        viewModel.onHudMessageShown()
    }

    val vmFactory1 = remember { ManageWalletsModule.Factory() }
    val manageWalletsViewModel = viewModel<ManageWalletsViewModel>(factory = vmFactory1)
    val restoreSettingsViewModel = viewModel<RestoreSettingsViewModel>(factory = vmFactory1)

    if (restoreSettingsViewModel.openZcashConfigure != null) {
        restoreSettingsViewModel.zcashConfigureOpened()

        navController.slideFromBottomForResult<ZcashConfigure.Result>(R.id.zcashConfigure) {
            if (it.config != null) {
                restoreSettingsViewModel.onEnter(it.config)
            } else {
                restoreSettingsViewModel.onCancelEnterBirthdayHeight()
            }
        }
    }
    var currentPage by remember { mutableIntStateOf(CHART_DEFAULT) }
    var scrollingEnabled by remember { mutableStateOf(true) }
    HSSwipeRefresh(
        refreshing = refreshing,
        onRefresh = {
            viewModel.refresh()
            chartViewModel.refresh()
            reloadAd()
        },
        content = {
            Crossfade(viewState, label = "") { viewState ->
                when (viewState) {
                    ViewState.Loading -> {
                        Loading()
                    }
                    ViewState.Success -> {
                        overview?.let { overview ->
                            Column(
                                modifier = Modifier.verticalScroll(
                                    rememberScrollState(),
                                    scrollingEnabled
                                )
                            ) {
                                ViewChart(
                                    currentPage,
                                    showCandlestick = {
                                        CandlestickChart(
                                            viewModel,
                                            priceTitle,
                                            candlestickSeries,
                                            seriesFlow,
                                            volumeFlow,
                                            scrollingEnabled = {
                                                scrollingEnabled = it
                                            },
                                            binanceViewModel,
                                            chartTabs,
                                            coinSymbol,
                                            fullCoin
                                        )
                                    },
                                    showChartDefault = {
                                        CoinScreenTitle(
                                            fullCoin.coin.name,
                                            overview.marketCapRank,
                                            fullCoin.coin.imageUrl,
                                            fullCoin.coin.alternativeImageUrl,fullCoin.iconPlaceholder
                                        )

                                        Chart(chartViewModel = chartViewModel)
                                    }
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                CellUniversalLawrenceSection {
                                    RowUniversal(
                                        modifier = Modifier
                                            .height(52.dp)
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                    ) {
                                        subhead2_grey(text = stringResource(R.string.CoinPage_Indicators))
                                        Spacer(modifier = Modifier.weight(1f))
                                        Box(modifier = Modifier.size(30.dp)) {
                                            when (binanceAvailable) {
                                                is BinanceAvailable.Loading -> {
                                                    Loading()
                                                }

                                                is BinanceAvailable.StateBinance -> {
                                                    if ((binanceAvailable as BinanceAvailable.StateBinance).available) {
                                                        ButtonSecondaryCircle(
                                                            icon = if (currentPage != CANDLESTICK) R.drawable.baseline_candlestick_chart_24
                                                            else R.drawable.ic_chart_24
                                                        ) {
                                                            currentPage =
                                                                if (currentPage != CANDLESTICK) CANDLESTICK else CHART_DEFAULT
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        HSpacer(width = 8.dp)
                                        if (chartIndicatorsState.hasActiveSubscription) {
                                            if (currentPage == CHART_DEFAULT) {
                                                if (chartIndicatorsState.enabled) {
                                                    ButtonSecondaryDefault(
                                                        title = stringResource(id = R.string.Button_Hide),
                                                        onClick = {
                                                            viewModel.disableChartIndicators()

                                                    stat(
                                                            page = StatPage.CoinOverview,
                                                            event = StatEvent.ToggleIndicators(false)
                                                        )
                                                    })
                                                } else {
                                                    ButtonSecondaryDefault(
                                                        title = stringResource(id = R.string.Button_Show),
                                                        onClick = {
                                                            viewModel.enableChartIndicators()
                                                            stat(
                                                            page = StatPage.CoinOverview,
                                                            event = StatEvent.ToggleIndicators(true)
                                                        )
                                                        }
                                                    )
                                                }
                                                HSpacer(width = 8.dp)
                                            }
                                            ButtonSecondaryCircle(
                                                icon = R.drawable.ic_setting_20
                                            ) {
                                                navController.slideFromRight(R.id.indicatorsFragment)

                                                stat(
                                                    page = StatPage.CoinOverview,
                                                    event = StatEvent.Open(StatPage.Indicators)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                MaxTemplateNativeAdViewComposable(adState, AdType.SMALL, navController)

                                if (overview.marketData.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    MarketData(overview.marketData)
                                }

                                if (overview.roi.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    CellSingleLineClear(borderTop = true) {
                                        body_leah(text = stringResource(R.string.CoinPage_ROI_Title, viewModel.fullCoin.coin.code))
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Roi(overview.roi, navController)
                                }

                                viewModel.tokenVariants?.let { tokenVariants ->
                                    Spacer(modifier = Modifier.height(24.dp))
                                    TokenVariants(
                                        tokenVariants = tokenVariants,
                                        onClickAddToWallet = {
                                            manageWalletsViewModel.enable(it)

                                            stat(
                                                page = StatPage.CoinOverview,
                                                event = StatEvent.AddToWallet
                                            )
                                        },
                                        onClickRemoveWallet = {
                                            manageWalletsViewModel.disable(it)

                                            stat(
                                                page = StatPage.CoinOverview,
                                                event = StatEvent.RemoveFromWallet
                                            )
                                        },
                                        onClickCopy = {
                                            TextHelper.copyText(it)
                                            HudHelper.showSuccessMessage(
                                                view,
                                                R.string.Hud_Text_Copied
                                            )

                                            stat(
                                                page = StatPage.CoinOverview,
                                                event = StatEvent.Copy(StatEntity.ContractAddress)
                                            )
                                        },
                                        onClickExplorer = {
                                            LinkHelper.openLinkInAppBrowser(context, it)

                                            stat(
                                                page = StatPage.CoinOverview,
                                                event = StatEvent.Open(StatPage.ExternalBlockExplorer)
                                            )
                                        },
                                    )
                                }

                                if (overview.about.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    About(overview.about)
                                }

                                if (overview.links.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Links(overview.links) { onClick(it, context, navController) }
                                }

                                Spacer(modifier = Modifier.height(32.dp))
                                CellFooter(text = stringResource(id = R.string.Market_PoweredByApi))
                            }
                        }

                    }

                    is ViewState.Error -> {
                        ListErrorView(
                            stringResource(id = R.string.BalanceSyncError_Title),
                            onClick = {
                                viewModel.retry()
                                chartViewModel.refresh()
                            })
                    }

                    null -> {}
                }
            }
        },
    )
}

@Composable
private fun LoadStream(
    isSendStream: Boolean,
    binanceViewModel: BinanceViewModel,
    coinSymbol: String,
    fullCoin: FullCoin
) {
    LaunchedEffect(key1 = isSendStream, block = {
        if (isSendStream) {
            delay(1000)
            binanceViewModel.loadCandlestickSeriesData(
                coinSymbol,
                BnTimePeriod.Minutes5.value
            )
            binanceViewModel.sendSubscribe(
                listOf(
                    fullCoin.coin.code.toSymbolTickerUseSocketBinance(),
                    fullCoin.coin.code.toSymbolKlineUseSocketBinance(BnTimePeriod.Minutes5.value)
                )
            )
        }
    })
}

@Composable
private fun CandlestickChart(
    viewModel: CoinOverviewViewModel,
    priceTitle: TickerStreamRepository?,
    candlestickSeries: CandlestickSeries,
    seriesFlow: Flow<SeriesData>,
    volumeFlow: Flow<SeriesData>,
    scrollingEnabled: (Boolean) -> Unit,
    binanceViewModel: BinanceViewModel,
    chartTabs: List<TabItem<BnTimePeriod>>,
    coinSymbol: String,
    fullCoin: FullCoin,
) {
    TitlePrice(viewModel.currency, priceTitle?.data)
    AnimatedContent(targetState = candlestickSeries) { state ->
        when (state) {
            CandlestickSeries.Loading -> {
                Box(modifier = Modifier.height(300.dp)) {
                    Loading()
                }
            }

            is CandlestickSeries.CandlestickSeriesData -> {
                val seriesData = state.seriesData
                val volumeData = state.volumeData
                ChartBinance(
                    seriesData,
                    volumeData,
                    seriesFlow,
                    volumeFlow,
                    androidx.lifecycle.compose.LocalLifecycleOwner.current,
                    object : ListenerChart {
                        override fun onChartTouchDown() {
                            scrollingEnabled(false)
                        }

                        override fun onChartTouchUp() {
                            scrollingEnabled(true)
                        }

                    },
                    binanceViewModel.onClear,
                    tabItems = chartTabs,
                    onSelectTab = {
                        binanceViewModel.onSelectChartInterval(
                            it,
                            coinSymbol,
                            fullCoin.coin.code
                        )
                    },
                    binanceViewModel.remove
                )
            }
        }

    }
}

private fun onClick(coinLink: CoinLink, context: Context, navController: NavController) {
    val absoluteUrl = getAbsoluteUrl(coinLink)

    when (coinLink.linkType) {
        LinkType.Guide -> {
            navController.slideFromRight(
                R.id.markdownFragment,
                MarkdownFragment.Input(absoluteUrl, true)
            )
        }

        else -> LinkHelper.openLinkInAppBrowser(context, absoluteUrl)
    }

    when(coinLink.linkType) {
        LinkType.Guide -> stat(page = StatPage.CoinOverview, event = StatEvent.Open(StatPage.Guide))
        LinkType.Website -> stat(
            page = StatPage.CoinOverview,
            event = StatEvent.Open(StatPage.ExternalCoinWebsite)
        )
        LinkType.Whitepaper -> stat(
            page = StatPage.CoinOverview,
            event = StatEvent.Open(StatPage.ExternalCoinWhitePaper)
        )
        LinkType.Twitter -> stat(
            page = StatPage.CoinOverview,
            event = StatEvent.Open(StatPage.ExternalTwitter)
        )
        LinkType.Telegram -> stat(
            page = StatPage.CoinOverview,
            event = StatEvent.Open(StatPage.ExternalTelegram)
        )
        LinkType.Reddit -> stat(
            page = StatPage.CoinOverview,
            event = StatEvent.Open(StatPage.ExternalReddit)
        )
        LinkType.Github -> stat(
            page = StatPage.CoinOverview,
            event = StatEvent.Open(StatPage.ExternalGithub)
        )
    }
}

private fun getAbsoluteUrl(coinLink: CoinLink) = when (coinLink.linkType) {
    LinkType.Twitter -> "https://twitter.com/${coinLink.url}"
    LinkType.Telegram -> "https://t.me/${coinLink.url}"
    else -> coinLink.url
}

@Preview
@Composable
fun LoadingPreview() {
    ComposeAppTheme {
        Loading()
    }
}

@Composable
fun Error(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        subhead2_grey(text = message)
    }
}

@Composable
fun Loading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = ComposeAppTheme.colors.grey,
            strokeWidth = 2.dp
        )
    }
}

@Composable
fun ViewChart(
    currentPage: Int,
    showCandlestick: @Composable (ColumnScope.() -> Unit),
    showChartDefault: @Composable (ColumnScope.() -> Unit)
) {
    AnimatedContent(
        targetState = currentPage,
    ) { screen ->
        when (screen) {
            CHART_DEFAULT -> {
                Column(content = showChartDefault)
            }

            CANDLESTICK -> {
                Column(content = showCandlestick)
            }
        }
    }
}

const val CANDLESTICK = 1
const val CHART_DEFAULT = 0
