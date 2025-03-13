package io.horizontalsystems.bankwallet.modules.usersubscription

import com.wallet.blockchain.bitcoin.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.subscriptions.core.AddressBlacklist
import io.horizontalsystems.subscriptions.core.AddressPhishing
import io.horizontalsystems.subscriptions.core.AdvancedSearch
import io.horizontalsystems.subscriptions.core.BasePlan
import io.horizontalsystems.subscriptions.core.DuressMode
import io.horizontalsystems.subscriptions.core.IPaidAction
import io.horizontalsystems.subscriptions.core.MultiWallet
import io.horizontalsystems.subscriptions.core.NoAds
import io.horizontalsystems.subscriptions.core.PricingPhase
import io.horizontalsystems.subscriptions.core.TokenInsights
import io.horizontalsystems.subscriptions.core.TradeSignals
import io.horizontalsystems.subscriptions.core.VIPSupport
import java.time.Period
import io.horizontalsystems.subscriptions.core.Watchlist

object BuySubscriptionModel {

    val IPaidAction.titleStringRes: Int
        get() = when (this) {
            TokenInsights -> R.string.Premium_UpgradeFeature_TokenInsights
            AdvancedSearch -> R.string.Premium_UpgradeFeature_AdvancedSearch
            TradeSignals -> R.string.Premium_UpgradeFeature_TradeSignals
            DuressMode -> R.string.Premium_UpgradeFeature_DuressMode
            AddressPhishing -> R.string.Premium_UpgradeFeature_AddressPhishing
            AddressBlacklist -> R.string.Premium_UpgradeFeature_AddressBlacklist
            VIPSupport -> R.string.Premium_UpgradeFeature_VipSupport
            Watchlist -> R.string.Market_Tab_Watchlist
            MultiWallet -> R.string.Premium_UpgradeFeature_MultiWallet
            NoAds -> R.string.Premium_UpgradeFeature_NoAds
            else -> throw IllegalArgumentException("Unknown IPaidAction")
        }

    val IPaidAction.descriptionStringRes: Int
        get() = when (this) {
            TokenInsights -> R.string.Premium_UpgradeFeature_TokenInsights_Description
            AdvancedSearch -> R.string.Premium_UpgradeFeature_AdvancedSearch_Description
            TradeSignals -> R.string.Premium_UpgradeFeature_TradeSignals_Description
            DuressMode -> R.string.Premium_UpgradeFeature_DuressMode_Description
            AddressPhishing -> R.string.Premium_UpgradeFeature_AddressPhishing_Description
            AddressBlacklist -> R.string.Premium_UpgradeFeature_AddressBlacklist_Description
            VIPSupport -> R.string.Premium_UpgradeFeature_VipSupport_Description
            Watchlist -> R.string.Hud_Added_To_Watchlist
            MultiWallet -> R.string.Premium_UpgradeFeature_MultiWallet_Description
            NoAds -> R.string.Premium_UpgradeFeature_NoAds_Description
            else -> throw IllegalArgumentException("Unknown IPaidAction")
        }

    val IPaidAction.bigDescriptionStringRes: Int
        get() = when (this) {
            TokenInsights -> R.string.Premium_UpgradeFeature_TokenInsights_BigDescription
            AdvancedSearch -> R.string.Premium_UpgradeFeature_AdvancedSearch_BigDescription
            TradeSignals -> R.string.Premium_UpgradeFeature_TradeSignals_BigDescription
            DuressMode -> R.string.Premium_UpgradeFeature_DuressMode_BigDescription
            AddressPhishing -> R.string.Premium_UpgradeFeature_AddressPhishing_BigDescription
            AddressBlacklist -> R.string.Premium_UpgradeFeature_AddressBlacklist_BigDescription
            VIPSupport -> R.string.Premium_UpgradeFeature_VipSupport_BigDescription
            Watchlist -> R.string.Hud_Added_To_Watchlist
            MultiWallet -> R.string.Premium_UpgradeFeature_MultiWallet_BigDescription
            NoAds -> R.string.Premium_UpgradeFeature_NoAds_BigDescription
            else -> throw IllegalArgumentException("Unknown IPaidAction")
        }

    val IPaidAction.iconRes: Int
        get() = when (this) {
            TokenInsights -> R.drawable.prem_portfolio_24
            AdvancedSearch -> R.drawable.prem_search_discovery_24
            TradeSignals -> R.drawable.prem_ring_24
            DuressMode -> R.drawable.prem_duress_24
            AddressPhishing -> R.drawable.prem_shield_24
            AddressBlacklist -> R.drawable.prem_warning_24
            VIPSupport -> R.drawable.prem_vip_support_24
            Watchlist -> R.drawable.star_filled_yellow_16
            MultiWallet -> R.drawable.ic_in_wallet_dark_24
            NoAds -> R.drawable.baseline_ads_click_24
            else -> throw IllegalArgumentException("Unknown IPaidAction")
        }

    fun BasePlan.title(): String {
        return pricingPhases.last().period.title()
    }

    fun Period.title(): String {
        return when {
            years > 0 -> Translator.getString(R.string.Premium_SubscriptionPeriod_Annually)
            months > 0 -> Translator.getString(R.string.Premium_SubscriptionPeriod_Monthly)
            else -> ""
        }
    }

    fun BasePlan.stringRepresentation(): String {
        val phase = pricingPhases.last()
        return "${phase.formattedPrice} / ${phase.period()}"
    }

    fun BasePlan.badge(): String? {
        return when (pricingPhases.last().period.years) {
            1 -> Translator.getString(R.string.Premium_SubscriptionPeriod_AnnuallySave)
            else -> null
        }
    }

    //billing periods: P1M, P3M, P6M, P1Y
    private fun PricingPhase.period(): String {
        return when (billingPeriod) {
            "P1M" -> Translator.getString(R.string.Premium_SubscriptionPeriod_Month)
            "P1Y" -> Translator.getString(R.string.Premium_SubscriptionPeriod_Year)
            else -> billingPeriod
        }
    }
}
