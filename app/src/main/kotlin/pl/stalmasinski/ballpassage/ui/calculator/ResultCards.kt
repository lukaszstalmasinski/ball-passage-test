package pl.stalmasinski.ballpassage.ui.calculator

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import pl.stalmasinski.ballpassage.core.Ball
import pl.stalmasinski.ballpassage.core.BendRatio
import pl.stalmasinski.ballpassage.core.CalculationProblem
import pl.stalmasinski.ballpassage.core.CalculationResult
import pl.stalmasinski.ballpassage.ui.TestTags
import pl.stalmasinski.ballpassage.ui.balls.details
import pl.stalmasinski.ballpassage.ui.theme.LocalWarningColors
import java.math.BigDecimal

@Composable
fun ResultCard(state: CalculatorUiState, onOpenBalls: () -> Unit) {
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
                is CalculationResult.Success -> SuccessContent(result, state, onOpenBalls)
                is CalculationResult.Failure -> FailureContent(result)
                null -> PlaceholderContent(hasFieldErrors = state.hasFieldErrors)
            }
        }
    }
}

@Composable
private fun SuccessContent(result: CalculationResult.Success, state: CalculatorUiState, onOpenBalls: () -> Unit) {
    val formatter = rememberNumberFormatter()
    val shareText = shareText(result, state.fittingBalls.firstOrNull(), formatter)

    Row(verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.result_label), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.value_mm, formatter.format(result.minBallDiameter)),
                modifier = Modifier.testTag(TestTags.BALL_DIAMETER),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Medium,
            )
        }
        ShareButton(shareText)
    }
    Text(
        text = stringResource(
            R.string.result_range,
            formatter.format(result.minBallDiameter),
            formatter.format(result.maxBallDiameterExclusive),
        ),
        style = MaterialTheme.typography.bodyMedium,
    )
    if (result.bendRatio == BendRatio.StraightTube) {
        Text(stringResource(R.string.result_straight_tube), style = MaterialTheme.typography.bodyMedium)
    }
    if (result.isExtrapolated) {
        val warning = LocalWarningColors.current
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TestTags.EXTRAPOLATION_WARNING),
            colors = CardDefaults.cardColors(containerColor = warning.container, contentColor = warning.onContainer),
        ) {
            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(painterResource(R.drawable.ic_warning), contentDescription = null, modifier = Modifier.size(20.dp))
                Text(stringResource(R.string.result_extrapolated), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
    BallRecommendation(state.fittingBalls, state.hasBallSet, state.ballSetName, formatter, onOpenBalls)
}

@Composable
private fun BallRecommendation(
    fittingBalls: List<Ball>,
    hasBallSet: Boolean,
    ballSetName: String,
    formatter: NumberFormatter,
    onOpenBalls: () -> Unit,
) {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(painterResource(R.drawable.ic_ball), contentDescription = null, modifier = Modifier.size(20.dp))
        when {
            !hasBallSet -> TextButton(onClick = onOpenBalls) { Text(stringResource(R.string.ball_set_empty_action)) }
            fittingBalls.isEmpty() -> Text(
                text = stringResource(R.string.ball_none_fits),
                modifier = Modifier.testTag(TestTags.BALL_RECOMMENDATION),
                style = MaterialTheme.typography.bodyMedium,
            )
            else -> Column(modifier = Modifier.testTag(TestTags.BALL_RECOMMENDATION)) {
                val recommended = fittingBalls.first()
                val size = formatter.format(recommended.diameter)
                Text(
                    text = if (ballSetName.isEmpty()) {
                        stringResource(R.string.ball_recommended, size)
                    } else {
                        stringResource(R.string.ball_recommended_from_set, ballSetName, size)
                    },
                    style = MaterialTheme.typography.titleSmall,
                )
                recommended.details().takeIf { it.isNotEmpty() }?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
                if (fittingBalls.size > 1) {
                    Text(
                        text = stringResource(
                            R.string.ball_also_fits,
                            fittingBalls.drop(1).joinToString { formatter.format(it.diameter) },
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareButton(text: String) {
    val context = LocalContext.current
    val chooserTitle = stringResource(R.string.share)
    IconButton(
        onClick = {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(send, chooserTitle))
        },
        modifier = Modifier.testTag(TestTags.SHARE),
    ) {
        Icon(painterResource(R.drawable.ic_share), contentDescription = chooserTitle)
    }
}

/** Plain-text summary for protocols and messages. */
@Composable
private fun shareText(
    result: CalculationResult.Success,
    recommendedBall: Ball?,
    formatter: NumberFormatter,
): String {
    val dimensions = result.dimensions
    val lines = buildList {
        add(stringResource(R.string.share_heading))
        add(
            stringResource(
                R.string.share_input,
                formatter.format(dimensions.outerDiameter),
                formatter.format(dimensions.wallThickness),
                formatter.format(dimensions.bendRadius),
            ),
        )
        add(
            stringResource(
                R.string.share_calculation,
                formatter.format(result.innerDiameter),
                bendRatioText(result.bendRatio, formatter),
                formatter.format(result.deduction),
            ),
        )
        add(
            stringResource(
                R.string.share_result,
                formatter.format(result.minBallDiameter),
                formatter.format(result.maxBallDiameterExclusive),
            ),
        )
        recommendedBall?.let { ball ->
            val size = formatter.format(ball.diameter)
            val details = ball.details()
            add(
                if (details.isEmpty()) {
                    stringResource(R.string.share_recommended_ball, size)
                } else {
                    stringResource(R.string.share_recommended_ball_details, size, details)
                },
            )
        }
        if (result.isExtrapolated) add(stringResource(R.string.share_extrapolated))
    }
    return lines.joinToString("\n")
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
            CalculationProblem.INNER_DIAMETER_TOO_SMALL ->
                stringResource(R.string.problem_inner_diameter_too_small, formatter.format(result.innerDiameter))
            CalculationProblem.INNER_DIAMETER_TOO_LARGE ->
                stringResource(R.string.problem_inner_diameter_too_large, formatter.format(result.innerDiameter))
            CalculationProblem.BEND_RATIO_TOO_SMALL ->
                stringResource(
                    R.string.problem_bend_ratio_too_small,
                    bendRatioText(result.bendRatio, formatter),
                    formatter.format(result.minBendRadius),
                )
            // The view model reports this one on the wall thickness field and never shows a failed result for it.
            CalculationProblem.WALL_TOO_THICK -> null
        }
        message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun PlaceholderContent(hasFieldErrors: Boolean) {
    Text(stringResource(R.string.result_label), style = MaterialTheme.typography.titleMedium)
    Text(
        text = stringResource(if (hasFieldErrors) R.string.result_fix_inputs else R.string.result_placeholder),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

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

@Composable
private fun bendRatioText(bendRatio: BendRatio, formatter: NumberFormatter): String = when (bendRatio) {
    BendRatio.StraightTube -> stringResource(R.string.breakdown_straight_tube)
    is BendRatio.Finite -> formatter.format(bendRatio.value)
}
