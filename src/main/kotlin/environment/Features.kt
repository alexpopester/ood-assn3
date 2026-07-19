package environment

import geometry.Rectangle
import geometry.Vector2
import javafx.scene.paint.Color
import kotlin.math.exp

/** A rectangular obstacle the robot must avoid (and that the sonar can see). */
data class Obstacle(val bounds: Rectangle)

/** A straight floor line segment (used by the line maze). */
data class LineSegment(val a: Vector2, val b: Vector2) {
    /** Shortest distance from point [p] to this segment. */
    fun distanceTo(p: Vector2): Double {
        val ab = b - a
        val denom = ab.dot(ab)
        val t = if (denom == 0.0) 0.0 else ((p - a).dot(ab) / denom).coerceIn(0.0, 1.0)
        return (a + ab * t).distanceTo(p)
    }
}

/** A colored ball to find and touch (used by the obstacle course). */
data class Ball(val center: Vector2, val radius: Double, val color: Color = Color.RED)

/** A radial temperature field: hottest at [source], falling off to [ambient] with distance. */
class TemperatureField(
    val source: Vector2,
    val peak: Double = 100.0,
    val ambient: Double = 12.0,
    val sigma: Double = 220.0,
) {
    fun valueAt(p: Vector2): Double {
        val d = p.distanceTo(source)
        val falloff = exp(-(d * d) / (2 * sigma * sigma))
        return ambient + (peak - ambient) * falloff
    }
}
