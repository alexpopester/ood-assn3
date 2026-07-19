import environment.AbstractEnvironment
import environment.Obstacle
import geometry.Pose
import geometry.Rectangle
import model.Robot
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/** An empty, large world for isolating kinematics. */
private class EmptyWorld(private val obs: List<Obstacle> = emptyList()) : AbstractEnvironment() {
    override val name = "test"
    override val bounds = Rectangle(0.0, 0.0, 2000.0, 2000.0)
    override val obstacles = obs
    override fun startPose() = Pose(1000.0, 1000.0, 0.0)
}

class KinematicsTest {

    @Test
    fun `equal track speeds drive in a straight line`() {
        val robot = Robot(Pose(100.0, 100.0, 0.0)) // heading 0 = +x
        robot.setTrackVelocities(100.0, 100.0)
        val env = EmptyWorld()
        repeat(60) { robot.step(1.0 / 60.0, env) }

        // ~100 units/sec for 1 sec along +x, y unchanged, heading unchanged
        assertTrue(abs(robot.pose.x - 200.0) < 1.0, "x=${robot.pose.x}")
        assertTrue(abs(robot.pose.y - 100.0) < 1e-6, "y=${robot.pose.y}")
        assertTrue(abs(robot.pose.heading) < 1e-6, "heading=${robot.pose.heading}")
    }

    @Test
    fun `opposite track speeds rotate in place`() {
        val robot = Robot(Pose(100.0, 100.0, 0.0))
        robot.setTrackVelocities(-50.0, 50.0)
        val env = EmptyWorld()
        repeat(60) { robot.step(1.0 / 60.0, env) }

        // position barely moves, heading changes
        assertTrue(robot.pose.position.distanceTo(geometry.Vector2(100.0, 100.0)) < 1.0)
        assertTrue(abs(robot.pose.heading) > 0.5, "heading=${robot.pose.heading}")
    }

    @Test
    fun `an obstacle blocks forward translation`() {
        val robot = Robot(Pose(100.0, 100.0, 0.0), radius = 16.0)
        // wall just ahead along +x
        val env = EmptyWorld(listOf(Obstacle(Rectangle(140.0, 60.0, 40.0, 80.0))))
        robot.setTrackVelocities(120.0, 120.0)
        repeat(120) { robot.step(1.0 / 60.0, env) }

        // robot should be stopped left of the wall (x + radius <= 140)
        assertTrue(robot.pose.x + robot.radius <= 140.0 + 0.5, "x=${robot.pose.x}")
    }
}
