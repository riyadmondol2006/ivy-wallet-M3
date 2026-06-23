package com.ivy.wallet.ui.theme.modal.edit

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.utils.hideKeyboard
import com.ivy.legacy.utils.onScreenStart
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.Ivy
import com.ivy.wallet.ui.theme.Orange
import com.ivy.wallet.ui.theme.Red
import com.ivy.wallet.ui.theme.components.ItemIconS
import com.ivy.wallet.ui.theme.modal.IvyModal
import com.ivy.wallet.ui.theme.modal.ModalSkip
import com.ivy.wallet.ui.theme.modal.ModalTitle
import com.ivy.wallet.ui.theme.toComposeColor
import java.util.UUID

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Suppress("ParameterNaming")
@ExperimentalFoundationApi
@Composable
fun BoxWithConstraintsScope.ChooseCategoryModal(
    id: UUID = UUID.randomUUID(),
    visible: Boolean,
    initialCategory: Category?,
    categories: List<Category>,

    showCategoryModal: (Category?) -> Unit,
    onCategoryChanged: (Category?) -> Unit,
    dismiss: () -> Unit
) {
    var selectedCategory by remember(initialCategory) {
        mutableStateOf(initialCategory)
    }

    IvyModal(
        id = id,
        visible = visible,
        dismiss = dismiss,
        PrimaryAction = {
            ModalSkip {
                save(
                    category = selectedCategory,
                    onCategoryChanged = onCategoryChanged,
                    dismiss = dismiss
                )
            }
        }
    ) {
        val view = LocalView.current
        onScreenStart {
            hideKeyboard(view)
        }

        Spacer(Modifier.height(32.dp))

        ModalTitle(
            text = stringResource(R.string.choose_category)
        )

        Spacer(Modifier.height(24.dp))

        CategoryPicker(
            categories = categories,
            selectedCategory = selectedCategory,
            showCategoryModal = showCategoryModal,
            onEditCategory = {
                showCategoryModal(it)
            }
        ) {
            selectedCategory = it
            save(
                shouldDismissModal = it != null,
                category = it,
                onCategoryChanged = onCategoryChanged,
                dismiss = dismiss
            )
        }

        Spacer(Modifier.height(56.dp))
    }
}

private fun save(
    shouldDismissModal: Boolean = true,

    category: Category?,
    onCategoryChanged: (Category?) -> Unit,
    dismiss: () -> Unit
) {
    onCategoryChanged(category)
    if (shouldDismissModal) {
        dismiss()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@ExperimentalFoundationApi
@Suppress("ParameterNaming")
@Composable
private fun CategoryPicker(
    categories: List<Category>,
    selectedCategory: Category?,
    showCategoryModal: (Category?) -> Unit,
    onEditCategory: (Category) -> Unit,
    onSelected: (Category?) -> Unit,
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        categories.forEach { category ->
            CategoryButton(
                category = category,
                selected = category == selectedCategory,
                onClick = {
                    onSelected(category)
                },
                onLongClick = {
                    onEditCategory(category)
                },
                onDeselect = {
                    onSelected(null)
                }
            )
        }

        AddNewButton {
            showCategoryModal(null)
        }
    }
}

@ExperimentalFoundationApi
@Composable
private fun CategoryButton(
    category: Category,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDeselect: () -> Unit,
) {
    val categoryColor = category.color.value.toComposeColor()

    FilterChip(
        modifier = Modifier
            .combinedClickable(
                onClick = {
                    if (selected) {
                        onDeselect()
                    } else {
                        onClick()
                    }
                },
                onLongClick = onLongClick
            )
            .testTag("choose_category_button"),
        selected = selected,
        onClick = {
            // Tap handling is driven by the outer combinedClickable so that
            // long-press (edit) is supported. This is intentionally a no-op.
        },
        label = {
            Text(text = category.name.value)
        },
        leadingIcon = {
            ItemIconS(
                modifier = Modifier.size(FilterChipDefaults.IconSize),
                iconName = category.icon?.id,
                tint = categoryColor,
                Default = {
                    Icon(
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                        painter = painterResource(id = R.drawable.ic_custom_category_s),
                        contentDescription = category.name.value,
                        tint = categoryColor
                    )
                }
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = categoryColor.copy(alpha = 0.2f)
        )
    )
}

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun AddNewButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    AssistChip(
        modifier = modifier,
        onClick = onClick,
        label = {
            Text(text = stringResource(R.string.add_new))
        },
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_plus),
                contentDescription = stringResource(R.string.add_new)
            )
        }
    )
}

@ExperimentalFoundationApi
@Preview
@Composable
private fun PreviewChooseCategoryModal() {
    IvyWalletPreview {
        val categories = mutableListOf(
            Category(
                name = NotBlankTrimmedString.unsafe("Test"),
                color = ColorInt(Ivy.toArgb()),
                icon = null,
                id = CategoryId(UUID.randomUUID()),
                orderNum = 0.0,
            ),
            Category(
                name = NotBlankTrimmedString.unsafe("Second"),
                color = ColorInt(Orange.toArgb()),
                icon = null,
                id = CategoryId(UUID.randomUUID()),
                orderNum = 0.0,
            ),
            Category(
                name = NotBlankTrimmedString.unsafe("Third"),
                color = ColorInt(Red.toArgb()),
                icon = null,
                id = CategoryId(UUID.randomUUID()),
                orderNum = 0.0,
            ),
        )

        ChooseCategoryModal(
            visible = true,
            initialCategory = categories.first(),
            categories = categories,
            showCategoryModal = { },
            onCategoryChanged = { }
        ) {
        }
    }
}
