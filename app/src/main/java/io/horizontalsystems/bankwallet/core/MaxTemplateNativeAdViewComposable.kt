package io.horizontalsystems.bankwallet.core

import android.view.ViewGroup
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.wallet.blockchain.bitcoin.R
import io.horizontalsystems.bankwallet.AdNativeUiState
import io.horizontalsystems.bankwallet.core.BaseViewModel.Companion.SHOW_ADS
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.Loading
import io.horizontalsystems.bankwallet.modules.settings.banners.SubscriptionBanner
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

/**
 * Jetpack Compose function to display MAX native ads using the Templates API.
 */
@Composable
fun MaxTemplateNativeAdViewComposable(
    adViewState: AdNativeUiState,
    adType: AdType = AdType.MEDIUM,
    navController: NavController,
    useDefault: Boolean = true
) {
    if (!SHOW_ADS) return
    Crossfade(adViewState, label = "MaxTemplateNativeAdView") { viewState ->
        when (viewState) {
            is AdNativeUiState.LoadError -> {
                SubscriptionBanner(
                    onClick = {
                        navController.slideFromBottom(R.id.buySubscriptionFragment)
                    }
                )
            }

            is AdNativeUiState.Loading -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(adType.height)
                        .then(if (useDefault) Modifier.padding(horizontal = 16.dp) else Modifier),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Loading()
                }
            }

            is AdNativeUiState.Success -> {
                viewState.adsView?.let { view ->
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (useDefault) Modifier.padding(horizontal = 16.dp) else Modifier),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors()
                    ) {
                        AndroidView(
                            factory = {
                                view.also {
                                    if (it.parent != null) (it.parent as ViewGroup).removeView(it)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(adType.height)
                        )
                    }
                }
            }

            AdNativeUiState.Nothing -> Unit
        }
    }
}

val AdType.height get() = if (this == AdType.MEDIUM) 300.dp else 125.dp

enum class AdType {
    SMALL,
    MEDIUM
}

@Preview(showBackground = true)
@Composable
private fun MaxTemplateNativeAdViewReview() {
    ComposeAppTheme {
        MaxTemplateNativeAdViewComposable(
            adViewState = AdNativeUiState.LoadError,
            adType = AdType.SMALL,
            navController = rememberNavController()
        )
    }
}
