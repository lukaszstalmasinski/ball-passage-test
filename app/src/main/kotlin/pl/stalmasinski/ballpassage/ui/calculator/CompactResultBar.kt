package pl.stalmasinski.ballpassage.ui.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pl.stalmasinski.ballpassage.R
import pl.stalmasinski.ballpassage.core.CalculationResult
import pl.stalmasinski.ballpassage.ui.TestTags
import pl.stalmasinski.ballpassage.ui.common.rememberNumberFormatter

/** Shown above the keyboard while typing, when the result card may be covered. */
@Composable
fun CompactResultBar(state: CalculatorUiState) {
    val formatter = rememberNumberFormatter()
    val result = state.result
    val (container, content) = when (result) {
        is CalculationResult.Success -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        is CalculationResult.Failure -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        null -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TestTags.COMPACT_RESULT),
        color = container,
        contentColor = content,
    ) {
        Row(
            modifier = Modifier
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (result) {
                is CalculationResult.Success -> {
                    Text(
                        text = stringResource(R.string.compact_result, formatter.format(result.minBallDiameter)),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    if (result.isExtrapolated) {
                        Icon(painterResource(R.drawable.ic_warning), contentDescription = stringResource(R.string.compact_extrapolated))
                    }
                    state.fittingBalls.firstOrNull()?.let { ball ->
                        val size = formatter.format(ball.diameter)
                        Text(
                            text = if (ball.label.isEmpty()) {
                                stringResource(R.string.compact_ball, size)
                            } else {
                                stringResource(R.string.compact_ball_label, size, ball.label)
                            },
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }
                is CalculationResult.Failure -> Text(
                    text = stringResource(R.string.result_failure_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                null -> Text(
                    text = stringResource(if (state.hasFieldErrors) R.string.result_fix_inputs else R.string.compact_placeholder),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
