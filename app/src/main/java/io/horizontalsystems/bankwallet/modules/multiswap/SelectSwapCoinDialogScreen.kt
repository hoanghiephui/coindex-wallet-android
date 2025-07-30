package io.horizontalsystems.bankwallet.modules.multiswap

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wallet.blockchain.bitcoin.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.alternativeImageUrl
import io.horizontalsystems.bankwallet.core.badge
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.ui.compose.components.B2
import io.horizontalsystems.bankwallet.ui.compose.components.Badge
import io.horizontalsystems.bankwallet.ui.compose.components.D1
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsImage
import io.horizontalsystems.bankwallet.ui.compose.components.MultitextM1
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.SearchBar
import io.horizontalsystems.bankwallet.ui.compose.components.SectionUniversalItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer

@Composable
fun SelectSwapCoinDialogScreen(
    title: String,
    coinBalanceItems: List<CoinBalanceItem>,
    onSearchTextChanged: (String) -> Unit,
    onClose: () -> Unit,
    onClickItem: (CoinBalanceItem) -> Unit
) {
    SearchBar(
        title = title,
        onSearchTextChanged = onSearchTextChanged,
        hint = stringResource(R.string.ManageCoins_Search),
        navigationAction = onClose,
        content = {
            LazyColumn {
                items(coinBalanceItems) { coinItem ->
                    SectionUniversalItem(borderTop = true) {
                        RowUniversal(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            onClick = {
                                onClickItem.invoke(coinItem)
                            }
                        ) {
                            HsImage(
                                url = coinItem.token.coin.imageUrl,
                            alternativeUrl = coinItem.token.coin.alternativeImageUrl,
                                placeholder = coinItem.token.iconPlaceholder,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.size(16.dp))
                            MultitextM1(
                                modifier = Modifier.weight(1f),title = {
                                    Row {
                                        B2(text = coinItem.token.coin.code)
                                        coinItem.token.badge?.let {
                                            Badge(text = it)
                                        }
                                    }
                                },
                                subtitle = { D1(text = coinItem.token.coin.name) }
                            )
                            HSpacer(8.dp)
                            MultitextM1(
                                title = {
                                    coinItem.balance?.let {
                                        App.numberFormatter.formatCoinShort(
                                            it,
                                            coinItem.token.coin.code,
                                            8
                                        )
                                    }?.let {
                                        B2(text = it)
                                    }
                                },
                                subtitle = {
                                    coinItem.fiatBalanceValue?.let { fiatBalanceValue ->
                                        App.numberFormatter.formatFiatShort(
                                            fiatBalanceValue.value,
                                            fiatBalanceValue.currency.symbol,
                                        2
                                        )
                                    }?.let {
                                        D1(
                                            modifier = Modifier.align(Alignment.End),
                                            text = it
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
                item {
                    VSpacer(height = 32.dp)
                }
            }
        }
    )
}
