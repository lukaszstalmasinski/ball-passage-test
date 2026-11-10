package pl.stalmasinski.ballpassage.ui.calculator

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import pl.stalmasinski.ballpassage.R
import pl.stalmasinski.ballpassage.core.KtaTube
import pl.stalmasinski.ballpassage.core.Tube
import pl.stalmasinski.ballpassage.core.TubeDimensions

/** Short description of a tube, e.g. "48.3 × 5 · R 70" or, for KTA 3211.1, "25 × 2.6 · Δ 0.5". */
@Composable
fun tubeLabel(tube: Tube, formatter: NumberFormatter): String = when (tube) {
    is TubeDimensions -> stringResource(
        R.string.recent_item,
        formatter.format(tube.outerDiameter),
        formatter.format(tube.wallThickness),
        formatter.format(tube.bendRadius),
    )
    is KtaTube -> stringResource(
        R.string.recent_item_kta,
        formatter.format(tube.outerDiameter),
        formatter.format(tube.wallThickness),
        formatter.format(tube.undersize),
    )
}
