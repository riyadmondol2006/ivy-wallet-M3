package com.ivy.navigation

import android.annotation.SuppressLint
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import com.ivy.design.system.IvyMotion
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlin.coroutines.cancellation.CancellationException

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
    val screenViewModels = rememberScreenViewModelStoreOwner()
    CompositionLocalProvider(
        LocalNavigation provides navigation,
        LocalViewModelStoreOwner provides screenViewModels,
    ) {
        DisposableEffect(navigation.currentScreen) {
            onDispose {
                // Destroy viewModels only for non-legacy screens
                if (navigation.lastScreen?.isLegacy == false) {
                    screenViewModels.viewModelStore.clear()
                }
            }
        }

        // Seekable transition state so a predictive-back gesture can *scrub* the screen change with
        // the user's finger, instead of the change happening instantly on release.
        val transitionState = remember {
            SeekableTransitionState<Screen?>(navigation.currentScreen)
        }
        // Direction of the in-flight transition (true = back/pop). Mirrors the shared axis so a
        // forward push slides in from the right and a pop slides in from the left.
        var backwards by remember { mutableStateOf(false) }

        // Normal (non-gesture) navigations: settle the seekable state onto the new current screen.
        LaunchedEffect(navigation.currentScreen) {
            if (transitionState.currentState != navigation.currentScreen) {
                backwards = navigation.isBack
                transitionState.animateTo(navigation.currentScreen)
            }
        }

        // Predictive back — enabled only for a plain non-legacy screen pop with no modal or custom
        // back override active, so the legacy back handling (RootActivity callback, modals, the
        // onBackPressed map, per-screen overrides) keeps working exactly as before everywhere else.
        val current = navigation.currentScreen
        val canPredictiveBack = current != null &&
            current.isLegacy == false &&
            !navigation.backStackEmpty() &&
            !navigation.hasBackOverride() &&
            !navigation.hasModalBackHandler()
        PredictiveBackHandler(enabled = canPredictiveBack) { progress ->
            val target = navigation.peekBack()
            if (target == null) {
                // Nothing to pop to; let the gesture no-op.
                try {
                    progress.collect { }
                } catch (_: CancellationException) {
                    // ignored
                }
                return@PredictiveBackHandler
            }
            backwards = true
            try {
                progress.collect { backEvent ->
                    transitionState.seekTo(backEvent.progress, targetState = target)
                }
                // Released past the threshold -> commit the pop; the LaunchedEffect above then
                // finishes the in-flight seek onto the now-current screen.
                navigation.back()
            } catch (_: CancellationException) {
                // Cancelled -> animate the partial seek back to the screen we started on.
                transitionState.animateTo(navigation.currentScreen)
                backwards = navigation.isBack
            }
        }

        // Wrap the screen switch in a SharedTransitionLayout so shared elements can still morph.
        SharedTransitionLayout {
            CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                val transition = rememberTransition(transitionState, label = "ivy-screen-transition")
                transition.AnimatedContent(
                    transitionSpec = {
                        // Material-3 shared-axis X, direction from [backwards].
                        val forward = !backwards
                        IvyMotion.sharedAxisXEnter(forward) togetherWith
                            IvyMotion.sharedAxisXExit(forward)
                    },
                    // Animate only on genuine screen changes, not on in-screen state updates.
                    contentKey = { it?.let { screen -> screen::class.qualifiedName } ?: "null" },
                ) { screen ->
                    CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                        navGraph(screen)
                    }
                }
            }
        }
    }
}

/**
 * Keeps the screens' ViewModels in their own store, inside a holder that lives in the activity's
 * store. Leaving a screen clears only this store: clearing the activity's store would also destroy
 * activity-level ViewModels such as the root one, which then comes back uninitialised and leaves
 * the app blank (e.g. after restoring a cloud backup during onboarding). The holder survives
 * configuration changes, so screen ViewModels do too, as before.
 */
@Composable
private fun rememberScreenViewModelStoreOwner(): ViewModelStoreOwner {
    val parent = LocalViewModelStoreOwner.current
        // Previews and screenshot tests have no owner; their screens don't use ViewModels.
        ?: return remember {
            object : ViewModelStoreOwner {
                override val viewModelStore = ViewModelStore()
            }
        }
    val holder: ScreenViewModelsHolder = viewModel(
        viewModelStoreOwner = parent,
        factory = viewModelFactory { initializer { ScreenViewModelsHolder() } },
    )
    return remember(parent, holder) {
        object : ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
            override val viewModelStore: ViewModelStore = holder.store

            // Screens are created by the activity's factory (Hilt), exactly as before.
            override val defaultViewModelProviderFactory: ViewModelProvider.Factory
                get() = (parent as? HasDefaultViewModelProviderFactory)
                    ?.defaultViewModelProviderFactory
                    ?: ViewModelProvider.NewInstanceFactory()

            override val defaultViewModelCreationExtras: CreationExtras
                get() = (parent as? HasDefaultViewModelProviderFactory)
                    ?.defaultViewModelCreationExtras
                    ?: CreationExtras.Empty
        }
    }
}

internal class ScreenViewModelsHolder : ViewModel() {
    val store = ViewModelStore()

    override fun onCleared() {
        store.clear()
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
