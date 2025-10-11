package io.horizontalsystems.bankwallet

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdRevenueListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.nativeAds.MaxNativeAdListener
import com.applovin.mediation.nativeAds.MaxNativeAdLoader
import com.applovin.mediation.nativeAds.MaxNativeAdView
import com.applovin.mediation.nativeAds.MaxNativeAdViewBinder
import com.wallet.blockchain.bitcoin.R
import io.horizontalsystems.bankwallet.core.AdType
import io.horizontalsystems.bankwallet.core.BaseViewModel.Companion.SHOW_ADS
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun rememberAdNativeView(
    adUnitId: String,
    adPlacements: String,
    revenueListener: MaxAdRevenueListener,
    adType: AdType = AdType.SMALL
): Pair<AdNativeUiState, () -> Unit> {
    if (!SHOW_ADS) {
        return Pair(AdNativeUiState.Nothing) {}
    }
    val context = LocalContext.current
    var loadedAd by remember {
        mutableStateOf<AdNativeUiState>(AdNativeUiState.Loading)
    }
    var nativeAd: MaxAd? = null
    var retryCount by remember { mutableIntStateOf(0) }
    val maxRetries = 3
    val scope = rememberCoroutineScope()
    val adView = remember(adType) {
        if (adType == AdType.MEDIUM) {
            createNativeAdMediumView(context)
        } else {
            createNativeAdSmallView(context)
        }
    }
    val nativeAdLoader = remember(context, adUnitId, adPlacements) {
        MaxNativeAdLoader(adUnitId).apply {
            placement = adPlacements
            setExtraParameter(
                "content_url",
                "https://play.google.com/store/apps/details?id=com.blockchain.btc.coinhub"
            )
            setRevenueListener(revenueListener)
            setNativeAdListener(object : MaxNativeAdListener() {
                override fun onNativeAdLoaded(loadedNativeAdView: MaxNativeAdView?, ad: MaxAd) {
                    if (nativeAd != null) {
                        this@apply.destroy(nativeAd)
                    }

                    if (loadedNativeAdView != null) {
                        nativeAd = ad
                        loadedAd = AdNativeUiState.Success(loadedNativeAdView)
                        Timber.d("Applovin SUCCESS: $loadedNativeAdView")
                    } else {
                        Timber.e("Applovin: NativeAdView is null")
                        if (retryCount < maxRetries) {
                            scope.launch {
                                delay(3000) // Wait for 3 seconds before retrying
                                retryCount++
                                Timber.e("Applovin retrying... Attempt $retryCount")
                                this@apply.loadAd(adView)
                            }
                        } else {
                            loadedAd = AdNativeUiState.LoadError
                        }
                    }
                }

                override fun onNativeAdLoadFailed(adUnitId: String, error: MaxError) {
                    Timber.e("Applovin ${error.message}")
                    if (retryCount < maxRetries) {
                        scope.launch {
                            delay(3000) // Wait for 3 seconds before retrying
                            retryCount++
                            Timber.e("Applovin retrying... Attempt $retryCount")
                            this@apply.loadAd(adView)
                        }
                    } else {
                        loadedAd = AdNativeUiState.LoadError
                    }
                }

                override fun onNativeAdClicked(ad: MaxAd) {}

                override fun onNativeAdExpired(nativeAd: MaxAd) {}
            })

            loadAd(adView)
            Timber.d("Loading Ad...")
        }
    }

    // Function to reload the ad externally
    val reloadAd: () -> Unit = {
        retryCount = 0
        loadedAd = AdNativeUiState.Loading
        nativeAdLoader.loadAd()  // Reset retry count and load ad again
    }

    DisposableEffect(nativeAdLoader) {
        onDispose {
            if (nativeAd != null) {
                nativeAdLoader.destroy(nativeAd)
                nativeAd = null
            }
            nativeAdLoader.destroy()
        }
    }

    // Return the ad state and the reload function
    return remember(revenueListener, loadedAd, adPlacements) {
        Pair(loadedAd, reloadAd)
    }
}

sealed interface AdNativeUiState {
    data object Nothing : AdNativeUiState
    data object Loading : AdNativeUiState
    data object LoadError : AdNativeUiState
    data class Success(
        val adsView: MaxNativeAdView?,
    ) : AdNativeUiState
}

private fun createNativeAdMediumView(context: Context): MaxNativeAdView {
    val binder: MaxNativeAdViewBinder =
        MaxNativeAdViewBinder.Builder(R.layout.native_custom_ad_view)
            .setTitleTextViewId(R.id.title_text_view)
            .setBodyTextViewId(R.id.body_text_view)
            .setAdvertiserTextViewId(R.id.advertiser_text_view)
            .setIconImageViewId(R.id.icon_image_view)
            .setMediaContentViewGroupId(R.id.media_view_container)
            .setOptionsContentViewGroupId(R.id.options_view)
            .setStarRatingContentViewGroupId(R.id.star_rating_view)
            .setCallToActionButtonId(R.id.cta_button)
            .build()
    return MaxNativeAdView(binder, context)
}

private fun createNativeAdSmallView(context: Context): MaxNativeAdView {
    val binder: MaxNativeAdViewBinder =
        MaxNativeAdViewBinder.Builder(R.layout.native_custom_ad_small_view)
            .setTitleTextViewId(R.id.title_text_view)
            .setBodyTextViewId(R.id.body_text_view)
            .setAdvertiserTextViewId(R.id.advertiser_text_view)
            .setIconImageViewId(R.id.icon_image_view)
            .setOptionsContentViewGroupId(R.id.options_view)
            .setStarRatingContentViewGroupId(R.id.star_rating_view)
            .setCallToActionButtonId(R.id.cta_button)
            .build()
    return MaxNativeAdView(binder, context)
}
