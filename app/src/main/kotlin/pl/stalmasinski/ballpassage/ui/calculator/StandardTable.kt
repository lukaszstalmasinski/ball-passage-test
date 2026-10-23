package pl.stalmasinski.ballpassage.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pl.stalmasinski.ballpassage.R
import pl.stalmasinski.ballpassage.core.BallPassageTable
import pl.stalmasinski.ballpassage.core.BendRatioColumn
import pl.stalmasinski.ballpassage.core.CalculationResult
import pl.stalmasinski.ballpassage.core.DiameterRow
import pl.stalmasinski.ballpassage.ui.TestTags

/** VGB-S-013 Table 3 with the cell used for the current result highlighted. */
@Composable
fun StandardTableCard(result: CalculationResult?) {
    val formatter = rememberNumberFormatter()
    // Bounds are shown as in the standard: 0.5, 1.0, 1.5, 2.0.
    val boundFormatter = rememberNumberFormatter(minimumFractionDigits = 1)
    val activeRow = (result as? CalculationResult.Success)?.row
    val activeColumn = (result as? CalculationResult.Success)?.column

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.table_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.table_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Column(modifier = Modifier.padding(top = 12.dp)) {
                TableRow {
                    HeaderCell("", weight = ROW_HEADER_WEIGHT)
                    HeaderCell(stringResource(R.string.table_header_ratio), weight = BendRatioColumn.entries.size.toFloat())
                }
                TableRow {
                    HeaderCell(stringResource(R.string.table_header_diameter), weight = ROW_HEADER_WEIGHT)
                    BendRatioColumn.entries.forEach { column ->
                        HeaderCell(column.boundsText(boundFormatter, "\n"), weight = 1f, highlighted = column == activeColumn)
                    }
                }
                DiameterRow.entries.forEachIndexed { rowIndex, row ->
                    TableRow {
                        HeaderCell(
                            text = if (row.coveredByStandard) "${row.min}–${row.max}" else "${row.min}–${row.max}*",
                            weight = ROW_HEADER_WEIGHT,
                            highlighted = row == activeRow,
                            italic = !row.coveredByStandard,
                        )
                        BendRatioColumn.entries.forEachIndexed { columnIndex, column ->
                            val deduction = formatter.format(BallPassageTable.deduction(row, column))
                            ValueCell(
                                text = deduction,
                                description = stringResource(
                                    R.string.table_cell_description,
                                    "${row.min}–${row.max}",
                                    column.boundsText(boundFormatter, " "),
                                    deduction,
                                ),
                                active = row == activeRow && column == activeColumn,
                                crossHighlighted = row == activeRow || column == activeColumn,
                                italic = !row.coveredByStandard,
                                modifier = Modifier.testTag(TestTags.tableCell(rowIndex, columnIndex)),
                            )
                        }
                    }
                }
            }

            Text(
                text = stringResource(R.string.table_footnote),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private const val ROW_HEADER_WEIGHT = 1.3f

private fun BendRatioColumn.boundsText(formatter: NumberFormatter, separator: String): String {
    val lower = formatter.format(lowerBound)
    return upperBound?.let { "≥ $lower$separator< ${formatter.format(it)}" } ?: "≥ $lower"
}

@Composable
private fun TableRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        content = content,
    )
}

@Composable
private fun RowScope.HeaderCell(
    text: String,
    weight: Float,
    highlighted: Boolean = false,
    italic: Boolean = false,
) {
    Box(
        modifier = Modifier
            .weight(weight)
            .fillMaxHeight()
            .padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
            color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun RowScope.ValueCell(
    text: String,
    description: String,
    active: Boolean,
    crossHighlighted: Boolean,
    italic: Boolean,
    modifier: Modifier = Modifier,
) {
    val background = when {
        active -> MaterialTheme.colorScheme.primary
        crossHighlighted -> MaterialTheme.colorScheme.secondaryContainer
        else -> Color.Transparent
    }
    Box(
        modifier = modifier
            .weight(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .padding(vertical = 10.dp)
            .semantics {
                selected = active
                contentDescription = description
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
            color = when {
                active -> MaterialTheme.colorScheme.onPrimary
                crossHighlighted -> MaterialTheme.colorScheme.onSecondaryContainer
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
    }
}
