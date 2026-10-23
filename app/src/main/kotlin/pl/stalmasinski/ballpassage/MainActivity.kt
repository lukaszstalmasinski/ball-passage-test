package pl.stalmasinski.ballpassage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import pl.stalmasinski.ballpassage.ui.calculator.CalculatorRoute
import pl.stalmasinski.ballpassage.ui.theme.BallPassageTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            BallPassageTheme {
                CalculatorRoute()
            }
        }
    }
}
