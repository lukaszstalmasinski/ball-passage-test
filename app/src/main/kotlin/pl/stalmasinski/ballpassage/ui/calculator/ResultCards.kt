package pl.stalmasinski.ballpassage.ui.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pl.stalmasinski.ballpassage.R
import pl.stalmasinski.ballpassage.core.BendRatio
import pl.stalmasinski.ballpassage.core.CalculationProblem
import pl.stalmasinski.ballpassage.core.CalculationResult
import pl.stalmasinski.ballpassage.ui.theme.LocalWarningColors
import java.math.BigDecimal

@Composable
fun ResultCard(state: CalculatorUiState) {
    val result = state.result
    val colors = when (result) {
        is CalculationResult.Success -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        is CalculationResult.Failure -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        )
        null -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    }

    Card(modifier = Modifier.fillMaxWidth(), colors = colors) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (result) {
                is CalculationResult.Success -> SuccessContent(result)
                is CalculationResult.Failure -> FailureContent(result)
                null -> PlaceholderContent(hasFieldErrors = state.hasFieldErrors)
            }
        }
    }
}

@Composable
private fun SuccessContent(result: CalculationResult.Success) {
    val formatter = rememberNumberFormatter()

    Text(stringResource(R.string.result_label), style = MaterialTheme.typography.titleMedium)
    Text(
        text = stringResource(R.string.value_mm, formatter.format(result.ballDiameter)),
        modifier = Modifier.testTag(TestTags.BALL_DIAMETER),
        style = MaterialTheme.typography.displayMedium,
        fontWeight = FontWeight.Medium,
    )
    if (result.isExtrapolated) {
        val warning = LocalWarningColors.current
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TestTags.EXTRAPOLATION_WARNING),
            colors = CardDefaults.cardColors(containerColor = warning.container, contentColor = warning.onContainer),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(painterResource(R.drawable.ic_warning), contentDescription = null, modifier = Modifier.size(20.dp))
                Text(stringResource(R.string.result_extrapolated), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun FailureContent(result: CalculationResult.Failure) {
    val formatter = rememberNumberFormatter()

    Row(
        modifier = Modifier.testTag(TestTags.FAILURE),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_error), contentDescription = null)
        Text(stringResource(R.string.result_failure_title), style = MaterialTheme.typography.titleMedium)
    }
    CalculationProblem.entries.filter { it in result.problems }.forEach { problem ->
        val message = when (problem) {
            CalculationProblem.INNER_DIAMETER_OUT_OF_RANGE ->
                stringResource(R.string.problem_inner_diameter_out_of_range, formatter.format(result.innerDiameter))
            CalculationProblem.BEND_RATIO_TOO_SMALL ->
                stringResource(R.string.problem_bend_ratio_too_small, bendRatioText(result.bendRatio, formatter))
            // Reported on the wall thickness field instead, see CalculatorViewModel.
            CalculationProblem.WALL_TOO_THICK -> stringResource(R.string.error_wall_too_thick)
        }
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PlaceholderContent(hasFieldErrors: Boolean) {
    Text(stringResource(R.string.result_label), style = MaterialTheme.typography.titleMedium)
    Text(
        text = stringResource(R.string.no_value),
        style = MaterialTheme.typography.displayMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = stringResource(if (hasFieldErrors) R.string.result_fix_inputs else R.string.result_placeholder),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

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
                    value = stringResource(R.string.value_mm, mm(result.ballDiameter)),
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

@Composable
private fun bendRatioText(bendRatio: BendRatio, formatter: NumberFormatter): String = when (bendRatio) {
    BendRatio.StraightTube -> stringResource(R.string.breakdown_straight_tube)
    is BendRatio.Finite -> formatter.format(bendRatio.value)
}
