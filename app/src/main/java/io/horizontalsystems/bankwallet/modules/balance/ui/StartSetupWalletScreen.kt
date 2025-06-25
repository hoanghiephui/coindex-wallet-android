package io.horizontalsystems.bankwallet.modules.balance.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.wallet.blockchain.bitcoin.R
import io.horizontalsystems.bankwallet.core.navigateWithTermsAccepted
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.RadialBackground
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_jacob
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_lucian
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_remus
import io.horizontalsystems.bankwallet.ui.compose.components.title3_leah

@Composable
fun StartSetupWalletScreen(
    navController: NavController
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        RadialBackground()
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Image(
                    modifier = Modifier
                        .padding(top = 50.dp)
                        .align(Alignment.CenterStart),
                    painter = painterResource(id = R.drawable.img_start),
                    contentDescription = null
                )
            }
            title3_leah(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp), text = "Wallet Setup"
            )

            subhead2_leah(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 60.dp),
                text = "Import an existing wallet \n" + "or create a new one",
            )

            ElevatedButton(
                onClick = {
                    navController.navigateWithTermsAccepted {
                        navController.slideFromRight(R.id.importWalletFragment)

                        stat(page = StatPage.Balance, event = StatEvent.Open(StatPage.ImportWallet))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    contentColor = MaterialTheme.colorScheme.background
                )
            ) {
                Text(
                    stringResource(R.string.ManageAccounts_ImportWallet),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            ElevatedButton(
                onClick = {
                    navController.navigateWithTermsAccepted {
                        navController.slideFromRight(R.id.createAccountFragment)

                        stat(page = StatPage.Balance, event = StatEvent.Open(StatPage.NewWallet))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ComposeAppTheme.colors.leah,
                    contentColor = MaterialTheme.colorScheme.background
                )
            ) {
                Text(
                    stringResource(R.string.ManageAccounts_CreateNewWallet),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
fun StartSetupWalletScreenPreview() {
    ComposeAppTheme {
        StartSetupWalletScreen(navController = rememberNavController())
    }
}