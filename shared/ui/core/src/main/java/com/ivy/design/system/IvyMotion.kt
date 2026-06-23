package com.ivy.design.system

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * Central Material-3 motion spec for Ivy Wallet. One source of truth so every screen, list and
 * card animates with the same physics — spring-based "spatial" motion for movement/size and a
 * snappier "effects" spring for fades. Material 3 Expressive `MotionScheme` is internal in
 * material3 1.4.0, so we model the same intent with stable spring specs here.
 */
object IvyMotion {
    /** Shared-axis travel = ~1/[SLIDE_FRACTION] of the container width. */
    private const val SLIDE_FRACTION = 5
    private const val SPATIAL_DAMPING = 0.9f
    private const val SPATIAL_STIFFNESS = 380f
    private const val EFFECTS_STIFFNESS = 1200f

    /** Spring for spatial changes (position, size). */
    fun <T> spatialSpring(visibilityThreshold: T? = null): SpringSpec<T> =
        spring(
            dampingRatio = SPATIAL_DAMPING,
            stiffness = SPATIAL_STIFFNESS,
            visibilityThreshold = visibilityThreshold
        )

    /** Snappier spring for non-spatial effects (alpha). */
    fun <T> effectsSpring(): SpringSpec<T> = spring(stiffness = EFFECTS_STIFFNESS)

    /** Placement spec for `Modifier.animateItem`. */
    fun placementSpring(): SpringSpec<IntOffset> = spatialSpring(IntOffset.VisibilityThreshold)

    /** Spec for `Modifier.animateContentSize`. */
    fun contentSizeSpring(): SpringSpec<IntSize> = spatialSpring(IntSize.VisibilityThreshold)

    /** Shared-axis X enter: forward slides in from the right, back from the left (+ fade). */
    fun sharedAxisXEnter(forward: Boolean): EnterTransition =
        slideInHorizontally(animationSpec = placementSpring()) { full ->
            if (forward) full / SLIDE_FRACTION else -full / SLIDE_FRACTION
        } + fadeIn(animationSpec = effectsSpring())

    /** Shared-axis X exit: forward slides out to the left, back to the right (+ fade). */
    fun sharedAxisXExit(forward: Boolean): ExitTransition =
        slideOutHorizontally(animationSpec = placementSpring()) { full ->
            if (forward) -full / SLIDE_FRACTION else full / SLIDE_FRACTION
        } + fadeOut(animationSpec = effectsSpring())

    /** Expand/collapse for togglable sections (e.g. Upcoming/Overdue). */
    val sectionExpand: EnterTransition
        get() = expandVertically(animationSpec = contentSizeSpring()) +
            fadeIn(animationSpec = effectsSpring())

    val sectionCollapse: ExitTransition
        get() = shrinkVertically(animationSpec = contentSizeSpring()) +
            fadeOut(animationSpec = effectsSpring())
}
