package pl.stalmasinski.ballpassage.ui.balls

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import pl.stalmasinski.ballpassage.core.balls.SharedBall
import pl.stalmasinski.ballpassage.core.balls.SharedBallSet
import java.math.BigDecimal

/**
 * Keeps a ball set waiting for the import confirmation across rotation and process death:
 * the name, then diameter, label and location of each ball, as strings.
 */
internal val SharedBallSetSaver: Saver<SharedBallSet?, Any> = listSaver(
    save = { set ->
        if (set == null) {
            emptyList()
        } else {
            listOf(set.name) + set.balls.flatMap { listOf(it.diameter.toPlainString(), it.label, it.location) }
        }
    },
    restore = { values ->
        if (values.isEmpty()) {
            null
        } else {
            SharedBallSet(
                name = values.first(),
                balls = values.drop(1).chunked(3).map { (diameter, label, location) ->
                    SharedBall(BigDecimal(diameter), label, location)
                },
            )
        }
    },
)
