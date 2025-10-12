package io.horizontalsystems.bankwallet.modules.manageaccount.backupkey

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.wallet.blockchain.bitcoin.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.managers.FaqManager
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.manageaccount.ui.PassphraseCell
import io.horizontalsystems.bankwallet.modules.manageaccount.ui.SeedPhraseList
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem

class BackupKeyFragment : BaseComposeFragment(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Account>(navController) { account ->
            account?.let {
                RecoveryPhraseScreen(navController, account)
            } ?: navController.popBackStack()
        }
    }

    override var logScreen: String
        get() = "BackupKeyFragment"
        set(value) {}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoveryPhraseScreen(
    navController: NavController,
    account: Account
) {
    val viewModel = viewModel<BackupKeyViewModel>(factory = BackupKeyModule.Factory(account))

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppBar(
                title = stringResource(R.string.RecoveryPhrase_Title),
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Info_Title),
                        icon = R.drawable.ic_info_24,
                        onClick = {
                            FaqManager.showFaqPage(navController, FaqManager.faqPathPrivateKeys)
                        }
                    ),
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = {
                            navController.popBackStack()
                        }
                    )
                )
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(top = it.calculateTopPadding())) {
            var hidden by remember { mutableStateOf(true) }

            InfoText(text = stringResource(R.string.RecoveryPhrase_Description))
            Spacer(Modifier.height(12.dp))
            SeedPhraseList(
                wordsNumbered = viewModel.wordsNumbered,
                hidden = hidden
            ) {
                hidden = !hidden
            }
            Spacer(Modifier.height(24.dp))
            PassphraseCell(viewModel.passphrase, hidden)
            Spacer(modifier = Modifier.weight(1f))
            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp),
                    title = stringResource(R.string.RecoveryPhrase_Verify),
                    onClick = {
                        navController.slideFromRight(
                            R.id.backupConfirmationKeyFragment,
                            viewModel.account
                        )
                    },
                )
            }
        }
    }
}
