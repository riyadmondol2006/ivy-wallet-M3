package com.ivy.disclaimer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ivy.disclaimer.composables.AcceptTermsText
import com.ivy.disclaimer.composables.AgreeButton
import com.ivy.disclaimer.composables.AgreementCheckBox
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.ui.component.OpenSourceCard

@Composable
fun DisclaimerScreenImpl() {
    val viewModel: DisclaimerViewModel = screenScopedViewModel()
    val viewState = viewModel.uiState()
    DisclaimerScreenUi(viewState = viewState, onEvent = viewModel::onEvent)
}

@Composable
fun DisclaimerScreenUi(
    viewState: DisclaimerViewState,
    onEvent: (DisclaimerViewEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        bottomBar = {
            BottomBar(
                enabled = viewState.agreeButtonEnabled,
                onAgree = { onEvent(DisclaimerViewEvent.OnAgreeClick) },
            )
        },
        content = { innerPadding ->
            Content(
                contentPadding = innerPadding,
                viewState = viewState,
                onEvent = onEvent,
            )
        }
    )
}

@Composable
private fun Content(
    contentPadding: PaddingValues,
    viewState: DisclaimerViewState,
    onEvent: (DisclaimerViewEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Header()
        }
        item {
            Spacer(modifier = Modifier.height(24.dp))
            OpenSourceCard()
        }
        item {
            Spacer(modifier = Modifier.height(24.dp))
            AcceptTermsText()
            Spacer(modifier = Modifier.height(8.dp))
        }
        itemsIndexed(items = viewState.checkboxes) { index, item ->
            Spacer(modifier = Modifier.height(12.dp))
            AgreementCheckBox(
                viewState = item,
                onClick = {
                    onEvent(DisclaimerViewEvent.OnCheckboxClick(index))
                }
            )
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Header(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    modifier = Modifier.size(36.dp),
                    imageVector = Icons.Rounded.Gavel,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.important_user_agreement),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun BottomBar(
    enabled: Boolean,
    onAgree: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            AgreeButton(
                enabled = enabled,
                onClick = onAgree,
            )
        }
    }
}
