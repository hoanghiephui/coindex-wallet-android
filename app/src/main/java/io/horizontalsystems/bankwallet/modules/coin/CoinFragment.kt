package io.horizontalsystems.bankwallet.modules.coin

import android.os.Parcelable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import com.applovin.mediation.ads.MaxRewardedAd
import com.wallet.blockchain.bitcoin.BuildConfig
import com.wallet.blockchain.bitcoin.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.core.stats.statTab
import io.horizontalsystems.bankwallet.modules.coin.analytics.CoinAnalyticsScreen
import io.horizontalsystems.bankwallet.modules.coin.coinmarkets.CoinMarketsScreen
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.CoinOverviewScreen
import io.horizontalsystems.bankwallet.ui.AdMaxRewardedLoader
import io.horizontalsystems.bankwallet.ui.AdRewardedCallback
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.TabItem
import io.horizontalsystems.bankwallet.ui.compose.components.Tabs
import io.horizontalsystems.bankwallet.ui.compose.components.body_jacob
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

class CoinFragment : BaseComposeFragment(), AdRewardedCallback {
    private val adMaxRewardedLoader = AdMaxRewardedLoader(this)
    private var viewModel: CoinViewModel? = null

    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<Input>()
        val coinUid = input?.coinUid ?: ""
        viewModel = coinViewModel(coinUid)

        CoinScreen(
            coinUid,
            viewModel,
            navController,
            childFragmentManager
        ) {
            adMaxRewardedLoader.createRewardedAd(requireActivity(), BuildConfig.COIN_REWARD)
        }
    }

    override val logScreen: String
        get() = "CoinFragment"

    private fun coinViewModel(
        coinUid: String
    ): CoinViewModel? = try {
        val viewModel by navGraphViewModels<CoinViewModel>(R.id.coinFragment) {
            CoinModule.Factory(coinUid)
        }
        viewModel
    } catch (e: Exception) {
        null
    }

    override fun onLoaded(rewardedAd: MaxRewardedAd) {
        if (rewardedAd.isReady) {
            rewardedAd.showAd(requireActivity())
        }
    }

    override fun onAdRewardLoadFail() {}

    override fun onUserRewarded(amount: Int) {
        viewModel?.onFavoriteClick()
    }

    override fun onShowFail() {}

    @Parcelize
    data class Input(val coinUid: String) : Parcelable
}

@Composable
fun CoinScreen(
    coinUid: String,
    coinViewModel: CoinViewModel?,
    navController: NavController,
    fragmentManager: FragmentManager,
    openAds: () -> Unit
) {
    if (coinViewModel != null) {
        CoinTabs(coinViewModel, navController, fragmentManager, openAds)
    } else {
        CoinNotFound(coinUid, navController)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinTabs(
    viewModel: CoinViewModel,
    navController: NavController,
    fragmentManager: FragmentManager,
    openAds: () -> Unit
) {
    val tabs = viewModel.tabs
    val pagerState = rememberPagerState(initialPage = 0) { tabs.size }
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current
    val isPlusMode = viewModel.purchaseStateUpdated
    var openAlertDialog by remember { mutableStateOf(false) }
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppBar(
                scrollBehavior = scrollBehavior,
                title = viewModel.fullCoin.coin.code,
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                },
                menuItems = buildList {
                    if (viewModel.isWatchlistEnabled) {
                        if (viewModel.isFavorite) {
                            add(
                                MenuItem(
                                    title = TranslatableString.ResString(R.string.CoinPage_Unfavorite),
                                    icon = R.drawable.ic_heart_filled_24,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    onClick = {
                                        viewModel.onUnfavoriteClick()

                                        stat(
                                            page = StatPage.CoinPage,
                                            event = StatEvent.RemoveFromWatchlist(viewModel.fullCoin.coin.uid)
                                        )
                                    }
                                )
                            )
                        } else {
                            add(
                                MenuItem(
                                    title = TranslatableString.ResString(R.string.CoinPage_Favorite),
                                    icon = R.drawable.ic_heart_24,
                                    tint = ComposeAppTheme.colors.grey,
                                    onClick = {
                                        if (isPlusMode) {
                                            viewModel.onFavoriteClick()
                                            stat(
                                                page = StatPage.CoinPage,
                                                event = StatEvent.AddToWatchlist(viewModel.fullCoin.coin.uid)
                                            )
                                        } else {
                                            openAlertDialog = true
                                        }
                                    }
                                )
                            )
                        }
                    }
                }
            )
        }
    ) { innerPaddings ->
        Column(
            modifier = Modifier
                .padding(innerPaddings)
        ) {
            val selectedTab = tabs[pagerState.currentPage]
            val tabItems = tabs.map {
                TabItem(stringResource(id = it.titleResId), it == selectedTab, it)
            }
            Tabs(tabItems, onClick = { tab ->
                coroutineScope.launch {
                    pagerState.scrollToPage(tab.ordinal)

                    stat(page = StatPage.CoinPage, event = StatEvent.SwitchTab(tab.statTab))
                }
            })

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false
            ) { page ->
                when (tabs[page]) {
                    CoinModule.Tab.Overview -> {
                        CoinOverviewScreen(
                            fullCoin = viewModel.fullCoin,
                            navController = navController
                        )
                    }

                    CoinModule.Tab.Market -> {
                        CoinMarketsScreen(fullCoin = viewModel.fullCoin)
                    }

                    CoinModule.Tab.Details -> {
                        CoinAnalyticsScreen(
                            fullCoin = viewModel.fullCoin,
                            navController = navController,
                            fragmentManager = fragmentManager
                        )
                    }
                }
            }

            viewModel.successMessage?.let {
                HudHelper.showSuccessMessage(view, it)

                viewModel.onSuccessMessageShown()
            }
        }
        if (openAlertDialog) {
            AlertDialog(
                title = {
                    body_jacob(text = stringResource(id = R.string.billing_plus_title))
                },
                text = {
                    Text(text = "To use the feature, you must subscribe to Wallet+ or watch ads in exchange for rewards.")
                },
                onDismissRequest = { openAlertDialog = false },
                confirmButton = {
                    TextButton(onClick = {
                        openAlertDialog = false
                        openAds.invoke()
                    }) {
                        Text(text = "View Ads")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        openAlertDialog = false
                        navController.slideFromBottom(R.id.buySubscriptionFragment)
                    }) {
                        Text(text = stringResource(id = R.string.billing_plus_title))
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinNotFound(coinUid: String, navController: NavController) {
    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppBar(
                title = coinUid,
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                }
            )
        },
        content = {
            ListEmptyView(
                paddingValues = it,
                text = stringResource(R.string.CoinPage_CoinNotFound, coinUid),
                icon = R.drawable.ic_not_available
            )
        }
    )
}
