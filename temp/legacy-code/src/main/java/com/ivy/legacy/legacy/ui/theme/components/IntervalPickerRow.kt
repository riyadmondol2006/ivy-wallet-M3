package com.ivy.wallet.ui.theme.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.data.model.IntervalType
import com.ivy.legacy.IvyWalletComponentPreview
import com.ivy.legacy.forDisplay
import com.ivy.legacy.utils.capitalizeLocal
import com.ivy.legacy.utils.selectEndTextFieldValue

private const val RepeatIntervalCharLimit = 5
private val RowInset = 24.dp
private val FieldGap = 12.dp
private val NumberFieldWidth = 96.dp
private val ControlHeight = 56.dp

/**
 * "Every N days/weeks/months/years" picker: a compact number field next to a unit selector of the
 * same height. Used by the period picker ("or in the last") and the recurring-rule sheet.
 */
@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun IntervalPickerRow(
    intervalN: Int,
    intervalType: IntervalType,

    onSetIntervalN: (Int) -> Unit,
    onSetIntervalType: (IntervalType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = RowInset),
        verticalAlignment = Alignment.CenterVertically
    ) {
        var intervalText by remember(intervalN) {
            mutableStateOf(selectEndTextFieldValue(intervalN.toString()))
        }
        val focusManager = LocalFocusManager.current

        OutlinedTextField(
            modifier = Modifier
                .width(NumberFieldWidth)
                .testTag("base_number_input"),
            value = intervalText,
            onValueChange = {
                val filtered = it.text.filter(Char::isDigit).take(RepeatIntervalCharLimit)
                if (filtered != intervalText.text) {
                    filtered.toIntOrNull()?.let(onSetIntervalN)
                }
                intervalText = it.copy(text = filtered)
            },
            placeholder = {
                Text(
                    text = "0",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
            shape = MaterialTheme.shapes.large,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        )

        Spacer(Modifier.width(FieldGap))

        IntervalTypeSelector(
            intervalN = intervalN,
            intervalType = intervalType,
            onSetIntervalType = onSetIntervalType,
        )
    }
}

@Composable
private fun RowScope.IntervalTypeSelector(
    intervalN: Int,
    intervalType: IntervalType,

    onSetIntervalType: (IntervalType) -> Unit
) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .height(ControlHeight),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onSetIntervalType(intervalType.previous()) },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = "interval_type_arrow_left",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                modifier = Modifier.weight(1f),
                text = intervalType.forDisplay(intervalN).capitalizeLocal(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )

            IconButton(
                onClick = { onSetIntervalType(intervalType.next()) },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "interval_type_arrow_right",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun IntervalType.next(): IntervalType = when (this) {
    IntervalType.DAY -> IntervalType.WEEK
    IntervalType.WEEK -> IntervalType.MONTH
    IntervalType.MONTH -> IntervalType.YEAR
    IntervalType.YEAR -> IntervalType.DAY
}

private fun IntervalType.previous(): IntervalType = when (this) {
    IntervalType.DAY -> IntervalType.YEAR
    IntervalType.WEEK -> IntervalType.DAY
    IntervalType.MONTH -> IntervalType.WEEK
    IntervalType.YEAR -> IntervalType.MONTH
}

@Preview
@Composable
private fun Preview() {
    IvyWalletComponentPreview {
        IntervalPickerRow(
            intervalN = 1,
            intervalType = IntervalType.WEEK,
            onSetIntervalN = {}
        ) {
        }
    }
}
