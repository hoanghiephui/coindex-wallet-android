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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.wallet.blockchain.bitcoin.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.core.stats.statTab
import io.horizontalsystems.bankwallet.modules.balance.ui.BalanceScreen
import io.horizontalsystems.bankwallet.modules.billing.showBillingPlusDialog
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Loading
import io.horizontalsystems.bankwallet.modules.keystore.NoSystemLockWarning
import io.horizontalsystems.bankwallet.modules.main.MainModule.MainNavigation
import io.horizontalsystems.bankwallet.modules.manageaccount.dialogs.BackupRequiredDialog
import io.horizontalsystems.bankwallet.modules.market.MarketScreen
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchModule
import io.horizontalsystems.bankwallet.modules.market.search.MarketSearchViewModel
import io.horizontalsystems.bankwallet.modules.rooteddevice.RootedDeviceModule
import io.horizontalsystems.bankwallet.modules.rooteddevice.RootedDeviceScreen
import io.horizontalsystems.bankwallet.modules.rooteddevice.RootedDeviceViewModel
import io.horizontalsystems.bankwallet.modules.search.SearchScreen
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
import io.horizontalsystems.bankwallet.ui.extensions.HeaderUpdate
import io.horizontalsystems.bankwallet.ui.extensions.rememberLifecycleEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import se.warting.inappupdate.compose.findActivity
import se.warting.inappupdate.compose.rememberInAppUpdateState
import se.warting.inappupdate.compose.review.rememberInAppReviewManager

class MainFragment : BaseComposeFragment() {
    private val searchViewModel by viewModels<MarketSearchViewModel> { MarketSearchModule.Factory() }
    private val mainActivityViewModel by activityViewModels<MainActivityViewModel>()

    @Composable
    override fun GetContent(navController: NavController) {
        val backStackEntry = navController.safeGetBackStackEntry(R.id.mainFragment)

        backStackEntry?.let {
            val viewModel = ViewModelProvider(
                backStackEntry.viewModelStore,
                TransactionsModule.Factory()
            )[TransactionsViewModel::class.java]
            MainScreenWithRootedDeviceCheck(
                transactionsViewModel = viewModel,
                navController = navController,
                searchViewModel = searchViewModel,
                mainActivity = (activity as MainActivity),
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
    searchViewModel: MarketSearchViewModel,
    mainActivity: MainActivity,
    mainActivityViewModel: MainActivityViewModel
) {
    if (rootedDeviceViewModel.showRootedDeviceWarning) {
        RootedDeviceScreen { rootedDeviceViewModel.ignoreRootedDeviceWarning() }
    } else {
        MainScreen(
            mainActivityViewModel, transactionsViewModel,
            navController,
            searchViewModel = searchViewModel,
            mainActivity = mainActivity
        )
    }
}

@Composable
private fun MainScreen(
    mainActivityViewModel: MainActivityViewModel,
    transactionsViewModel: TransactionsViewModel,
    fragmentNavController: NavController,
    viewModel: MainViewModel = viewModel(factory = MainModule.Factory()),
    searchViewModel: MarketSearchViewModel,
    mainActivity: MainActivity
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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val modalBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val lifecycleEvent = rememberLifecycleEvent()
    val updateState = rememberInAppUpdateState()
    val inAppReviewManager = rememberInAppReviewManager()
    val openPro by viewModel.openPro.collectAsStateWithLifecycle()
    val bottomBarHeight = remember { mutableFloatStateOf(0f) }
    val bottomBarOffsetHeightPx = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(key1 = openPro, block = {
        if (openPro) {
            mainActivity.showBillingPlusDialog()
        }
    })

    ModalBottomSheetLayout(
        sheetState = modalBottomSheetState,
        sheetBackgroundColor = ComposeAppTheme.colors.transparent,
        sheetContent = {
            HeaderUpdate(
                updateState, context, modalBottomSheetState, coroutineScope
            )
        },
    ) {
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
                                .onGloballyPositioned { coordinates ->
                                    bottomBarHeight.floatValue = coordinates.size.height.toFloat()
                                },
                        )
                    }
                }
            }
        ) { padding ->
            BackHandler(enabled = modalBottomSheetState.isVisible) {
                coroutineScope.launch {
                    modalBottomSheetState.hide()
                }
            }
            Column(
                modifier = Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Horizontal,
                        ),
                    )
            ) {
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
                        MainNavigation.Market -> MarketScreen(
                            fragmentNavController,
                        )

                        MainNavigation.Balance -> {
                            if (lifecycleEvent == Lifecycle.Event.ON_RESUME) {
                                mainActivity.validate(
                                    onUseAppNotWallet = {
                                        viewModel.openPageBalance(false)
                                    },
                                    onUseAppWallet = {
                                        viewModel.openPageBalance(true)
                                    },
                                    isWithBalance = true
                                )
                            }
                            if (uiState.isLoadBalance) {
                                Loading()
                            } else {
                                if (uiState.isShowBalance) {
                                    BalanceScreen(
                                        fragmentNavController,
                                        mainViewModel = viewModel,
                                    )
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize(),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        NoSystemLockWarning()
                                    }
                                }
                            }
                        }

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
    }

    LaunchedEffect(key1 = uiState.showRateAppDialog, block = {
        if (uiState.showRateAppDialog) {
            inAppReviewManager.launchReviewFlow(
                activity = context.findActivity(),
                onReviewRequestSuccess = {
                    viewModel.closeRateDialog()
                },
                onReviewRequestFail = {
                    viewModel.closeRateDialog()
                }
            )
        }
    })



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

            else -> Unit
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
    NotificationPermissionEffect()

    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        viewModel.onResume()
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

fun NavController.safeGetBackStackEntry(destinationId: Int): NavBackStackEntry? {
    return try {
        this.getBackStackEntry(destinationId)
    } catch (e: IllegalArgumentException) {
        null
    }
}
