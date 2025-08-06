package io.horizontalsystems.bankwallet.modules.main

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.wallet.blockchain.bitcoin.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.managers.RateAppManager
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.stats.StatEntity
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.core.stats.statTab
import io.horizontalsystems.bankwallet.modules.balance.ui.BalanceScreen
import io.horizontalsystems.bankwallet.modules.main.MainModule.MainNavigation
import io.horizontalsystems.bankwallet.modules.manageaccount.dialogs.BackupRequiredDialog
import io.horizontalsystems.bankwallet.modules.market.MarketScreen
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchModule
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchViewModel
import io.horizontalsystems.bankwallet.modules.rateapp.RateApp
import io.horizontalsystems.bankwallet.modules.releasenotes.ReleaseNotesFragment
import io.horizontalsystems.bankwallet.modules.rooteddevice.RootedDeviceModule
import io.horizontalsystems.bankwallet.modules.rooteddevice.RootedDeviceScreen
import io.horizontalsystems.bankwallet.modules.rooteddevice.RootedDeviceViewModel
import io.horizontalsystems.bankwallet.modules.search.SearchScreen
import io.horizontalsystems.bankwallet.modules.sendtokenselect.SendTokenSelectFragment
import io.horizontalsystems.bankwallet.modules.settings.main.SettingsScreen
import io.horizontalsystems.bankwallet.modules.tor.TorStatusView
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsModule
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsScreen
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.WCAccountTypeNotSupportedDialog
import io.horizontalsystems.bankwallet.modules.walletconnect.WCManager.SupportState
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.NiaNavigationBar
import io.horizontalsystems.bankwallet.ui.compose.NiaNavigationBarItem
import io.horizontalsystems.bankwallet.ui.compose.components.BadgeText
import io.horizontalsystems.bankwallet.ui.extensions.WalletSwitchBottomSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainFragment : BaseComposeFragment() {
    private val mainActivityViewModel by activityViewModels<MainActivityViewModel>()

    @Composable
    override fun GetContent(navController: NavController) {
        val backStackEntry = navController.safeGetBackStackEntry(R.id.mainFragment)

        backStackEntry?.let {
            val viewModel =
                ViewModelProvider(backStackEntry.viewModelStore, TransactionsModule.Factory())[TransactionsViewModel::class.java]
            MainScreenWithRootedDeviceCheck(
                transactionsViewModel = viewModel,
                navController = navController,
                mainActivityViewModel = mainActivityViewModel
            )
        } ?: run {
            // Back stack entry doesn't exist, restart activity
            val intent = Intent(context, MainActivity::class.java)
            requireActivity().startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    requireActivity().moveTaskToBack(true)
                }
            })
    }

    override val logScreen: String
        get() = "MainFragment"
}

