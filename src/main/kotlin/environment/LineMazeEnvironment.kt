package environment

import geometry.Pose
import geometry.Rectangle
import geometry.Vector2
import javafx.scene.paint.Color
import kotlin.math.roundToInt

/** A single winding line the robot should follow from start to end. */
class LineMazeEnvironment : AbstractEnvironment() {
    override val name = "Line Maze — follow the line"

    override val bounds = Rectangle(0.0, 0.0, 880.0, 600.0)

    override val obstacles = emptyList<Obstacle>()

    override val lineWidth = 16.0
    override val floorColor: Color = Color.web("#1c2b33")
    override val lineColor: Color = Color.web("#ffd23f")

    private val waypoints = listOf(
        Vector2(90.0, 90.0),
        Vector2(90.0, 500.0),
        Vector2(300.0, 500.0),
        Vector2(300.0, 190.0),
        Vector2(560.0, 190.0),
        Vector2(560.0, 520.0),
        Vector2(790.0, 520.0),
        Vector2(790.0, 110.0),
    )

    override val lines = waypoints.zipWithNext { a, b -> LineSegment(a, b) }

    private val goal = waypoints.last()

    // Start on the line, heading down (+y) toward the first turn.
    override fun startPose() = Pose(90.0, 90.0, Math.toRadians(90.0))

    override fun objectiveStatus(robotPos: Vector2, robotRadius: Double): String {
        val d = robotPos.distanceTo(goal)
        return if (d < 25) "🎉 Reached the end of the line!"
        else "Follow the line to the end — ${d.roundToInt()} units to go"
    }
}
