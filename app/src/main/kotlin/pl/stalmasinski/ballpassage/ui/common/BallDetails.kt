package pl.stalmasinski.ballpassage.ui.common

import pl.stalmasinski.ballpassage.core.balls.Ball

/** Label and location joined for display, e.g. "K-12 · case 2"; empty when neither is set. */
fun Ball.details(): String = listOf(label, location).filter { it.isNotEmpty() }.joinToString(" · ")
