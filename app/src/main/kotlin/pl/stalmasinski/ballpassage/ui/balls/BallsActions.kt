package pl.stalmasinski.ballpassage.ui.balls

import pl.stalmasinski.ballpassage.core.balls.Ball
import pl.stalmasinski.ballpassage.core.balls.BallSet
import pl.stalmasinski.ballpassage.core.balls.SharedBallSet

/**
 * What the Balls screen can do. [BallsViewModel] implements it; the screen depends only on this
 * interface, so that it can be previewed and tested with a fake.
 */
interface BallsActions {
    fun onInputChange(text: String)

    /** Adds the typed ball; returns false when the input is not accepted. */
    fun add(): Boolean

    fun addRange(from: String, to: String, step: String): RangeResult

    fun remove(ball: Ball)

    /** Puts back a ball removed by mistake, with its label and location. */
    fun restore(ball: Ball)

    fun updateDetails(ball: Ball, label: String, location: String)

    fun createSet(name: String)

    fun renameSet(name: String)

    fun deleteSet()

    fun selectSet(set: BallSet)

    /** The active set in the sharing format, for a QR code or a message, under its displayed [name]. */
    fun exportText(name: String): String

    fun parseImport(text: String): SharedBallSet?

    fun importSet(shared: SharedBallSet)
}
