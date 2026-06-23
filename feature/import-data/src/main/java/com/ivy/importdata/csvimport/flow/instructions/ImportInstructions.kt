package com.ivy.importdata.csvimport.flow.instructions

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.importdata.csvimport.flow.ImportSteps
import com.ivy.legacy.domain.deprecated.logic.csv.model.ImportType
import com.ivy.navigation.navigation
import com.ivy.onboarding.components.OnboardingToolbar
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.components.IvyIcon

@ExperimentalFoundationApi
@Composable
fun BoxWithConstraintsScope.ImportInstructions(
    hasSkip: Boolean,
    importType: ImportType,

    onSkip: () -> Unit,
    onUploadClick: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        stickyHeader {
            val nav = navigation()
            OnboardingToolbar(
                hasSkip = hasSkip,
                onBack = { nav.onBackPressed() },
                onSkip = onSkip
            )
            // onboarding toolbar include paddingBottom 16.dp
        }

        item {
            Spacer(Modifier.height(8.dp))

            Text(
                modifier = Modifier.padding(start = 32.dp),
                text = stringResource(R.string.how_to_import),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(8.dp))

            Text(
                modifier = Modifier.padding(start = 32.dp),
                text = stringResource(R.string.open),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = Bold
            )

            Spacer(Modifier.height(24.dp))

            App(
                importType = importType
            )

            Spacer(Modifier.height(24.dp))

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }

        item {
            Spacer(Modifier.height(24.dp))

            Text(
                modifier = Modifier.padding(start = 32.dp),
                text = stringResource(R.string.steps),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            importType.ImportSteps(onUploadClick = onUploadClick)
        }

        item {
            // last spacer
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun VideoArticleRow(
    videoUrl: String?,
    articleUrl: String?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        val rootScreen = com.ivy.legacy.rootScreen()

        Spacer(Modifier.width(16.dp))

        if (videoUrl != null) {
            VideoButton(
                modifier = Modifier.weight(1f)
            ) {
                rootScreen.openUrlInBrowser(videoUrl)
            }
        }

        if (videoUrl != null && articleUrl != null) {
            Spacer(Modifier.width(8.dp))
        }

        if (articleUrl != null) {
            ArticleButton(
                modifier = Modifier.weight(1f)
            ) {
                rootScreen.openUrlInBrowser(articleUrl)
            }
        }

        Spacer(Modifier.width(16.dp))
    }
}

@Composable
fun VideoButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    InstructionButton(
        modifier = modifier,
        icon = R.drawable.ic_import_video,
        caption = stringResource(R.string.how_to),
        text = stringResource(R.string.video)
    ) {
        onClick()
    }
}

@Composable
fun ArticleButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    InstructionButton(
        modifier = modifier,
        icon = R.drawable.ic_import_web,
        caption = stringResource(R.string.how_to),
        text = stringResource(R.string.article)
    ) {
        onClick()
    }
}

@Composable
fun InstructionButton(
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int?,
    caption: String,
    text: String,

    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(16.dp))

            if (icon != null) {
                IvyIcon(
                    modifier = Modifier.background(
                        MaterialTheme.colorScheme.surface,
                        CircleShape
                    ),
                    icon = icon,
                    tint = Color.Unspecified
                )
            }

            Spacer(Modifier.width(if (icon != null) 24.dp else 12.dp))

            Column {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = Bold
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = Bold
                )
            }
        }
    }
}

@Composable
fun UploadFileStep(
    stepNumber: Int,
    text: String = stringResource(R.string.upload_csv_file),
    btnTitle: String = text,
    onUploadClick: () -> Unit
) {
    StepTitle(
        number = stepNumber,
        title = text
    )

    Spacer(Modifier.height(16.dp))

    Button(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        onClick = onUploadClick
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_upload),
            contentDescription = null
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = btnTitle,
            fontWeight = Bold
        )
    }
}

@Suppress("MultipleEmitters")
@Composable
fun StepTitle(
    number: Int,
    title: String,
    description: String? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(24.dp))

        Text(
            modifier = Modifier
                .size(24.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            text = number.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp, end = 32.dp),
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }

    if (description != null) {
        Spacer(Modifier.height(4.dp))

        Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = description,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun App(
    importType: ImportType
) {
    val rootScreen = com.ivy.legacy.rootScreen()

    Row(
        modifier = Modifier
            .padding(horizontal = 32.dp)
            .clickable {
                rootScreen.openGooglePlayAppPage(
                    appId = importType.appId()
                )
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        IvyIcon(
            modifier = Modifier.size(48.dp),
            icon = importType.logo(),
            tint = Color.Unspecified
        )

        Spacer(Modifier.width(16.dp))

        Text(
            text = importType.appName(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@ExperimentalFoundationApi
@Preview
@Composable
private fun Preview() {
    com.ivy.legacy.IvyWalletPreview {
        ImportInstructions(
            hasSkip = true,
            importType = ImportType.MONEY_MANAGER,
            onSkip = {}
        ) {
        }
    }
}
