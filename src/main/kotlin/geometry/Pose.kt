package geometry

/** Position plus heading (radians). Heading 0 points along +x; it grows clockwise on screen (y-down). */
data class Pose(val x: Double, val y: Double, val heading: Double) {
    val position: Vector2 get() = Vector2(x, y)
}
