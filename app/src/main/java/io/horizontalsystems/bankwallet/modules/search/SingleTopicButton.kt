package io.horizontalsystems.bankwallet.modules.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.modules.market.topsectors.TopSectorViewItem
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage

@Composable
fun SingleTopicButton(
    viewItem: TopSectorViewItem,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .width(312.dp)
,        shape = RoundedCornerShape(corner = CornerSize(8.dp)),
        color = MaterialTheme.colorScheme.surface,
        onClick = {
            onClick()
        },
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .width(76.dp)
                ) {
                    val iconModifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(ComposeAppTheme.colors.tyler)

                    CoinImage(
                        coin = viewItem.coin3.coin,
                        modifier = iconModifier.align(Alignment.TopEnd)
                    )
                    CoinImage(
                        coin = viewItem.coin2.coin,
                        modifier = iconModifier.align(Alignment.TopCenter)
                    )
                    CoinImage(
                        coin = viewItem.coin1.coin,
                        modifier = iconModifier.align(Alignment.TopStart)
                    )
                }
                Text(
                    text = viewItem.coinCategory.name,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Text(
                text = viewItem.coinCategory.description["en"] ?: "",
                style = MaterialTheme.typography.bodySmall,
                overflow = TextOverflow.Ellipsis,
                maxLines = 3,
                modifier = Modifier
                    .padding(top = 8.dp),
            )
        }

    }
}