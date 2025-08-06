package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wallet.blockchain.bitcoin.R
import io.horizontalsystems.bankwallet.ui.compose.ColoredTextStyle
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    content: @Composable (() -> Unit),
    navigationAction: () -> Unit,
    menuItems: List<MenuItem> = listOf(),
    hint: String,
    onSearchTextChanged: (String) -> Unit = {},
    title: String
) {
    var text by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var searchMode by remember { mutableStateOf(false) }
    var searchClear by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val menuItem = mutableListOf(
        MenuItem(
            title = TranslatableString.ResString(R.string.Button_Search),
            icon = R.drawable.icon_search,
            onClick = {
                searchMode = true
                expanded = true
                keyboardController?.show()
            },
            tint = MaterialTheme.colorScheme.onSurface
        )
    ).also {
        it.addAll(menuItems)
    }
    val color = if (expanded) {
        SearchBarDefaults.colors(
            containerColor = ComposeAppTheme.colors.tyler
        )
    } else {
        SearchBarDefaults.colors()
    }
    Box(
        Modifier
            .fillMaxSize()
    ) {
        if (!searchMode) {
            Column {
                AppBar(
                    title = title,
                    navigationIcon = {
                        HsBackButton(onClick = navigationAction)
                    },
                    menuItems = menuItem
                )
                content.invoke()
            }
        } else {
            androidx.compose.material3.SearchBar(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .semantics { traversalIndex = 0f },
                inputField = {
                    SearchBarDefaults.InputField(
                        query = text,
                        onQueryChange = {
                            text = it
                            onSearchTextChanged(it)
                            searchClear = it.isNotEmpty()
                        },
                        onSearch = {
                            expanded = false
                            searchMode = false
                            keyboardController?.hide()
                        },
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        placeholder = { Text(hint) },
                        leadingIcon = {
                            IconButton(onClick = {
                                expanded = false
                                searchMode = false
                                keyboardController?.hide()
                                onSearchTextChanged("")
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                            }
                        },
                        trailingIcon = {
                            if (searchClear) {
                                IconButton(onClick = {
                                    searchClear = false
                                    text = ""
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = null)
                                }
                            }
                        },
                    )
                },
                expanded = expanded,
                onExpandedChange = { expanded = it },
                colors = color
            ) {
                content.invoke()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalAnimationApi
@Composable
fun SearchBar(
    title: String,
    searchHintText: String = "",
    searchOnlyMode: Boolean = false,
    searchModeInitial: Boolean = false,
    focusRequester: FocusRequester = remember { FocusRequester() },
    menuItems: List<MenuItem> = listOf(),
    onClose: () -> Unit,
    onSearchTextChanged: (String) -> Unit = {},
) {

    var searchMode by remember { mutableStateOf(searchModeInitial) }
    var showClearButton by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    var searchText by remember { mutableStateOf("") }
    val backgroundColor: TopAppBarColors = TopAppBarDefaults.centerAlignedTopAppBarColors()

    TopAppBar(
        modifier = Modifier, title = {
            title3_leah(
                text = if (searchMode) "" else title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        colors = backgroundColor,
        navigationIcon = {
            HsIconButton(onClick = {
                if (searchMode && !searchOnlyMode) {
                    searchText = ""
                    onSearchTextChanged.invoke("")
                    searchMode = false
                } else {
                    onClose.invoke()
                }
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_left_24),
                    contentDescription = stringResource(R.string.Button_Back),
                )
            }
        },
        actions = {
            if (searchMode) {
                OutlinedTextField(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 2.dp)
                        .focusRequester(focusRequester),
                    value = searchText,
                    onValueChange = {
                        searchText = it
                        onSearchTextChanged.invoke(it)
                        showClearButton = it.isNotEmpty()
                    },
                    placeholder = {
                        body_grey50(
                            text = searchHintText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    textStyle = ComposeAppTheme.typography.body,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = ComposeAppTheme.colors.jacob,
                    ),
                    maxLines = 1,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        keyboardController?.hide()
                    }),
                    trailingIcon = {
                        AnimatedVisibility(
                            visible = showClearButton,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            HsIconButton(onClick = {
                                searchText = ""
                                onSearchTextChanged.invoke("")
                                showClearButton = false
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_close),
                                    contentDescription = stringResource(R.string.Button_Cancel),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                        }
                    },
                )

                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            }

            if (!searchMode) {
                AppBarMenuButton(
                    icon = R.drawable.ic_search,
                    onClick = { searchMode = true },
                    description = stringResource(R.string.Button_Search),
                )

                menuItems.forEach { menuItem ->
                    if (menuItem.icon != null) {
                        AppBarMenuButton(
                            icon = menuItem.icon,
                            onClick = menuItem.onClick,
                            description = menuItem.title.getString(),
                            enabled = menuItem.enabled,
                            tint = menuItem.tint
                        )
                    } else {
                        Text(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .clickable(
                                    enabled = menuItem.enabled,
                                    onClick = menuItem.onClick
                                ),
                            text = menuItem.title.getString(),
                            style = ComposeAppTheme.typography.headline2,
                            color = if (menuItem.enabled) ComposeAppTheme.colors.jacob else ComposeAppTheme.colors.yellow50
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun SearchCell(
    modifier: Modifier = Modifier,
    onSearchQueryChange: (String) -> Unit = {},
    placeholder: String = stringResource(R.string.Balance_ReceiveHint_Search),
    onSearchClick: () -> Unit = {}
) {
    var searchText by remember { mutableStateOf("") }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(
                color = ComposeAppTheme.colors.blade,
                shape = RoundedCornerShape(24.dp)
            )
            .clickable { onSearchClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = "Search",
            tint = ComposeAppTheme.colors.grey,
        )

        HSpacer(16.dp)

        BasicTextField(
            value = searchText,
            onValueChange = {
                searchText = it
                onSearchQueryChange.invoke(it)
            },
            modifier = Modifier.weight(1f),
            textStyle = ColoredTextStyle(
                color = ComposeAppTheme.colors.leah,
                textStyle = ComposeAppTheme.typography.body
            ),
            singleLine = true,
            decorationBox = { innerTextField ->
                if (searchText.isEmpty()) {
                    body_andy(
                        text = placeholder,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                innerTextField()
            }
        )
    }
}
