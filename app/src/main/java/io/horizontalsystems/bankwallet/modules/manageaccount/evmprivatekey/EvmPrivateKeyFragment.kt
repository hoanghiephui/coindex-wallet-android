package io.horizontalsystems.bankwallet.modules.manageaccount.evmprivatekey

import android.os.Parcelable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material3.Scaffold
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.wallet.blockchain.bitcoin.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.stats.StatEntity
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.manageaccount.SecretKeyScreen
import kotlinx.parcelize.Parcelize

class EvmPrivateKeyFragment : BaseComposeFragment(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Input>(navController) { input ->
            input?.let {
                EvmPrivateKeyScreen(navController, input.evmPrivateKey)
            } ?: navController.popBackStack()
        }
    }

    override val logScreen: String
        get() = "EvmPrivateKeyFragment"

    @Parcelize
    data class Input(val evmPrivateKey: String) : Parcelable
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvmPrivateKeyScreen(
    navController: NavController,
    evmPrivateKey: String,
) {
    SecretKeyScreen(
        navController = navController,
        secretKey = evmPrivateKey,
        title = stringResource(R.string.EvmPrivateKey_Title),
        hideScreenText = stringResource(R.string.EvmPrivateKey_ShowPrivateKey),
        onCopyKey = {
            stat(
                page = StatPage.EvmPrivateKey,
                event = StatEvent.Copy(StatEntity.EvmPrivateKey)
            )
        },
        onOpenFaq = {
            stat(
                page = StatPage.EvmPrivateKey,
                event = StatEvent.Open(StatPage.Info)
            )
        },
        onToggleHidden = {
            stat(page = StatPage.EvmPrivateKey, event = StatEvent.ToggleHidden)
        }
    )
}
