package com.ivy.accounts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.design.system.IvyMaterial3Theme
import com.ivy.legacy.data.model.AccountData
import com.ivy.legacy.data.model.CreditCardData
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.util.UUID

class CreditCardsPaparazziTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5, maxPercentDifference = 0.005)
    private val id = AccountId(UUID(0, 1))
    private val main = AccountData(
        Account(
            id, NotBlankTrimmedString.unsafe("EBL Visa Platinum"), AssetCode.unsafe("BDT"),
            ColorInt(0xFF6750A4.toInt()), null, false, 0.0, 100000.0, id, true, 120.0
        ),
        -20000.0,
        null,
        0.0,
        0.0,
    )
    private val second = main.copy(
        account = main.account.copy(
            id = AccountId(UUID(0, 2)),
            asset = AssetCode.unsafe("USD"),
            creditLimit = 1000.0,
            creditLimitShared = false,
            creditExchangeRate = null
        ),
        balance = -100.0,
    )

    @Test fun sharedDualCurrencyDark() = render(dark = true)

    @Test fun independentDualCurrencyLight() = render(shared = false)

    @Test fun narrowLargeText() = render(largeText = true)
    private fun render(dark: Boolean = false, shared: Boolean = true, largeText: Boolean = false) {
        paparazzi.snapshot {
            IvyMaterial3Theme(isTrueBlack = false, dark = dark) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, if (largeText) 1.5f else 1f)) {
                    Surface {
                        Column(Modifier.width(if (largeText) 320.dp else 393.dp).padding(top = 24.dp)) {
                            CreditCardsSection(
                                today = LocalDate.of(2026, 9, 6),
                                cards = persistentListOf(
                                    CreditCardData(
                                        main.copy(account = main.account.copy(creditLimitShared = shared)),
                                        second
                                    )
                                ),
                                onCardClick = {},
                                onAddCard = {},
                            )
                        }
                    }
                }
            }
        }
    }
}
