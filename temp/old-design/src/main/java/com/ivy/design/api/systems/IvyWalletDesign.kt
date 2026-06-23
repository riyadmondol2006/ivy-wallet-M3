package com.ivy.design.api.systems

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivy.base.legacy.Theme
import com.ivy.design.api.IvyDesign
import com.ivy.design.l0_system.*

@Deprecated("Old design system. Use `:ivy-design` and Material3")
abstract class IvyWalletDesign : IvyDesign {
    companion object {
        // Zeroed for the M3 redesign: these compensated for Raleway/Open Sans metrics; Roboto
        // (now the legacy face) sits on the normal baseline and needs no shift.
        const val OPEN_SANS_BASELINE_SHIFT = 0.0f
        const val RALEWAY_BASELINE_SHIFT = 0.0f
    }

    @Deprecated("Old design system. Use `:ivy-design` and Material3")
    override fun typography(): IvyTypography {
        // Material 3 redesign: both legacy faces now resolve to system Roboto so every legacy
        // screen that still reads `UI.typo` inherits the authentic Google/M3 look. The bundled
        // Raleway/Open Sans assets remain in res/font for old previews but are no longer used here.
        val openSans = FontFamily.Default
        val raleWay = FontFamily.Default

        val h1 = 40.sp
        val h2 = 32.sp
        val b1 = 20.sp
        val b2 = 16.sp
        val c = 12.sp

        return object : IvyTypography {
            override val h1 = TextStyle(
                fontFamily = raleWay,
                fontWeight = FontWeight.Black,
                fontSize = h1,
                baselineShift = BaselineShift(RALEWAY_BASELINE_SHIFT),
            )
            override val h2 = TextStyle(
                fontFamily = raleWay,
                fontWeight = FontWeight.ExtraBold,
                fontSize = h2,
                baselineShift = BaselineShift(RALEWAY_BASELINE_SHIFT),
            )
            override val b1 = TextStyle(
                fontFamily = raleWay,
                fontWeight = FontWeight.Bold,
                fontSize = b1,
                baselineShift = BaselineShift(RALEWAY_BASELINE_SHIFT),
            )
            override val b2 = TextStyle(
                fontFamily = raleWay,
                fontWeight = FontWeight.Medium,
                fontSize = b2,
                baselineShift = BaselineShift(RALEWAY_BASELINE_SHIFT),
            )
            override val c = TextStyle(
                fontFamily = raleWay,
                fontWeight = FontWeight.ExtraBold,
                fontSize = c,
                baselineShift = BaselineShift(RALEWAY_BASELINE_SHIFT),
            )

            override val nH1 = TextStyle(
                fontFamily = openSans,
                fontWeight = FontWeight.Bold,
                fontSize = h1,
                baselineShift = BaselineShift(OPEN_SANS_BASELINE_SHIFT),
            )
            override val nH2 = TextStyle(
                fontFamily = openSans,
                fontWeight = FontWeight.Bold,
                fontSize = h2,
                baselineShift = BaselineShift(OPEN_SANS_BASELINE_SHIFT),
            )
            override val nB1 = TextStyle(
                fontFamily = openSans,
                fontWeight = FontWeight.Bold,
                fontSize = b1,
                baselineShift = BaselineShift(OPEN_SANS_BASELINE_SHIFT),
            )
            override val nB2 = TextStyle(
                fontFamily = openSans,
                fontWeight = FontWeight.Normal,
                fontSize = b2,
                baselineShift = BaselineShift(OPEN_SANS_BASELINE_SHIFT),
            )
            override val nC = TextStyle(
                fontFamily = openSans,
                fontWeight = FontWeight.Bold,
                fontSize = c,
                baselineShift = BaselineShift(OPEN_SANS_BASELINE_SHIFT),
            )
        }
    }

    @Deprecated("Old design system. Use `:ivy-design` and Material3")
    override fun colors(theme: Theme, isDarkModeEnabled: Boolean): IvyColors {
        return when (theme) {
            Theme.LIGHT -> object : IvyColors {
                override val pure = White
                override val pureInverse = Black
                override val gray = Gray
                override val medium = MediumWhite
                override val mediumInverse = MediumBlack

                override val primary = Purple
                override val primary1 = IvyDark

                override val green = Green
                override val green1 = GreenLight

                override val orange = Orange
                override val orange1 = OrangeLight

                override val red = Red
                override val red1 = RedLight
                override val red1Inverse = RedDark

                override val isLight = true
            }

            Theme.DARK -> object : IvyColors {
                override val pure = Black
                override val pureInverse = White
                override val gray = Gray
                override val medium = MediumBlack
                override val mediumInverse = MediumWhite

                override val primary = Purple
                override val primary1 = IvyLight

                override val green = Green
                override val green1 = GreenDark

                override val orange = Orange
                override val orange1 = OrangeDark

                override val red = Red
                override val red1 = RedDark
                override val red1Inverse = RedLight

                override val isLight = false
            }

            Theme.AMOLED_DARK -> object : IvyColors {
                override val pure = TrueBlack
                override val pureInverse = White
                override val gray = Gray
                override val medium = MediumBlack
                override val mediumInverse = MediumWhite

                override val primary = Purple
                override val primary1 = IvyLight

                override val green = Green
                override val green1 = GreenDark

                override val orange = Orange
                override val orange1 = OrangeDark

                override val red = Red
                override val red1 = RedDark
                override val red1Inverse = RedLight

                override val isLight = false
            }

            Theme.AUTO -> if (isDarkModeEnabled) {
                colors(Theme.DARK, true)
            } else {
                colors(
                    Theme.LIGHT,
                    false
                )
            }
        }
    }

    @Deprecated("Old design system. Use `:ivy-design` and Material3")
    override fun shapes(): IvyShapes {
        return object : IvyShapes() {
            override val r1 = RoundedCornerShape(32.dp)
            override val r1Top = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            override val r1Bot = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)

            override val r2 = RoundedCornerShape(24.dp)
            override val r2Top = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            override val r2Bot = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)

            override val r3 = RoundedCornerShape(20.dp)
            override val r3Top = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            override val r3Bot = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)

            override val r4 = RoundedCornerShape(16.dp)
            override val r4Top = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            override val r4Bot = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
        }
    }
}
