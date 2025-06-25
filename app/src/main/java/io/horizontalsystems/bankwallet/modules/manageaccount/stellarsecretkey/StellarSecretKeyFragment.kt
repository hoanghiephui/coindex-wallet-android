package io.horizontalsystems.bankwallet.modules.manageaccount.stellarsecretkey

import android.os.Parcelable
import androidx.compose.runtime.Composable
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

class StellarSecretKeyFragment : BaseComposeFragment(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Input>(navController) { input ->
            input?.let {
                StellarSecretKeyScreen(navController, input.stellarSecretKey)
            } ?: run {
                navController.popBackStack()
            }
        }
    }

    override val logScreen: String
        get() = "StellarSecretKeyFragment"

    @Parcelize
    data class Input(val stellarSecretKey: String) : Parcelable
}

@Composable
fun StellarSecretKeyScreen(
    navController: NavController,
    stellarSecretKey: String,
) {
    SecretKeyScreen(
        navController = navController,
        secretKey = stellarSecretKey,
        title = stringResource(R.string.StellarSecretKey_Title),
        hideScreenText = stringResource(R.string.StellarSecretKey_ShowSecretKey),
        onCopyKey = {
            stat(
                page = StatPage.StellarSecretKey,
                event = StatEvent.Copy(StatEntity.StellarSecretKey)
            )
        },
        onOpenFaq = {
            stat(
                page = StatPage.StellarSecretKey,
                event = StatEvent.Open(StatPage.Info)
            )
        },
        onToggleHidden = {
            stat(page = StatPage.StellarSecretKey, event = StatEvent.ToggleHidden)
        }
    )
}
