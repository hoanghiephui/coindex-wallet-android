package io.horizontalsystems.bankwallet.modules.market.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.wallet.blockchain.bitcoin.R
import io.horizontalsystems.bankwallet.analytics.TrackScreenViewEvent
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.alternativeImageUrl
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.core.stats.statSection
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchModule.CoinItem
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderStick
import io.horizontalsystems.bankwallet.ui.compose.components.HsIconButton
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.HsImage
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.NiaBackground
import io.horizontalsystems.bankwallet.ui.compose.components.SectionItemBorderedRowUniversalClear
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.marketkit.models.Coin
import java.util.Optional

class MarketSearchFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        val viewModel = viewModel<MarketSearchViewModel>(
            factory = MarketSearchModule.Factory()
        )
        MarketSearchScreen(viewModel, navController)
    }

    override val logScreen: String
        get() = "MarketSearchFragment"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketSearchScreen(viewModel: MarketSearchViewModel, navController: NavController) {
    val uiState = viewModel.uiState

    NiaBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.background,
            topBar = {
                AppBar(
                    title = stringResource(R.string.Market_Search),
                    navigationIcon = {
                        HsBackButton(onClick = { navController.popBackStack() })
                    },
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Market_Filters),
                            enabled = true,
                            onClick = {
                                navController.slideFromRight(R.id.marketAdvancedSearchFragment)
                            },
                            icon = R.drawable.baseline_filter_list_24
                        )
                    )
                )
            }
        ) {
            val itemSections = when (uiState.page) {
                is MarketSearchViewModel.Page.Discovery -> {
                    mapOf(
                        MarketSearchSection.Recent to uiState.page.recent,
                        MarketSearchSection.Popular to uiState.page.popular,
                    )
                }

                is MarketSearchViewModel.Page.SearchResults -> {
                    mapOf(
                        MarketSearchSection.SearchResults to uiState.page.items
                    )
                }
            }

            MarketSearchResults(
                modifier = Modifier.padding(it),
                uiState.listId,
                itemSections = itemSections,
                onCoinClick = { coin, section ->
                    viewModel.onCoinOpened(coin)
                    navController.slideFromRight(
                        R.id.coinFragment,
                        CoinFragment.Input(coin.uid)
                    )
                    stat(
                        page = StatPage.MarketSearch,
                        section = section.statSection,
                        event = StatEvent.OpenCoin(coin.uid)
                    )
                }
            ) { favorited, coinUid ->
                viewModel.onFavoriteClick(favorited, coinUid)
            }
        }
    }
}


enum class MarketSearchSection(val title: Optional<Int>) {
    Recent(Optional.of(R.string.Market_Search_Sections_RecentTitle)),
    Popular(Optional.of(R.string.Market_Search_Sections_PopularTitle)),
    SearchResults(Optional.empty())
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MarketSearchResults(
    modifier: Modifier = Modifier,
    vararg inputs: Any?,
    itemSections: Map<MarketSearchSection, List<CoinItem>>,
    onCoinClick: (Coin, MarketSearchSection) -> Unit,
    backgroundColor: Color = ComposeAppTheme.colors.tyler,
    onFavoriteClick: (Boolean, String) -> Unit
) {
    if (itemSections.all { (_, items) -> items.isEmpty() }) {
        ListEmptyView(
            text = stringResource(R.string.EmptyResults),
            icon = R.drawable.ic_not_found
        )
    } else {
        val keyboardController = LocalSoftwareKeyboardController.current
        val nestedScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    keyboardController?.hide()
                    return Offset.Zero
                }
            }
        }
        LazyColumn(
            modifier = modifier.nestedScroll(nestedScrollConnection),
            state = rememberSaveable(
                *inputs,
                saver = LazyListState.Saver
            ) {
                LazyListState()
            }
        ) {
            itemSections.forEach { (section, coinItems) ->
                section.title.ifPresent {
                    stickyHeader {
                        HeaderStick(
                            borderTop = true,
                            text = stringResource(id = section.title.get())
                        )
                    }
                }
                items(coinItems) { item ->
                    val coin = item.fullCoin.coin

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max)
                    ) {
                        Box(modifier = Modifier.background(backgroundColor)) {
                            MarketCoin(
                                coinUid = coin.uid,
                                coinCode = coin.code,
                                coinName = coin.name,
                                coinIconUrl = coin.imageUrl,
                                alternativeCoinIconUrl = coin.alternativeImageUrl,
                                coinIconPlaceholder = item.fullCoin.iconPlaceholder,
                                favourited = item.favourited,
                                onFavoriteClick = onFavoriteClick,
                                onClick = { onCoinClick(coin, section) },
                            )
                        }
                    }
                }
            }

            item {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = ComposeAppTheme.colors.steel10,
                )
            }
        }
    }
    TrackScreenViewEvent("MarketSearchResults")
}

@Composable
fun MarketCoin(
    coinUid: String,
    coinCode: String,
    coinName: String,
    coinIconUrl: String,
    alternativeCoinIconUrl: String?,
    coinIconPlaceholder: Int,
    favourited: Boolean,
    onFavoriteClick: (Boolean, String) -> Unit,
    onClick: () -> Unit,
) {

    SectionItemBorderedRowUniversalClear(
        borderTop = true,
        onClick = onClick
    ) {
        HsImage(
            url = coinIconUrl,
            alternativeUrl = alternativeCoinIconUrl,
            placeholder = coinIconPlaceholder,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(32.dp)
                .clip(CircleShape)
        )
        Column(
            modifier = Modifier.weight(1f)
        ) {
            body_leah(
                text = coinCode,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            subhead2_grey(
                text = coinName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HSpacer(16.dp)
        if (favourited) {
            HsIconButton(
                modifier = Modifier.size(20.dp),
                onClick = {
                    onFavoriteClick(true, coinUid)
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_heart_filled_20),
                    contentDescription = "heart icon button",
                    tint = ComposeAppTheme.colors.jacob
                )
            }
        } else {
            HsIconButton(
                modifier = Modifier.size(20.dp),
                onClick = {
                    onFavoriteClick(false, coinUid)
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_heart_20),
                    contentDescription = "heart icon button",
                    tint = ComposeAppTheme.colors.grey
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MarketCoinPreview() {
    val coin = Coin("ether", "Ethereum", "ETH")
    ComposeAppTheme {
        MarketCoin(
            coin.uid,
            coin.code,
            coin.name,
            coin.imageUrl,
            null,
            R.drawable.coin_placeholder,
            false,
            { _, _ -> },
            {}
        )
    }
}