@Composable
private fun MainScreenWithRootedDeviceCheck(
    transactionsViewModel: TransactionsViewModel,
    navController: NavController,
    rootedDeviceViewModel: RootedDeviceViewModel = viewModel(factory = RootedDeviceModule.Factory()),
    mainActivityViewModel: MainActivityViewModel
) {
    if (rootedDeviceViewModel.showRootedDeviceWarning) {
        RootedDeviceScreen { rootedDeviceViewModel.ignoreRootedDeviceWarning() }
    } else {
        MainScreen(mainActivityViewModel, transactionsViewModel, navController)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    mainActivityViewModel: MainActivityViewModel,
    transactionsViewModel: TransactionsViewModel,
    fragmentNavController: NavController,
    viewModel: MainViewModel = viewModel(factory = MainModule.Factory()),
    searchViewModel: MarketSearchViewModel = viewModel(factory = MarketSearchModule.Factory())
) {
    val activityIntent by mainActivityViewModel.intentLiveData.observeAsState()
    LaunchedEffect(activityIntent) {
        activityIntent?.data?.let {
            mainActivityViewModel.intentHandled()
            viewModel.handleDeepLink(it)
        }
    }

    val uiState = viewModel.uiState
    val selectedPage = uiState.selectedTabIndex
    val pagerState = rememberPagerState(initialPage = selectedPage) { uiState.mainNavItems.size }

    val coroutineScope = rememberCoroutineScope()

    val modalBottomSheetState = androidx.compose.material3.rememberModalBottomSheetState()
    var isBottomSheetVisible by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Column {
                if (uiState.torEnabled) {
                    TorStatusView()
                }
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    NiaBottomBar(
                        destinations = uiState.mainNavItems,
                        onNavigateToDestination = {
                            viewModel.onSelect(it.mainNavItem)
                            stat(
                                page = StatPage.Main,
                                event = StatEvent.SwitchTab(it.mainNavItem.statTab)

                            )
                        },
                        bottomBarVisibility = true,
                        modifier = Modifier
                    )
                }
            }
        }
    ) {
        BackHandler(enabled = modalBottomSheetState.isVisible) {
            coroutineScope.launch {
                modalBottomSheetState.hide()
                isBottomSheetVisible = false
            }
        }
        Column(modifier = Modifier.padding(it)) {
            LaunchedEffect(key1 = selectedPage, block = {
                pagerState.scrollToPage(selectedPage)
            })

            HorizontalPager(
                modifier = Modifier.weight(1f),
                state = pagerState,
                userScrollEnabled = false,
                verticalAlignment = Alignment.Top
            ) { page ->
                when (uiState.mainNavItems[page].mainNavItem) {
                    MainNavigation.Market -> MarketScreen(fragmentNavController)
                    MainNavigation.Balance -> BalanceScreen(fragmentNavController)
                    MainNavigation.Transactions -> TransactionsScreen(
                        fragmentNavController,
                        transactionsViewModel
                    )

                    MainNavigation.Settings -> SettingsScreen(fragmentNavController)
                    MainNavigation.Search -> {
                        SearchScreen(
                            searchViewModel,
                            fragmentNavController
                        )
                    }
                }
            }
        }
    }
    if (isBottomSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = {
                isBottomSheetVisible = false
            },
            sheetState = modalBottomSheetState,
            containerColor = ComposeAppTheme.colors.transparent
        ) {
            WalletSwitchBottomSheet(
                wallets = viewModel.wallets,
                watchingAddresses = viewModel.watchWallets,
                selectedAccount = uiState.activeWallet,
                onSelectListener = {
                    coroutineScope.launch {
                        modalBottomSheetState.hide()
                        isBottomSheetVisible = false
                        viewModel.onSelect(it)

                        stat(
                            page = StatPage.SwitchWallet,
                            event = StatEvent.Select(StatEntity.Wallet)
                        )
                    }
                },
                onCancelClick = {
                    coroutineScope.launch {
                        modalBottomSheetState.hide()
                        isBottomSheetVisible = false
                    }
                }
            )
        }
    }

    if (uiState.showWhatsNew) {
        LaunchedEffect(Unit) {
            fragmentNavController.slideFromBottom(
                R.id.releaseNotesFragment,
                ReleaseNotesFragment.Input(true)
            )
            viewModel.whatsNewShown()
        }
    }

    if (uiState.showDonationPage) {
        LaunchedEffect(Unit) {
            fragmentNavController.slideFromBottom(R.id.whyDonateFragment)
            viewModel.donationShown()
        }
    }

    if (uiState.showRateAppDialog) {
        val context = LocalContext.current
        /*RateApp(
            onRateClick = {
                RateAppManager.openPlayMarket(context)
                viewModel.closeRateDialog()
            },
            onCancelClick = { viewModel.closeRateDialog() }
        )*/
    }

    if (uiState.wcSupportState != null) {
        when (val wcSupportState = uiState.wcSupportState) {
            SupportState.NotSupportedDueToNoActiveAccount -> {
                fragmentNavController.slideFromBottom(R.id.wcErrorNoAccountFragment)
            }

            is SupportState.NotSupportedDueToNonBackedUpAccount -> {
                val text = stringResource(R.string.WalletConnect_Error_NeedBackup)
                fragmentNavController.slideFromBottom(
                    R.id.backupRequiredDialog,
                    BackupRequiredDialog.Input(wcSupportState.account, text)
                )

                stat(page = StatPage.Main, event = StatEvent.Open(StatPage.BackupRequired))
            }

            is SupportState.NotSupported -> {
                fragmentNavController.slideFromBottom(
                    R.id.wcAccountTypeNotSupportedDialog,
                    WCAccountTypeNotSupportedDialog.Input(wcSupportState.accountTypeDescription)
                )
            }

            else -> {}
        }
        viewModel.wcSupportStateHandled()
    }

    uiState.deeplinkPage?.let { deepLinkPage ->
        LaunchedEffect(Unit) {
            delay(500)
            fragmentNavController.slideFromRight(
                deepLinkPage.navigationId,
                deepLinkPage.input
            )
            viewModel.deeplinkPageHandled()
        }
    }

    uiState.openSend?.let { openSend ->
        fragmentNavController.slideFromRight(
            R.id.sendTokenSelectFragment,
            SendTokenSelectFragment.Input(
                openSend.blockchainTypes,
                openSend.tokenTypes,
                openSend.address,
                openSend.amount
            )
        )
        viewModel.onSendOpened()
    }

    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        viewModel.onResume()
    }
}

