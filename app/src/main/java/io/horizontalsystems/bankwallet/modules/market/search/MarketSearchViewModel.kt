package io.horizontalsystems.bankwallet.modules.market.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.BaseViewModel
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.market.MarketDataValue
import io.horizontalsystems.bankwallet.modules.market.Value
import io.horizontalsystems.bankwallet.modules.market.sortedByDescendingNullLast
import io.horizontalsystems.bankwallet.modules.market.topsectors.TopSectorViewItem
import io.horizontalsystems.bankwallet.modules.market.topsectors.TopSectorWithDiff
import io.horizontalsystems.bankwallet.modules.market.topsectors.TopSectorsRepository
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.FullCoin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

class MarketSearchViewModel(
    private val marketFavoritesManager: MarketFavoritesManager,
    private val marketSearchService: MarketSearchService,
    private val marketDiscoveryService: MarketDiscoveryService,
    private val topSectorsRepository: TopSectorsRepository,
    private val currencyManager: CurrencyManager,
    private val numberFormatter: IAppNumberFormatter
) : BaseViewModel() {
    private var searchState = marketSearchService.stateFlow.value
    private var discoveryState = marketDiscoveryService.stateFlow.value
    private var listId: String = ""
    private var page: Page = Page.Discovery(
        recent = coinItems(discoveryState.recent),
        popular = coinItems(discoveryState.popular),
    )

    var uiState by mutableStateOf(UiState(page, listId))
        private set
    private var _mainState: MutableStateFlow<MainPage> = MutableStateFlow(MainPage.LoadingPage)
    val mainState = _mainState.asStateFlow()

    init {
        viewModelScope.launch {
            marketSearchService.stateFlow.collect {
                handleUpdatedSearchState(it)
            }
        }
        viewModelScope.launch {
            marketDiscoveryService.stateFlow.collect {
                handleUpdatedDiscoveryState(it)
            }
        }
        viewModelScope.launch {
            marketFavoritesManager.dataUpdatedAsync.asFlow().collect {
                emitState()
            }
        }

        marketDiscoveryService.start()
        fetchItems()
    }

    private fun handleUpdatedDiscoveryState(discoveryState: MarketDiscoveryService.State) {
        this.discoveryState = discoveryState

        emitState()
    }

    private fun handleUpdatedSearchState(searchState: MarketSearchService.State) {
        this.searchState = searchState

        emitState()
    }

    fun searchByQuery(query: String) {
        marketSearchService.setQuery(query)
    }

    private fun coinItems(fullCoins: List<FullCoin>) =
        fullCoins.map {
            MarketSearchModule.CoinItem(
                it,
                marketFavoritesManager.isCoinInFavorites(it.coin.uid)
            )
        }

    private fun emitState() {
        if (searchState.query.isNotBlank()) {
            page = Page.SearchResults(coinItems(searchState.results))
            listId = searchState.query
        } else {
            page = Page.Discovery(
                coinItems(discoveryState.recent),
                coinItems(discoveryState.popular),
            )
            listId = ""
        }

        viewModelScope.launch {
            uiState = UiState(page, listId)
        }
    }

    fun onFavoriteClick(favourited: Boolean, coinUid: String) {
        if (favourited) {
            marketFavoritesManager.remove(coinUid)

            stat(page = StatPage.MarketSearch, event = StatEvent.RemoveFromWatchlist(coinUid))
        } else {
            marketFavoritesManager.add(coinUid)

            stat(page = StatPage.MarketSearch, event = StatEvent.AddToWatchlist(coinUid))
        }
    }

    fun onCoinOpened(coin: Coin) {
        marketDiscoveryService.addCoinToRecent(coin)
    }

    fun onClearSearch() {
        marketSearchService.setQuery("")
    }

    fun fetchItems(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    _mainState.update {
                        MainPage.LoadingPage
                    }
                    val topSectors =
                        topSectorsRepository.get(currencyManager.baseCurrency, forceRefresh)

                    val topCategoryWithDiffList = topSectors.map {
                        TopSectorWithDiff(
                            it.coinCategory,
                            it.coinCategory.diff24H,
                            it.topCoins
                        )
                    }
                    val sortedTopSectors =
                        topCategoryWithDiffList.sortedByDescendingNullLast { it.coinCategory.marketCap }
                    val items = sortedTopSectors.map { getViewItem(it) }
                    _mainState.update {
                        MainPage.LoadedPage(items)
                    }
                } catch (e: CancellationException) {
                    // no-op
                } catch (e: Throwable) {
                    _mainState.update {
                        MainPage.ErrorPage
                    }
                }
            }
        }
    }

    private fun getViewItem(item: TopSectorWithDiff) =
        TopSectorViewItem(
            coinCategory = item.coinCategory,
            marketCapValue = item.coinCategory.marketCap?.let {
                numberFormatter.formatFiatShort(
                    it,
                    currencyManager.baseCurrency.symbol,
                    2
                )
            },
            changeValue = item.diff?.let {
                MarketDataValue.DiffNew(Value.Percent(it))
            },
            coin1 = item.topCoins[0],
            coin2 = item.topCoins[1],
            coin3 = item.topCoins[2],
        )

    data class UiState(
        val page: Page,
        val listId: String
    )

    sealed class Page {
        data class Discovery(
            val recent: List<MarketSearchModule.CoinItem>,
            val popular: List<MarketSearchModule.CoinItem>
        ) : Page()

        data class SearchResults(val items: List<MarketSearchModule.CoinItem>) : Page()
    }

    sealed class MainPage {
        data object LoadingPage : MainPage()
        data class LoadedPage(val items: List<TopSectorViewItem>) : MainPage()
        data object ErrorPage : MainPage()
    }
}
