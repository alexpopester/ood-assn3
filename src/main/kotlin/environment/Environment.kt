package environment

import geometry.Pose
import geometry.Rectangle
import geometry.Vector2
import javafx.scene.paint.Color

/**
 * A world the robot lives in. It owns the obstacles and the optional features (lines, ball,
 * temperature field) and answers the queries the sensors make.
 */
interface Environment {
    val name: String
    val bounds: Rectangle
    val obstacles: List<Obstacle>
    val lines: List<LineSegment>
    val ball: Ball?
    val temperatureField: TemperatureField?
    val lineWidth: Double
    val floorColor: Color
    val lineColor: Color

    /** Temperature at world point [p] (for the temperature sensor / heat-map rendering). */
    fun temperatureAt(p: Vector2): Double

    /** Color the robot's vision sensor would see at world point [p]. */
    fun colorAt(p: Vector2): Color

    /** True when world point [p] lies on a floor line. */
    fun isOnLineAt(p: Vector2): Boolean

    /** Human-readable objective/progress for a robot at [robotPos] with [robotRadius]. */
    fun objectiveStatus(robotPos: Vector2, robotRadius: Double): String

    /** Where the robot starts in this world. */
    fun startPose(): Pose
}

/** Shared defaults so each concrete world only specifies what makes it distinctive. */
abstract class AbstractEnvironment : Environment {
    override val lines: List<LineSegment> get() = emptyList()
    override val ball: Ball? get() = null
    override val temperatureField: TemperatureField? get() = null
    override val lineWidth: Double get() = 12.0
    override val floorColor: Color get() = Color.web("#23262b")
    override val lineColor: Color get() = Color.web("#f4f4f4")

    override fun temperatureAt(p: Vector2): Double = temperatureField?.valueAt(p) ?: 20.0

    override fun isOnLineAt(p: Vector2): Boolean = lines.any { it.distanceTo(p) <= lineWidth / 2 }

    override fun colorAt(p: Vector2): Color {
        ball?.let { if (p.distanceTo(it.center) <= it.radius) return it.color }
        if (isOnLineAt(p)) return lineColor
        return floorColor
    }

    override fun startPose(): Pose = Pose(bounds.centerX, bounds.centerY, 0.0)

    override fun objectiveStatus(robotPos: Vector2, robotRadius: Double): String = ""
}
