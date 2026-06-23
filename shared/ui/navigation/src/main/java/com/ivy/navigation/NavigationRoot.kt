package com.ivy.navigation

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import com.ivy.design.system.IvyMotion
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

@SuppressLint("ComposeCompositionLocalUsage")
private val LocalNavigation = compositionLocalOf<Navigation> { error("No LocalNavigation") }

/**
 * Shared-element key for the "add transaction" container transform: the add-transaction FAB and
 * the `EditTransactionScreen` root use this key so the FAB morphs into the screen.
 */
const val AddTransactionSharedKey = "add-transaction-container"

/**
 * The app-level [SharedTransitionScope]. Exposed so screens in other modules (e.g. the add
 * transaction FAB and `EditTransactionScreen`) can opt into shared-element / container-transform
 * animations across navigation. Null when no [SharedTransitionLayout] is in scope.
 */
@SuppressLint("ComposeCompositionLocalUsage")
@Suppress("CompositionLocalAllowlist")
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/**
 * The [AnimatedVisibilityScope] of the current navigation [AnimatedContent]. Needed alongside
 * [LocalSharedTransitionScope] to drive `Modifier.sharedBounds`/`sharedElement`.
 */
@SuppressLint("ComposeCompositionLocalUsage")
@Suppress("CompositionLocalAllowlist")
val LocalNavAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NavigationRoot(
    navigation: Navigation,
    navGraph: @Composable (screen: Screen?) -> Unit
) {
    CompositionLocalProvider(
        LocalNavigation provides navigation,
    ) {
        val viewModelStore = LocalViewModelStoreOwner.current
        DisposableEffect(navigation.currentScreen) {
            onDispose {
                // Destroy viewModels only for non-legacy screens
                if (navigation.lastScreen?.isLegacy == false) {
                    viewModelStore?.viewModelStore?.clear()
                }
            }
        }

        // Wrap the screen switch in a SharedTransitionLayout + AnimatedContent so navigation
        // becomes physics-based and shared elements (FAB -> EditTransactionScreen) can morph.
        SharedTransitionLayout {
            CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                AnimatedContent(
                    targetState = navigation.currentScreen,
                    transitionSpec = {
                        // Material-3 shared-axis X: forward slides in from the right, back from the
                        // left (direction comes from whether this change was a back-navigation).
                        val forward = !navigation.isBack
                        IvyMotion.sharedAxisXEnter(forward) togetherWith
                            IvyMotion.sharedAxisXExit(forward)
                    },
                    // Animate only on genuine screen changes, not on in-screen state updates.
                    contentKey = { it?.let { screen -> screen::class.qualifiedName } ?: "null" },
                    label = "ivy-screen-transition",
                ) { screen ->
                    CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                        navGraph(screen)
                    }
                }
            }
        }
    }
}

@Composable
fun navigation(): Navigation {
    return LocalNavigation.current
}

/**
 * Provides a [ViewModel] instance scoped the screen's life.
 * When the user navigates away from the screen all screen scoped
 * viewModels are destroyed.
 * Does not apply for legacy screens.
 */
@Composable
inline fun <reified T : ViewModel> screenScopedViewModel(
    factory: ViewModelProvider.Factory? = null
): T {
    val viewModelStoreOwner = LocalViewModelStoreOwner.current
    requireNotNull(viewModelStoreOwner) { "No ViewModelStoreOwner provided" }
    val viewModelProvider = factory?.let {
        ViewModelProvider(viewModelStoreOwner, it)
    } ?: ViewModelProvider(viewModelStoreOwner)
    return viewModelProvider[T::class.java]
}
