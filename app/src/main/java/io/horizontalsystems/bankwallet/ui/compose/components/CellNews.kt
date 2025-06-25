package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun CellNews(
    source: String,
    title: String,
    body: String,
    date: String,
    onClick: () -> Unit
) {
    var titleLines by remember { mutableIntStateOf(0) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        onClick = { onClick.invoke() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            captionSB_grey(
                text = source,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(8.dp))
            headline2_leah(
                text = title,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { res -> titleLines = res.lineCount }
            )
            if (titleLines < 3) {
                Spacer(modifier = Modifier.height(8.dp))
                subhead2_grey(
                    text = body,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = if (titleLines == 1) 2 else 1,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = date,
                color = ComposeAppTheme.colors.andy,
                style = ComposeAppTheme.typography.micro,
                maxLines = 1,
            )
        }
    }
}
