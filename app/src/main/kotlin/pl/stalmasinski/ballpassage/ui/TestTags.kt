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
    const val BALL_LABEL = "ball_label"
    const val BALL_LOCATION = "ball_location"
    const val BALLS_MENU = "balls_menu"
    const val SET_SELECTOR = "set_selector"
    const val SET_NEW = "set_new"
    const val SET_NAME = "set_name"
    const val TUBE_CONTEXT = "tube_context"
    const val RANGE_OPEN = "range_open"
    const val RANGE_CONFIRM = "range_confirm"
    const val DIALOG_CONFIRM = "dialog_confirm"
    const val EXPORT = "export"
    const val EXPORT_SHEET = "export_sheet"
    const val EXPORT_SHARE_TEXT = "export_share_text"
    const val QR_CODE = "qr_code"
    const val QR_SCANNER = "qr_scanner"
    const val IMPORT_TEXT = "import_text"

    fun field(field: DimensionField) = "field_${field.name}"

    fun tableCell(rowIndex: Int, columnIndex: Int) = "cell_${rowIndex}_$columnIndex"

    fun historyItem(index: Int) = "history_$index"

    fun ballItem(diameter: BigDecimal) = "ball_${diameter.toPlainString()}"

    fun destination(name: String) = "nav_$name"

    fun rangeField(index: Int) = "range_field_$index"
}
