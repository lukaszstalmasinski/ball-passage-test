package pl.stalmasinski.ballpassage.core

import java.math.BigDecimal

/**
 * A steel ball available in the workshop.
 *
 * @property label marking used to identify the ball, e.g. "K-12"; empty when not set.
 * @property location where the ball is kept, e.g. "case 2"; empty when not set.
 */
public data class Ball(
    val id: String,
    val diameter: BigDecimal,
    val label: String = "",
    val location: String = "",
)

/** A named group of balls, e.g. one per workshop or case. An empty [name] means the default set. */
public data class BallSet(
    val id: String,
    val name: String,
    val balls: List<Ball> = emptyList(),
)

/** All ball sets of the user; [activeSetId] is the one the calculator recommends from. */
public data class BallSets(
    val sets: List<BallSet>,
    val activeSetId: String,
) {
    init {
        require(sets.isNotEmpty()) { "There is always at least one ball set." }
    }

    val active: BallSet get() = sets.firstOrNull { it.id == activeSetId } ?: sets.first()

    public fun set(id: String): BallSet? = sets.firstOrNull { it.id == id }

    /** Adds balls to a set, skipping diameters that are already in it. */
    public fun addBalls(setId: String, diameters: List<BigDecimal>, newId: () -> String): BallSets =
        updateSet(setId) { set ->
            val added = diameters
                .map(::normalize)
                .distinct()
                .filter { diameter -> set.balls.none { it.diameter.compareTo(diameter) == 0 } }
                .map { Ball(id = newId(), diameter = it) }
            set.copy(balls = (set.balls + added).sortedBy { it.diameter })
        }

    /** Adds a ball object as it is, e.g. to undo a removal. Ignored when its diameter is already in the set. */
    public fun restoreBall(setId: String, ball: Ball): BallSets =
        updateSet(setId) { set ->
            if (set.balls.any { it.diameter.compareTo(ball.diameter) == 0 }) {
                set
            } else {
                set.copy(balls = (set.balls + ball).sortedBy { it.diameter })
            }
        }

    public fun removeBall(setId: String, ballId: String): BallSets =
        updateSet(setId) { set -> set.copy(balls = set.balls.filterNot { it.id == ballId }) }

    public fun updateBallDetails(setId: String, ballId: String, label: String, location: String): BallSets =
        updateSet(setId) { set ->
            set.copy(
                balls = set.balls.map {
                    if (it.id == ballId) it.copy(label = cleanText(label), location = cleanText(location)) else it
                },
            )
        }

    /** Creates an empty set and makes it active. */
    public fun createSet(name: String, id: String): BallSets =
        copy(sets = sets + BallSet(id = id, name = uniqueName(cleanText(name))), activeSetId = id)

    public fun renameSet(setId: String, name: String): BallSets =
        updateSet(setId) { set -> set.copy(name = uniqueName(cleanText(name), exceptId = setId)) }

    /** Deletes a set; the last remaining set cannot be deleted. */
    public fun deleteSet(setId: String): BallSets {
        if (sets.size <= 1) return this
        val remaining = sets.filterNot { it.id == setId }
        return copy(
            sets = remaining,
            activeSetId = if (activeSetId == setId) remaining.first().id else activeSetId,
        )
    }

    public fun selectSet(setId: String): BallSets =
        if (set(setId) == null) this else copy(activeSetId = setId)

    /** Adds a shared set as a new, active set with new ids; the name gets a suffix if it is taken. */
    public fun importSet(shared: SharedBallSet, newId: () -> String): BallSets {
        val setId = newId()
        val balls = shared.balls
            .distinctBy { normalize(it.diameter) }
            .map { Ball(newId(), normalize(it.diameter), cleanText(it.label), cleanText(it.location)) }
            .sortedBy { it.diameter }
        return copy(
            sets = sets + BallSet(setId, uniqueName(cleanText(shared.name)), balls),
            activeSetId = setId,
        )
    }

    private fun updateSet(setId: String, transform: (BallSet) -> BallSet): BallSets =
        copy(sets = sets.map { if (it.id == setId) transform(it) else it })

    private fun uniqueName(name: String, exceptId: String? = null): String {
        if (name.isEmpty()) return name
        val taken = sets.filter { it.id != exceptId }.map { it.name.lowercase() }.toSet()
        if (name.lowercase() !in taken) return name
        return generateSequence(2) { it + 1 }.map { "$name ($it)" }.first { it.lowercase() !in taken }
    }

    public companion object {
        public const val MAX_TEXT_LENGTH: Int = 40

        public fun initial(id: String): BallSets = BallSets(listOf(BallSet(id = id, name = "")), activeSetId = id)

        /** 32.0 and 32 are the same ball; stores numbers without trailing zeros or exponent. */
        public fun normalize(value: BigDecimal): BigDecimal =
            value.stripTrailingZeros().let { if (it.scale() < 0) it.setScale(0) else it }

        /** Single line, trimmed and limited in length, so that it can be shared and displayed safely. */
        public fun cleanText(text: String): String =
            text.replace(Regex("\\s+"), " ").trim().take(MAX_TEXT_LENGTH)
    }
}
