package environment

import geometry.Pose
import geometry.Rectangle
import geometry.Vector2
import javafx.scene.paint.Color
import kotlin.math.roundToInt

/** Obstacles scattered across the field plus a red ball to find and touch. */
class ObstacleCourseEnvironment : AbstractEnvironment() {
    override val name = "Obstacle Course — find the red ball"

    override val bounds = Rectangle(0.0, 0.0, 880.0, 600.0)

    override val obstacles = listOf(
        Obstacle(Rectangle(200.0, 110.0, 60.0, 250.0)),
        Obstacle(Rectangle(400.0, 300.0, 240.0, 55.0)),
        Obstacle(Rectangle(520.0, 70.0, 60.0, 170.0)),
        Obstacle(Rectangle(300.0, 450.0, 190.0, 55.0)),
        Obstacle(Rectangle(680.0, 360.0, 55.0, 180.0)),
    )

    override val ball = Ball(Vector2(810.0, 120.0), 18.0, Color.web("#e5342b"))

    override fun startPose() = Pose(60.0, 540.0, Math.toRadians(-30.0))

    override fun objectiveStatus(robotPos: Vector2, robotRadius: Double): String {
        val b = ball
        val gap = robotPos.distanceTo(b.center) - robotRadius - b.radius
        return if (gap <= 0) "🎉 Ball reached!"
        else "Find & touch the red ball — ${gap.roundToInt()} units away"
    }
}
