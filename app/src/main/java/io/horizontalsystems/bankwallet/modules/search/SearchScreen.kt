package io.horizontalsystems.bankwallet.modules.search

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.wallet.blockchain.bitcoin.BuildConfig
import com.wallet.blockchain.bitcoin.R
import io.horizontalsystems.bankwallet.core.AdType
import io.horizontalsystems.bankwallet.core.MaxTemplateNativeAdViewComposable
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchResults
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchSection
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchViewModel
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchViewModel.MainPage
import io.horizontalsystems.bankwallet.rememberAdNativeView
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subheadR_leah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    searchViewModel: MarketSearchViewModel,
    navController: NavController,
) {
    var text by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val mainState by searchViewModel.mainState.collectAsStateWithLifecycle()
    val (adState, reloadAd) = rememberAdNativeView(
        BuildConfig.HOME_MARKET_NATIVE,
        adPlacements = "Search",
        searchViewModel
    )
    Box(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Box(
            Modifier
                .semantics { isTraversalGroup = true }
                .zIndex(1f)
                .fillMaxWidth()
        ) {
            val color = if (expanded) {
                SearchBarDefaults.colors(
                    containerColor = ComposeAppTheme.colors.tyler
                )
            } else {
                SearchBarDefaults.colors()
            }
            SearchBar(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .semantics {
                            traversalIndex = 0f
                        },
                inputField = {
                    SearchBarDefaults.InputField(
                        onSearch = {
                            expanded = false
                            text = ""
                        },
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        placeholder = { Text(stringResource(R.string.Market_Search_Hint)) },
                        leadingIcon = {
                            if (expanded) {
                                IconButton(onClick = {
                                    expanded = false
                                    text = ""
                                    searchViewModel.onClearSearch()
                                }) {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "back"
                                    )
                                }
                            } else {
                                Icon(Icons.Rounded.Search, contentDescription = null)
                            }
                        },
                        trailingIcon = {
                            if (text.isBlank()) {
                                IconButton(onClick = {
                                    expanded = false
                                    navController.slideFromRight(R.id.marketSearchFragment)
                                }) {
                                    Icon(Icons.Rounded.MoreVert, contentDescription = null)
                                }
                            } else {
                                IconButton(onClick = {
                                    text = ""
                                    searchViewModel.onClearSearch()
                                }) {
                                    Icon(
                                        Icons.Rounded.Clear,
                                        contentDescription = "clear"
                                    )
                                }
                            }
                        },
                        query = text,
                        onQueryChange = {
                            text = it
                            searchViewModel.searchByQuery(it)
                        },
                    )
                },
                expanded = expanded,
                onExpandedChange = { expanded = it },
                colors = color
            ) {
                Crossfade(targetState = text.isBlank(), label = "") { isSearch ->
                    val uiSearchState = searchViewModel.uiState
                    val itemSections =
                        if (!isSearch && uiSearchState.page is MarketSearchViewModel.Page.SearchResults)
                            mapOf(
                                MarketSearchSection.SearchResults to uiSearchState.page.items
                            )
                        else if (uiSearchState.page is MarketSearchViewModel.Page.Discovery)
                            mapOf(
                                MarketSearchSection.Popular to uiSearchState.page.popular.take(6),
                            ) else mapOf()

                    MarketSearchResults(
                        modifier = Modifier,
                        uiSearchState.listId,
                        itemSections = itemSections,
                        onCoinClick = { coin, _ ->
                            expanded = false
                            text = ""
                            uiSearchState.page
                            searchViewModel.onCoinOpened(coin)
                            navController.slideFromRight(
                                R.id.coinFragment,
                                CoinFragment.Input(coin.uid)
                            )
                            searchViewModel.onClearSearch()
                        },
                        backgroundColor = MaterialTheme.colorScheme.inverseOnSurface,
                        onFavoriteClick = { favorite, coinUid ->
                            searchViewModel.onFavoriteClick(favorite, coinUid)
                        }
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 80.dp,
                    end = 16.dp,
                    bottom = 16.dp
                ),
            ) {
                discovery(
                    mainState = mainState,
                    navController = navController,
                    interestsItemModifier = Modifier.layout { measurable, constraints ->
                        val placeable = measurable.measure(
                            constraints.copy(
                                maxWidth = constraints.maxWidth + 32.dp.roundToPx(),
                            ),
                        )
                        layout(placeable.width, placeable.height) {
                            placeable.place(0, 0)
                        }
                    },
                    onRetry = {
                        searchViewModel.fetchItems()
                    }
                )
                item {
                    MaxTemplateNativeAdViewComposable(
                        adState,
                        AdType.SMALL,
                        navController,
                        false
                    )
                }
            }

        }
    }
}

private fun LazyListScope.discovery(
    interestsItemModifier: Modifier = Modifier,
    mainState: MainPage,
    navController: NavController,
    onRetry: () -> Unit
) {
    item {
        when (mainState) {
            is MainPage.LoadingPage -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                    )
                }
            }

            is MainPage.LoadedPage -> {
                Column(modifier = interestsItemModifier) {
                    subhead2_leah(
                        modifier = Modifier.padding(16.dp),
                        text = "Discovery",
                        maxLines = 1,
                    )

                    LazyHorizontalGrid(
                        rows = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = 0.dp,
                            end = 16.dp,
                            bottom = 16.dp
                        ),
                        modifier = Modifier
                            // LazyHorizontalGrid has to be constrained in height.
                            // However, we can't set a fixed height because the horizontal grid contains
                            // vertical text that can be rescaled.
                            // When the fontScale is at most 1, we know that the horizontal grid will be at most
                            // 240dp tall, so this is an upper bound for when the font scale is at most 1.
                            // When the fontScale is greater than 1, the height required by the text inside the
                            // horizontal grid will increase by at most the same factor, so 240sp is a valid
                            // upper bound for how much space we need in that case.
                            // The maximum of these two bounds is therefore a valid upper bound in all cases.
                            .heightIn(
                                max = max(
                                    280.dp,
                                    with(LocalDensity.current) { 400.sp.toDp() })
                            )
                            .fillMaxWidth()
                            .semantics { traversalIndex = 1f },
                    ) {
                        items(mainState.items) { item ->
                            SingleTopicButton(
                                viewItem = item,
                                onClick = {
                                    navController.slideFromBottom(
                                        R.id.marketSectorFragment,
                                        item.coinCategory
                                    )
                                },
                            )
                        }
                    }
                }

            }

            else -> {
                ListErrorView(
                    stringResource(id = R.string.BalanceSyncError_Title),
                    onClick = onRetry
                )
            }
        }
    }
}