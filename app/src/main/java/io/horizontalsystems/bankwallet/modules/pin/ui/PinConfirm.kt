package io.horizontalsystems.bankwallet.modules.pin.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wallet.blockchain.bitcoin.R
import io.horizontalsystems.bankwallet.modules.pin.unlock.PinConfirmViewModel
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinConfirm(
    onSuccess: () -> Unit,
    onCancel: () -> Unit,
) {
    val viewModel = viewModel<PinConfirmViewModel>(factory = PinConfirmViewModel.Factory())

    if (viewModel.uiState.unlocked) {
        onSuccess.invoke()
        viewModel.unlocked()
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            AppBar(
                title = stringResource(R.string.Unlock_Title),
                navigationIcon = {
                    HsBackButton(onClick = onCancel)
                },
            )
        }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
        ) {
            PinTopBlock(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.Unlock_EnterPasscode),
                enteredCount = viewModel.uiState.enteredCount,
                showShakeAnimation = viewModel.uiState.showShakeAnimation,
                inputState = viewModel.uiState.inputState,
                onShakeAnimationFinish = { viewModel.onShakeAnimationFinish() }
            )

            PinNumpad(
                pinRandomized = viewModel.pinRandomized,
                onNumberClick = { number -> viewModel.onKeyClick(number) },
                onDeleteClick = { viewModel.onDelete() },
                inputState = viewModel.uiState.inputState,
                updatePinRandomized = { random -> viewModel.updatePinRandomized(random) }
            )
        }
    }
}
