package io.horizontalsystems.bankwallet.modules.intro

import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.crashlytics
import com.wallet.blockchain.bitcoin.BuildConfig
import com.wallet.blockchain.bitcoin.R
import io.horizontalsystems.bankwallet.core.ILocalStorage

class IntroViewModel(
        private val localStorage: ILocalStorage
): ViewModel() {

    val slides = listOf(
        IntroModule.IntroSliderData(
            R.string.Intro_Wallet_Screen2Title,
            R.string.Intro_Wallet_Screen5Description,
            R.drawable.ic_independence_light,
            R.drawable.ic_independence
        ),
        IntroModule.IntroSliderData(
            R.string.Intro_Wallet_Screen3Title,
            R.string.Intro_Wallet_Screen3Description,
            R.drawable.ic_knowledge_light,
            R.drawable.ic_knowledge
        ),
        IntroModule.IntroSliderData(
            R.string.Intro_Wallet_Screen4Title,
            R.string.Intro_Wallet_Screen4Description,
            R.drawable.ic_privacy_light,
            R.drawable.ic_privacy
        ),
    )

    fun onStartClicked() {
        localStorage.mainShowedOnce = true
    }

    fun onSaveConfig(checkAnalytic: Boolean,
                     checkDetectCrash: Boolean) {
        localStorage.isAnalytic = checkAnalytic
        localStorage.isDetectCrash = checkDetectCrash

        Firebase.analytics.setAnalyticsCollectionEnabled(if (!BuildConfig.DEBUG) checkAnalytic else false)
        Firebase.crashlytics.isCrashlyticsCollectionEnabled = if (!BuildConfig.DEBUG) checkDetectCrash else false
    }

}
