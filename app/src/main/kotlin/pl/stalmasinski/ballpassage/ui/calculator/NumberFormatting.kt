package pl.stalmasinski.ballpassage.ui.calculator

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat

/** Formats numbers with the separators of the current app locale, e.g. 31.8 or 31,8. */
fun interface NumberFormatter {
    fun format(value: BigDecimal): String
}

@Composable
fun rememberNumberFormatter(minimumFractionDigits: Int = 0): NumberFormatter {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale, minimumFractionDigits) {
        val format = NumberFormat.getNumberInstance(locale).apply {
            this.minimumFractionDigits = minimumFractionDigits
            maximumFractionDigits = 3
            roundingMode = RoundingMode.HALF_UP
        }
        NumberFormatter { format.format(it) }
    }
}
