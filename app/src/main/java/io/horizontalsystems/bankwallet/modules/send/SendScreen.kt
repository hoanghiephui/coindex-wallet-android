package io.horizontalsystems.bankwallet.modules.send

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendScreen(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    HSScaffold(
        title = title,
        onBack = onBack,
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            content.invoke(this)
        }
    }
}
