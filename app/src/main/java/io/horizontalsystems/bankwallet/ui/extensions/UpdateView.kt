package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.play.core.ktx.AppUpdateResult
import com.wallet.blockchain.bitcoin.R
import io.horizontalsystems.bankwallet.analytics.TrackScreenViewEvent
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import se.warting.inappupdate.compose.APP_UPDATE_REQUEST_CODE
import se.warting.inappupdate.compose.InAppUpdateState
import se.warting.inappupdate.compose.findActivity

@Composable
fun HeaderUpdate(
    updateState: InAppUpdateState,
    context: Context,
    onShow: () -> Unit,
    onHide: () -> Unit,
    rememberCoroutineScope: CoroutineScope
) {
    BottomSheetHeader(
        modifier = Modifier.navigationBarsPadding(),
        iconPainter = painterResource(R.drawable.ic_baseline_update_24),
        iconTint = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
        title = "Software update",
        onCloseClick = {
            onHide()
        }
    ) {
        when (val result = updateState.appUpdateResult) {
            is AppUpdateResult.Available -> {
                Text(
                    text = stringResource(id = R.string.update_content),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 24.dp)
                )
                ButtonPrimaryDefault(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 24.dp, end = 24.dp, bottom = 10.dp
                        ),
                    title = stringResource(R.string.update_now),
                    onClick = {
                        rememberCoroutineScope.launch {
                            result.startFlexibleUpdate(
                                context.findActivity(), APP_UPDATE_REQUEST_CODE
                            )
                        }
                        onHide()
                    }
                )

                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.safeDrawing))
                LaunchedEffect(key1 = Unit, block = {
                    onShow()
                })
            }

            is AppUpdateResult.InProgress -> {
                val updateProgress: Long =
                    if (result.installState.totalBytesToDownload() == 0L) {
                        0L
                    } else {
                        (result.installState.bytesDownloaded() * 100L /
                                result.installState.totalBytesToDownload())
                    }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    DownloadLottie()
                }
                Text(
                    text = stringResource(id = R.string.downloading),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 24.dp)
                )
                val process = updateProgress.toFloat() / 100f
                LinearProgressIndicator(
                    progress = { process },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                )
                Text(
                    text = "${updateProgress}%",
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                LaunchedEffect(key1 = Unit, block = {
                    onShow()
                })
            }

            is AppUpdateResult.Downloaded -> {
                Spacer(modifier = Modifier.height(30.dp))
                Text(
                    text = stringResource(id = R.string.update_done),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 24.dp)
                )
                ButtonPrimaryDefault(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    title = stringResource(R.string.install_now),
                    onClick = {
                        rememberCoroutineScope.launch {
                            onHide()
                            result.completeUpdate()
                        }
                    }
                )
                LaunchedEffect(key1 = Unit, block = {
                    onShow()
                })
            }

            else -> {
                LaunchedEffect(key1 = Unit, block = {
                    onHide()
                })
            }
        }
        TrackScreenViewEvent("HeaderUpdate")
    }
}
