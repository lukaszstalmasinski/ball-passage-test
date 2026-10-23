package pl.stalmasinski.ballpassage.ui

import pl.stalmasinski.ballpassage.ui.calculator.DimensionField
import java.math.BigDecimal

/** Stable node tags for UI tests. */
object TestTags {
    const val BALL_DIAMETER = "ball_diameter"
    const val BALL_RECOMMENDATION = "ball_recommendation"
    const val EXTRAPOLATION_WARNING = "extrapolation_warning"
    const val FAILURE = "failure"
    const val SHARE = "share"
    const val CLEAR_ALL = "clear_all"
    const val CLEAR_HISTORY = "clear_history"
    const val COMPACT_RESULT = "compact_result"
    const val BALL_INPUT = "ball_input"
    const val BALL_ADD = "ball_add"

    fun field(field: DimensionField) = "field_${field.name}"

    fun tableCell(rowIndex: Int, columnIndex: Int) = "cell_${rowIndex}_$columnIndex"

    fun historyItem(index: Int) = "history_$index"

    fun ballItem(diameter: BigDecimal) = "ball_${diameter.toPlainString()}"

    fun destination(name: String) = "nav_$name"
}
