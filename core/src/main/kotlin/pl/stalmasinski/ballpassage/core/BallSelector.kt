package pl.stalmasinski.ballpassage.core

/** Picks balls from the user's set that are suitable for a calculated tube. */
public object BallSelector {
    /**
     * Returns the balls that meet [requirement], smallest first. The first one is the recommendation,
     * because it is the closest to the diameter required by the standard.
     */
    public fun fittingBalls(requirement: BallRequirement, balls: Collection<Ball>): List<Ball> =
        balls.filter { requirement.fits(it.diameter) }.sortedBy { it.diameter }
}
