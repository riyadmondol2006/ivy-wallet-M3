package com.ivy.home.customerjourney

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.design.system.IvySpacing
import com.ivy.domain.RootScreen
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.rootScreen
import com.ivy.navigation.IvyPreview
import com.ivy.navigation.navigation
import com.ivy.ui.R
import kotlinx.collections.immutable.ImmutableList

@Composable
fun CustomerJourney(
    customerJourneyCards: ImmutableList<CustomerJourneyCardModel>,
    modifier: Modifier = Modifier,
    onDismiss: (CustomerJourneyCardModel) -> Unit,
) {
    val ivyContext = ivyWalletCtx()
    val nav = navigation()
    // Check is added for Paparazzi Test where context is different
    if (LocalContext.current is RootScreen) {
        val rootScreen = rootScreen()

        for (card in customerJourneyCards) {
            Spacer(Modifier.height(16.dp))

            CustomerJourneyCard(
                modifier = modifier,
                cardData = card,
                onDismiss = {
                    onDismiss(card)
                }
            ) {
                card.onAction(nav, ivyContext, rootScreen)
            }
        }
    } else {
        Box(modifier)
    }
}

/** Soft tonal prompt card on `primaryContainer`; same corner radius and gutter as the other Home cards. */
@Composable
fun CustomerJourneyCard(
    cardData: CustomerJourneyCardModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onCTA: () -> Unit,
) {
    val onContainer = MaterialTheme.colorScheme.onPrimaryContainer
    Card(
        onClick = onCTA,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = IvySpacing.screenGutter),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = onContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 8.dp),
                    text = cardData.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = onContainer,
                )

                if (cardData.hasDismiss) {
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = onContainer,
                        )
                    }
                }
            }

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp),
                text = cardData.description,
                style = MaterialTheme.typography.bodyMedium,
                color = onContainer,
            )

            if (cardData.cta != null) {
                Spacer(Modifier.height(4.dp))

                Button(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(end = 8.dp)
                        .testTag("cta_prompt_${cardData.id}"),
                    onClick = onCTA,
                ) {
                    Icon(
                        painter = painterResource(cardData.ctaIcon),
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(text = cardData.cta)
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewCard() {
    IvyPreview {
        CustomerJourneyCard(
            cardData = CustomerJourneyCardsProvider.adjustBalanceCard(),
            onCTA = { },
            onDismiss = {}
        )
    }
}
