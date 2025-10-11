package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CardsSwapInfo(content: @Composable() (ColumnScope.() -> Unit)) {
    OutlinedCard(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            //.border(0.5.dp, ComposeAppTheme.colors.blade, RoundedCornerShape(16.dp))
            .padding(vertical = 2.dp),
        content = content,
        colors = CardDefaults.outlinedCardColors(
            contentColor = Color.Transparent,
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(16.dp)
    )
}
