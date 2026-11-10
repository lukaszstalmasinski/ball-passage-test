package pl.stalmasinski.ballpassage.ui.balls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pl.stalmasinski.ballpassage.R
import pl.stalmasinski.ballpassage.core.Ball
import pl.stalmasinski.ballpassage.core.CalculationResult
import pl.stalmasinski.ballpassage.ui.TestTags
import pl.stalmasinski.ballpassage.ui.common.rememberNumberFormatter

/** The calculator's current tube, so that the list can mark the balls that fit it. */
data class TubeContext(
    val result: CalculationResult.Success,
    val fittingBalls: List<Ball>,
)

/** Tells which tube from the calculator the marks in the list refer to. */
@Composable
internal fun TubeContextCard(tubeContext: TubeContext?, hasBalls: Boolean, modifier: Modifier = Modifier) {
    if (tubeContext == null && !hasBalls) return
    val formatter = rememberNumberFormatter()

    Card(
        modifier = modifier.testTag(TestTags.TUBE_CONTEXT),
        colors = CardDefaults.cardColors(
            containerColor = if (tubeContext != null) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (tubeContext == null) {
                Text(
                    text = stringResource(R.string.context_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }
            val dimensions = tubeContext.result.dimensions
            Text(
                text = stringResource(
                    R.string.context_tube,
                    formatter.format(dimensions.outerDiameter),
                    formatter.format(dimensions.wallThickness),
                    formatter.format(dimensions.bendRadius),
                ),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(
                    R.string.result_standard_ball,
                    formatter.format(tubeContext.result.minBallDiameter),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (hasBalls && tubeContext.fittingBalls.isEmpty()) {
                Text(stringResource(R.string.ball_none_fits), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