@Composable
private fun BadgedIcon(
    badge: MainModule.BadgeType?,
    icon: @Composable BoxScope.() -> Unit,
) {
    when (badge) {
        is MainModule.BadgeType.BadgeNumber ->
            BadgedBox(
                badge = {
                    BadgeText(
                        text = badge.number.toString(),
                    )
                },
                content = icon
            )

        MainModule.BadgeType.BadgeDot ->
            BadgedBox(
                badge = {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                ComposeAppTheme.colors.lucian,
                                shape = RoundedCornerShape(4.dp)
                            )
                    ) { }
                },
                content = icon
            )

        else -> {
            Box {
                icon()
            }
        }
    }
}

fun NavController.safeGetBackStackEntry(destinationId: Int): NavBackStackEntry? {
    return try {
        this.getBackStackEntry(destinationId)
    } catch (_: IllegalArgumentException) {
        null
    }
}

@Composable
private fun NiaBottomBar(
    destinations: List<MainModule.NavigationViewItem>,
    onNavigateToDestination: (MainModule.NavigationViewItem) -> Unit,
    modifier: Modifier = Modifier,
    bottomBarVisibility: Boolean,
) {
    NiaNavigationBar(
        modifier = modifier,
        visibility = bottomBarVisibility
    ) {
        destinations.forEach { destination ->
            val selected = destination.selected
            NiaNavigationBarItem(
                selected = selected,
                onClick = { onNavigateToDestination(destination) },
                icon = {
                    Icon(
                        painter = painterResource(destination.mainNavItem.iconRes),
                        contentDescription = stringResource(destination.mainNavItem.titleRes)
                    )
                },
                selectedIcon = {
                    Icon(
                        painter = painterResource(destination.mainNavItem.iconRes),
                        contentDescription = stringResource(destination.mainNavItem.titleRes)
                    )
                },
                label = {
                    Text(
                        stringResource(destination.mainNavItem.titleRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                modifier = Modifier,
                enabled = destination.enabled
            )
        }
    }
}

@Composable
@OptIn(ExperimentalPermissionsApi::class)
private fun NotificationPermissionEffect() {
    // Permission requests should only be made from an Activity Context, which is not present
    // in previews
    if (LocalInspectionMode.current) return
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val notificationsPermissionState = rememberPermissionState(
        Manifest.permission.POST_NOTIFICATIONS,
    )
    LaunchedEffect(notificationsPermissionState) {
        val status = notificationsPermissionState.status
        if (status is PermissionStatus.Denied && !status.shouldShowRationale) {
            notificationsPermissionState.launchPermissionRequest()
        }
    }
}