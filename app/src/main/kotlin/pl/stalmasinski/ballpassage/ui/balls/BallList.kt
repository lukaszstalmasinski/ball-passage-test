package pl.stalmasinski.ballpassage.ui.balls

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pl.stalmasinski.ballpassage.R
import pl.stalmasinski.ballpassage.core.balls.Ball
import pl.stalmasinski.ballpassage.ui.TestTags
import pl.stalmasinski.ballpassage.ui.common.details
import pl.stalmasinski.ballpassage.ui.common.rememberNumberFormatter
import androidx.compose.foundation.shape.RoundedCornerShape as Rounded

/**
 * The balls of the active set as items of the screen's LazyColumn, one item per ball, so that only
 * the visible rows are composed. Together they look like one card: the hint has the card's top
 * corners and the last ball its bottom corners.
 */
internal fun LazyListScope.ballList(
    balls: List<Ball>,
    tubeContext: TubeContext?,
    onEdit: (Ball) -> Unit,
    onRemove: (Ball) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (balls.isEmpty()) {
        item(key = "balls-empty") {
            Card(
                modifier = modifier,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Text(
                    text = stringResource(R.string.balls_empty),
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }

    val recommendedId = tubeContext?.fittingBalls?.firstOrNull()?.id
    val fittingIds = tubeContext?.fittingBalls.orEmpty().map { it.id }.toSet()

    item(key = "balls-hint") {
        CardPart(top = true, bottom = false, modifier = modifier) {
            // One hint for the whole list instead of repeating it in every row.
            Text(
                text = stringResource(R.string.ball_add_details),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    itemsIndexed(balls, key = { _, ball -> ball.id }) { index, ball ->
        val last = index == balls.lastIndex
        CardPart(top = false, bottom = last, modifier = modifier) {
            Column(modifier = Modifier.padding(bottom = if (last) 8.dp else 0.dp)) {
                if (index > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                BallRow(
                    ball = ball,
                    badge = when (ball.id) {
                        recommendedId -> R.string.ball_badge_recommended
                        in fittingIds -> R.string.ball_badge_fits
                        else -> null
                    },
                    onEdit = onEdit,
                    onRemove = onRemove,
                )
            }
        }
    }
}

/** A slice of the list's card: its background, with the card's corners only where they belong. */
@Composable
private fun CardPart(top: Boolean, bottom: Boolean, modifier: Modifier, content: @Composable () -> Unit) {
    // The card uses the medium shape of the theme.
    val corner = MaterialTheme.shapes.medium.topStart
    val none = CornerSize(0.dp)
    val shape: Shape = Rounded(
        topStart = if (top) corner else none,
        topEnd = if (top) corner else none,
        bottomEnd = if (bottom) corner else none,
        bottomStart = if (bottom) corner else none,
    )
    Surface(modifier = modifier, shape = shape, color = MaterialTheme.colorScheme.surfaceContainerLow, content = content)
}

@Composable
private fun BallRow(ball: Ball, @StringRes badge: Int?, onEdit: (Ball) -> Unit, onRemove: (Ball) -> Unit) {
    val formatter = rememberNumberFormatter()
    val size = formatter.format(ball.diameter)
    val details = ball.details()
    ListItem(
        headlineContent = {
            Text(stringResource(R.string.ball_size, size), style = MaterialTheme.typography.titleMedium)
        },
        supportingContent = if (details.isNotEmpty()) {
            { Text(details) }
        } else {
            null
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (badge != null) FitBadge(badge, recommended = badge == R.string.ball_badge_recommended)
                IconButton(onClick = { onRemove(ball) }) {
                    Icon(
                        painterResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.balls_remove, size),
                    )
                }
            }
        },
        modifier = Modifier
            .clickable { onEdit(ball) }
            .padding(start = 4.dp)
            .testTag(TestTags.ballItem(ball.diameter)),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun FitBadge(@StringRes text: Int, recommended: Boolean) {
    Surface(
        shape = Rounded(8.dp),
        color = if (recommended) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (recommended) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            text = stringResource(text),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
