package io.horizontalsystems.bankwallet.modules.settings.subscription

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.wallet.blockchain.bitcoin.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.billing.PLAY_STORE_SUBSCRIPTION_URL
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.StatPremiumTrigger
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_jacob
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.cell.CellUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionUniversalLawrence

class SubscriptionFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        SubscriptionScreen(navController)
    }

    override val logScreen: String
        get() = "SubscriptionFragment"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(navController: NavController) {
    val viewModel = viewModel<SubscriptionViewModel>()
    val uiState = viewModel.uiState
    val context = LocalContext.current

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppBar(
                title = stringResource(R.string.Settings_Subscription),
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                }
            )
        }
    ) {
        Column(
            modifier = Modifier.padding(it)
        ) {
            VSpacer(12.dp)

            SectionUniversalLawrence {
                if (uiState.userHasActiveSubscription) {
                    CellUniversal(
                        borderTop = false,
                        onClick = {
                            viewModel.launchManageSubscriptionScreen(context)
                        }
                    ) {
                        body_leah(
                            text = stringResource(R.string.SettingsSubscription_ManageSubscription),
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Image(
                            modifier = Modifier.size(20.dp),
                            painter = painterResource(id = R.drawable.ic_arrow_right),
                            contentDescription = null,
                        )
                    }
                    CellUniversal(
                        onClick = {
                            navController.slideFromBottom(R.id.buySubscriptionFragment)
                        }
                    ) {
                        body_leah(
                            text = stringResource(R.string.SettingsSubscription_GetPremium),
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Image(
                            modifier = Modifier.size(20.dp),
                            painter = painterResource(id = R.drawable.ic_arrow_right),
                            contentDescription = null,
                        )
                    }
                } else {
                    CellUniversal(
                        borderTop = false,
                        onClick = {
                            navController.slideFromBottom(R.id.buySubscriptionFragment)
                            stat(
                                page = StatPage.PurchaseList,
                                event = StatEvent.OpenPremium(StatPremiumTrigger.GetPremium)
                            )
                        }
                    ) {
                        body_leah(
                            text = stringResource(R.string.SettingsSubscription_GetPremium),
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Image(
                            modifier = Modifier.size(20.dp),
                            painter = painterResource(id = R.drawable.ic_arrow_right),
                            contentDescription = null,
                        )
                    }
                    CellUniversal(
                        onClick = viewModel::restorePurchase
                    ) {
                        body_jacob(
                            text = stringResource(R.string.SettingsSubscription_RestorePurchase),
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Image(
                            modifier = Modifier.size(20.dp),
                            painter = painterResource(id = R.drawable.ic_arrow_right),
                            contentDescription = null,
                        )
                    }
                }
            }
        }
    }
}
