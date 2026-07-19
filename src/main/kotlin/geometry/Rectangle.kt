package geometry

/** Axis-aligned rectangle defined by its top-left corner ([x], [y]) plus [width] and [height]. */
data class Rectangle(val x: Double, val y: Double, val width: Double, val height: Double) {
    val minX get() = x
    val minY get() = y
    val maxX get() = x + width
    val maxY get() = y + height
    val centerX get() = x + width / 2.0
    val centerY get() = y + height / 2.0

    fun contains(p: Vector2) = p.x in minX..maxX && p.y in minY..maxY

    /** The point on (or inside) this rectangle nearest to [p]. */
    fun closestPointTo(p: Vector2) =
        Vector2(p.x.coerceIn(minX, maxX), p.y.coerceIn(minY, maxY))

    /** True when a circle of [radius] centered at [center] overlaps this rectangle. */
    fun intersectsCircle(center: Vector2, radius: Double) =
        closestPointTo(center).distanceTo(center) <= radius
}
