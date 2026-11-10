package pl.stalmasinski.ballpassage.core.calculation

import java.math.BigDecimal

/**
 * VGB-S-013-00-2014-12, section 7.2.5, Table 3 (Kugeldurchlaufversuch):
 * by how many millimetres the ball diameter must be smaller than the inner diameter of the tube.
 */
public object BallPassageTable {
    private val deductions: Map<DiameterRow, List<BigDecimal>> = mapOf(
        DiameterRow.D20_29 to listOf("6", "5.5", "5", "4"),
        DiameterRow.D30_39 to listOf("7", "6.5", "6", "5"),
        DiameterRow.D40_49 to listOf("8", "7.5", "7", "6"),
        DiameterRow.D50_57 to listOf("9", "8.5", "8", "7"),
    ).mapValues { (_, values) -> values.map(::BigDecimal) }

    public fun deduction(row: DiameterRow, column: BendRatioColumn): BigDecimal =
        deductions.getValue(row)[column.ordinal]
}
