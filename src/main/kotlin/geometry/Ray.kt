package geometry

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** A ray from [origin] along [direction] (which should be normalized). Used by the sonar sensor. */
class Ray(val origin: Vector2, val direction: Vector2) {

    /** Distance from origin to the first intersection with the circle ([center], [radius]), or null. */
    fun intersectCircle(center: Vector2, radius: Double): Double? {
        val f = origin - center
        val b = 2 * f.dot(direction)
        val c = f.dot(f) - radius * radius
        val disc = b * b - 4 * c // a = direction·direction = 1 (normalized)
        if (disc < 0) return null
        val root = sqrt(disc)
        val t1 = (-b - root) / 2
        if (t1 >= 0) return t1
        val t2 = (-b + root) / 2
        return if (t2 >= 0) t2 else null
    }

    /**
     * Distance from the origin to the first intersection with [rect], or null if the ray misses.
     * Uses the slab method for ray/AABB intersection.
     */
    fun intersectRectangle(rect: Rectangle): Double? {
        var tMin = Double.NEGATIVE_INFINITY
        var tMax = Double.POSITIVE_INFINITY

        if (abs(direction.x) < 1e-9) {
            if (origin.x < rect.minX || origin.x > rect.maxX) return null
        } else {
            val t1 = (rect.minX - origin.x) / direction.x
            val t2 = (rect.maxX - origin.x) / direction.x
            tMin = max(tMin, min(t1, t2))
            tMax = min(tMax, max(t1, t2))
        }

        if (abs(direction.y) < 1e-9) {
            if (origin.y < rect.minY || origin.y > rect.maxY) return null
        } else {
            val t1 = (rect.minY - origin.y) / direction.y
            val t2 = (rect.maxY - origin.y) / direction.y
            tMin = max(tMin, min(t1, t2))
            tMax = min(tMax, max(t1, t2))
        }

        if (tMax < 0 || tMin > tMax) return null
        val t = if (tMin >= 0) tMin else tMax
        return if (t >= 0) t else null
    }
}
