package pl.stalmasinski.ballpassage.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.stalmasinski.ballpassage.R
import pl.stalmasinski.ballpassage.ui.balls.BallsRoute
import pl.stalmasinski.ballpassage.ui.calculator.CalculatorScreen
import pl.stalmasinski.ballpassage.ui.calculator.CalculatorViewModel
import pl.stalmasinski.ballpassage.ui.calculator.CompactResultBar
import pl.stalmasinski.ballpassage.ui.info.InfoScreen

enum class Destination(@StringRes val label: Int, @DrawableRes val icon: Int) {
    CALCULATOR(R.string.nav_calculator, R.drawable.ic_calculate),
    BALLS(R.string.nav_balls, R.drawable.ic_ball),
    INFO(R.string.nav_info, R.drawable.ic_info),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BallPassageApp(calculatorViewModel: CalculatorViewModel = viewModel(factory = CalculatorViewModel.Factory)) {
    var destination by rememberSaveable { mutableStateOf(Destination.CALCULATOR) }
    val calculatorState by calculatorViewModel.uiState.collectAsStateWithLifecycle()

    // Back from another tab returns to the calculator instead of leaving the app.
    BackHandler(enabled = destination != Destination.CALCULATOR) { destination = Destination.CALCULATOR }

    Scaffold(
        bottomBar = {
            // While typing, the navigation bar gives way to the result so that it stays visible above
            // the keyboard on small screens.
            if (WindowInsets.isImeVisible) {
                if (destination == Destination.CALCULATOR) CompactResultBar(calculatorState)
            } else {
                NavigationBar {
                    Destination.entries.forEach { item ->
                        NavigationBarItem(
                            selected = item == destination,
                            onClick = { destination = item },
                            icon = { Icon(painterResource(item.icon), contentDescription = null) },
                            label = {
                                FittingText(
                                    text = stringResource(item.label),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = LocalContentColor.current,
                                )
                            },
                            modifier = Modifier.testTag(TestTags.destination(item.name)),
                        )
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            when (destination) {
                Destination.CALCULATOR -> CalculatorScreen(
                    state = calculatorState,
                    onValueChange = calculatorViewModel::onValueChange,
                    onClearAll = calculatorViewModel::clearAll,
                    onApplyHistory = calculatorViewModel::applyHistory,
                    onClearHistory = calculatorViewModel::clearHistory,
                    onOpenBalls = { destination = Destination.BALLS },
                )
                Destination.BALLS -> BallsRoute()
                Destination.INFO -> InfoScreen()
            }
        }
    }
}
