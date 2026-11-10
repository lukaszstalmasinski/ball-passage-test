package pl.stalmasinski.ballpassage.core

import pl.stalmasinski.ballpassage.core.balls.Ball
import pl.stalmasinski.ballpassage.core.calculation.BallPassageCalculator
import pl.stalmasinski.ballpassage.core.calculation.CalculationResult
import pl.stalmasinski.ballpassage.core.calculation.TubeDimensions
import java.math.BigDecimal

// Builders for tests of core and app, written as the numbers appear in the standard and the UI.

/** A tube from outer diameter, wall thickness and bend radius in millimetres, e.g. tube("48.3", "5", "70"). */
public fun tube(outer: String, wall: String, radius: String): TubeDimensions =
    TubeDimensions(BigDecimal(outer), BigDecimal(wall), BigDecimal(radius))

/** The calculation result for [tube]. */
public fun calculate(outer: String, wall: String, radius: String): CalculationResult =
    BallPassageCalculator.calculate(tube(outer, wall, radius))

/** A ball whose id is its diameter unless given. */
public fun ball(diameter: String, label: String = "", location: String = "", id: String = diameter): Ball =
    Ball(id = id, diameter = BigDecimal(diameter), label = label, location = location)
