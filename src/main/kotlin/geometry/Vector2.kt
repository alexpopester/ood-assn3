package geometry

import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** An immutable 2D vector (also used for points) in world coordinates. */
data class Vector2(val x: Double, val y: Double) {
    operator fun plus(o: Vector2) = Vector2(x + o.x, y + o.y)
    operator fun minus(o: Vector2) = Vector2(x - o.x, y - o.y)
    operator fun times(s: Double) = Vector2(x * s, y * s)

    fun dot(o: Vector2) = x * o.x + y * o.y
    fun length() = hypot(x, y)
    fun distanceTo(o: Vector2) = hypot(x - o.x, y - o.y)

    fun normalized(): Vector2 {
        val len = length()
        return if (len == 0.0) Vector2(0.0, 0.0) else Vector2(x / len, y / len)
    }

    companion object {
        /** Unit (or scaled) vector pointing along [angleRad] (0 = +x, growing clockwise on screen). */
        fun fromAngle(angleRad: Double, length: Double = 1.0) =
            Vector2(cos(angleRad) * length, sin(angleRad) * length)
    }
}
