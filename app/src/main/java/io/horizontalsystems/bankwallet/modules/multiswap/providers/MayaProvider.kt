package io.horizontalsystems.bankwallet.modules.multiswap.providers

import com.wallet.blockchain.bitcoin.R

object MayaProvider : BaseThorChainProvider(
    baseUrl = "https://mayanode.mayachain.info/mayachain/",
    affiliate = "hrz_android",
    affiliateBps = 100,
) {
    override val id = "mayachain"
    override val title = "Maya Protocol"
    override val icon = R.drawable.maya
    override val priority = 0
}
