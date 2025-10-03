package io.horizontalsystems.bankwallet.modules.receive.monero

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.core.BaseComposeFragment

class MoneroSubaddressesFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        withInput<SubaddressesParams>(navController) {
            it?.let {
                MoneroSubaddressesScreen(it) { navController.popBackStack() }
            } ?: run {
                navController.popBackStack()
            }
        }
    }

    override val logScreen: String
        get() = "MoneroSubaddressesFragment"
}
