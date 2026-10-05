package com.ivy.wallet.ui.edit.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.ivy.design.system.IvyMaterial3Theme
import com.ivy.legacy.datamodel.Account
import kotlinx.collections.immutable.toImmutableList
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class AccountPickerPaparazziTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6_PRO,
        maxPercentDifference = 0.005,
    )

    private val accounts = (1..100).map { index ->
        Account(
            name = when (index) {
                1 -> "Ucb Bank"
                2 -> "EBL Bank"
                3 -> "Prime Bank"
                4 -> "Cash"
                5 -> "Family savings and household expenses account"
                else -> "Savings account $index"
            },
            color = listOf(0xFF2F6B56, 0xFF7046FF, 0xFFB34C66)[index % 3].toInt(),
            currency = if (index % 3 == 0) "USD" else null,
            id = UUID(0, index.toLong()),
        )
    }

    @Test
    fun `twenty accounts dark theme`() {
        render(count = 20, dark = true)
    }

    @Test
    fun `single account light theme`() {
        render(count = 1)
    }

    @Test
    @Ignore("Paparazzi 1.3.5 drops static header drawing for pre-scrolled lists; verify on a device.")
    fun `hundred accounts selected at the end`() {
        render(count = 100, selectedIndex = 99)
    }

    @Test
    fun `currency search`() {
        render(count = 20, query = "usd")
    }

    @Test
    fun `no search results`() {
        render(count = 20, query = "missing")
    }

    @Test
    fun `empty account list`() {
        render(count = 0)
    }

    @Test
    fun `narrow picker with large text`() {
        render(count = 20, largeText = true)
    }

    @Test
    fun `short viewport leaves search and add account visible`() {
        render(count = 20, query = "bank", shortViewport = true)
    }

    @Test
    fun `selected account cards`() {
        snapshot {
            IvyMaterial3Theme(isTrueBlack = false, dark = true) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column(Modifier.width(320.dp).padding(24.dp)) {
                        AccountSelector(accounts[1], "BDT", {})
                        AccountSelector(accounts[4], "BDT", {}, Modifier.padding(top = 16.dp))
                        AccountSelector(null, "BDT", {}, Modifier.padding(top = 16.dp))
                    }
                }
            }
        }
    }

    private fun snapshot(content: @Composable () -> Unit) {
        val view = ComposeView(paparazzi.context).apply {
            setContent {
                // Keep static header content when layoutlib redraws a pre-scrolled list.
                Box(
                    Modifier.graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                ) { content() }
            }
        }
        try {
            paparazzi.snapshot(view, offsetMillis = 500)
        } finally {
            view.disposeComposition()
        }
    }

    private fun render(
        count: Int,
        dark: Boolean = false,
        query: String = "",
        selectedIndex: Int = 1,
        largeText: Boolean = false,
        shortViewport: Boolean = false,
    ) {
        snapshot {
            IvyMaterial3Theme(isTrueBlack = false, dark = dark) {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density.density, if (largeText) 1.5f else 1f),
                ) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                        Surface(
                            modifier = when {
                                shortViewport -> Modifier.fillMaxWidth().height(380.dp)
                                largeText -> Modifier.width(320.dp).fillMaxHeight()
                                else -> Modifier.fillMaxSize()
                            },
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                        ) {
                            AccountPickerContent(
                                title = "Choose account",
                                accounts = accounts.take(count).toImmutableList(),
                                selectedAccount = accounts.take(count).getOrNull(
                                    selectedIndex.coerceAtMost(count - 1)
                                ),
                                baseCurrency = "BDT",
                                query = query,
                                onQueryChange = {},
                                onSelect = {},
                                onAddAccount = {},
                                onClose = {},
                                modifier = Modifier.fillMaxSize().padding(top = 24.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
