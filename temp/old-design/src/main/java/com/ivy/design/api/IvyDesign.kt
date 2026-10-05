package com.ivy.design.api

import com.ivy.design.IvyContext

/**
 * Legacy design entry point. Colours, typography and shapes are no longer defined here: the
 * deprecated `UI.*` tokens are derived from the active Material 3 theme in `IvyTheme`.
 */
@Deprecated("Old design system. Use `:ivy-design` and Material3")
interface IvyDesign {
    @Deprecated("Old design system. Use `:ivy-design` and Material3")
    fun context(): IvyContext
}
