package pl.stalmasinski.ballpassage.ui.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pl.stalmasinski.ballpassage.R
import pl.stalmasinski.ballpassage.core.calculation.CalculationResult
import pl.stalmasinski.ballpassage.ui.common.rememberNumberFormatter
import java.math.BigDecimal

/** Step-by-step values, so that the result can be checked by hand. */
@Composable
fun CalculationBreakdown(result: CalculationResult) {
    val formatter = rememberNumberFormatter()
    val mm = { value: BigDecimal -> formatter.format(value) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.breakdown_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
            )
            BreakdownRow(
                label = stringResource(R.string.breakdown_inner_diameter),
                value = stringResource(R.string.value_mm, mm(result.innerDiameter)),
            )
            BreakdownRow(
                label = stringResource(R.string.breakdown_bend_ratio),
                value = bendRatioText(result.bendRatio, formatter),
            )
            if (result is CalculationResult.Success) {
                BreakdownRow(
                    label = stringResource(R.string.breakdown_deduction),
                    value = stringResource(R.string.value_mm, mm(result.deduction)),
                )
                HorizontalDivider()
                BreakdownRow(
                    label = stringResource(R.string.breakdown_ball_diameter),
                    value = stringResource(R.string.value_mm, mm(result.minBallDiameter)),
                    emphasized = true,
                )
            }
        }
    }
}

@Composable
private fun BreakdownRow(label: String, value: String, emphasized: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = if (emphasized) Color.Unspecified else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
