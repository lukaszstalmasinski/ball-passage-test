package pl.stalmasinski.ballpassage.core

import java.math.BigDecimal

/** A ball set as exchanged between devices: no ids, which are assigned on import. */
public data class SharedBallSet(
    val name: String,
    val balls: List<SharedBall>,
)

public data class SharedBall(
    val diameter: BigDecimal,
    val label: String = "",
    val location: String = "",
)

/**
 * Plain-text format for sharing a ball set by QR code or message:
 *
 * ```
 * BPT1
 * <set name>
 * 32<TAB>K-12<TAB>case 2
 * 35
 * ```
 *
 * The block may be surrounded by other text (e.g. a message header), so a pasted message
 * imports as well. Diameters always use a dot as the decimal separator.
 */
public object BallSetCodec {
    public const val MARKER: String = "BPT1"
    private const val SEPARATOR = '\t'

    public fun encode(set: BallSet): String = buildString {
        appendLine(MARKER)
        appendLine(BallSets.cleanText(set.name))
        set.balls.forEach { ball ->
            append(BallSets.normalize(ball.diameter).toPlainString())
            if (ball.label.isNotEmpty() || ball.location.isNotEmpty()) {
                append(SEPARATOR).append(BallSets.cleanText(ball.label))
                if (ball.location.isNotEmpty()) append(SEPARATOR).append(BallSets.cleanText(ball.location))
            }
            appendLine()
        }
    }.trimEnd()

    /** Returns the set found in [text], or null when there is none or it holds no valid ball. */
    public fun decode(text: String): SharedBallSet? {
        val lines = text.replace("\r\n", "\n").replace('\r', '\n').lines()
        val start = lines.indexOfFirst { it.trim() == MARKER }
        if (start < 0 || start + 1 >= lines.size) return null

        val name = BallSets.cleanText(lines[start + 1])
        val balls = lines.drop(start + 2)
            .asSequence()
            .map { it.trim() }
            .takeWhile { it.isNotEmpty() }
            .mapNotNull(::decodeBall)
            .distinctBy { BallSets.normalize(it.diameter) }
            .toList()
        return if (balls.isEmpty()) null else SharedBallSet(name, balls)
    }

    private fun decodeBall(line: String): SharedBall? {
        val parts = line.split(SEPARATOR)
        val diameter = parts[0].trim().replace(',', '.').toBigDecimalOrNull()
            ?.takeIf { it.signum() > 0 }
            ?: return null
        return SharedBall(
            diameter = BallSets.normalize(diameter),
            label = BallSets.cleanText(parts.getOrNull(1).orEmpty()),
            location = BallSets.cleanText(parts.getOrNull(2).orEmpty()),
        )
    }
}
