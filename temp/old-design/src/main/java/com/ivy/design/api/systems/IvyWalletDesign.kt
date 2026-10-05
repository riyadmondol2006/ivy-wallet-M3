package com.ivy.design.api.systems

import com.ivy.design.api.IvyDesign

/**
 * Legacy design-system root. It used to own a Raleway/Open Sans type scale, a fixed palette and
 * its own corner radii; all of that now comes from the Material 3 theme through the bridge in
 * `IvyTheme`, so only the context hook remains.
 */
@Deprecated("Old design system. Use `:ivy-design` and Material3")
abstract class IvyWalletDesign : IvyDesign
