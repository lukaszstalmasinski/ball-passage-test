package pl.stalmasinski.ballpassage.ui.calculator

import java.math.BigDecimal

/** Handling of decimal numbers typed by the user; both `.` and `,` are accepted as separators. */
object DecimalInput {
    const val MAX_LENGTH = 9

    private val pattern = Regex("""[0-9]+(\.[0-9]*)?|\.[0-9]+""")

    /** Drops characters that can never be part of a number and limits the length. */
    fun sanitize(text: String): String =
        text.filter { it in '0'..'9' || it == '.' || it == ',' }.take(MAX_LENGTH)

    /** Returns the parsed value, or null when [text] is not a valid non-negative number. */
    fun parse(text: String): BigDecimal? {
        val normalized = text.trim().replace(',', '.')
        return if (pattern.matches(normalized)) BigDecimal(normalized) else null
    }

    /** Formats [value] for an input field: no grouping separators, so that it parses back. */
    fun format(value: BigDecimal, decimalSeparator: Char): String =
        value.toPlainString().replace('.', decimalSeparator)
}
