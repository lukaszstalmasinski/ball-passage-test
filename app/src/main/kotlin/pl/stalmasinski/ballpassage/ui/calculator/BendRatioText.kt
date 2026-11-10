package pl.stalmasinski.ballpassage.ui.calculator

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import pl.stalmasinski.ballpassage.R
import pl.stalmasinski.ballpassage.core.calculation.BendRatio
import pl.stalmasinski.ballpassage.ui.common.NumberFormatter

@Composable
internal fun bendRatioText(bendRatio: BendRatio, formatter: NumberFormatter): String = when (bendRatio) {
    BendRatio.StraightTube -> stringResource(R.string.breakdown_straight_tube)
    is BendRatio.Finite -> formatter.format(bendRatio.value)
}
