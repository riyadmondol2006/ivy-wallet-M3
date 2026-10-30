package com.ivy.legacy.legacy.ui.theme.components

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ivy.ui.R

/**
 * Screen-reader label for an icon-only button, derived from its icon when the caller gave none.
 * Returns null for icons without an obvious action, rather than a meaningless "icon".
 */
@Composable
internal fun iconActionLabel(@DrawableRes icon: Int): String? {
    val label = when (icon) {
        R.drawable.ic_dismiss, R.drawable.ic_dismiss_close -> R.string.close
        R.drawable.ic_back, R.drawable.ic_back_android -> R.string.back
        R.drawable.ic_delete, R.drawable.ic_remove, R.drawable.ic_backspace -> R.string.delete
        R.drawable.ic_edit -> R.string.edit
        R.drawable.ic_sort_by_alpha_24 -> R.string.sort_by
        R.drawable.ic_reorder -> R.string.reorder
        R.drawable.ic_filter_xs, R.drawable.ic_filter_l -> R.string.filter
        R.drawable.ic_plus, R.drawable.ic_add -> R.string.add
        R.drawable.ic_search -> R.string.search
        R.drawable.ic_settings -> R.string.settings
        else -> null
    }
    return label?.let { stringResource(it) }
}
