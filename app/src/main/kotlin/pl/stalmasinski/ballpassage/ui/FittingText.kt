package pl.stalmasinski.ballpassage.ui

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

/**
 * Text for narrow, fixed-width slots (table cells, navigation labels): instead of breaking a
 * value such as "20–29" in the middle at large system font sizes, it shrinks to fit its lines.
 */
@Composable
fun FittingText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    fontStyle: FontStyle? = null,
    textAlign: TextAlign = TextAlign.Center,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.merge(color = color, fontWeight = fontWeight, fontStyle = fontStyle, textAlign = textAlign),
        softWrap = false,
        maxLines = text.count { it == '\n' } + 1,
        autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = style.fontSize),
    )
}
