import environment.AbstractEnvironment
import environment.LineSegment
import environment.Obstacle
import environment.TemperatureField
import geometry.Pose
import geometry.Rectangle
import geometry.Vector2
import model.Robot
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SensorTest {

    private class SonarWorld : AbstractEnvironment() {
        override val name = "sonar"
        override val bounds = Rectangle(0.0, 0.0, 2000.0, 2000.0)
        override val obstacles = listOf(Obstacle(Rectangle(300.0, 60.0, 40.0, 200.0)))
        override fun startPose() = Pose(100.0, 100.0, 0.0)
    }

    @Test
    fun `sonar reports distance to an obstacle straight ahead`() {
        val robot = Robot(Pose(100.0, 100.0, 0.0), radius = 16.0)
        robot.updateSensors(SonarWorld())
        // sensor sits at x=116 (center + radius); wall face at x=300 → ~184 units
        val d = robot.sonar.reading!!
        assertTrue(abs(d - 184.0) < 2.0, "sonar=$d")
    }

    private class EmptyRoom : AbstractEnvironment() {
        override val name = "room"
        override val bounds = Rectangle(0.0, 0.0, 500.0, 500.0)
        override val obstacles = emptyList<Obstacle>()
        override fun startPose() = Pose(100.0, 250.0, Math.PI) // facing -x, toward the left wall
    }

    @Test
    fun `sonar senses the outer wall when there is no obstacle`() {
        val robot = Robot(EmptyRoom().startPose(), radius = 16.0)
        robot.updateSensors(EmptyRoom())
        // sensor at x = 100 - 16 = 84, facing the wall at x = 0 → ~84 units
        val d = robot.sonar.reading!!
        assertTrue(abs(d - 84.0) < 2.0, "sonar=$d")
    }

    @Test
    fun `collision sensor fires while the robot drives into a wall`() {
        val env = EmptyRoom()
        val robot = Robot(env.startPose(), radius = 16.0) // (100,250) facing the -x wall
        robot.updateSensors(env)
        assertEquals(false, robot.collision.reading, "should start clear")

        robot.setTrackVelocities(100.0, 100.0) // drive into the wall
        repeat(90) { robot.step(1.0 / 60.0, env) }
        assertEquals(true, robot.collision.reading, "should be blocked at the wall")
    }

    private class HeatWorld : AbstractEnvironment() {
        override val name = "heat"
        override val bounds = Rectangle(0.0, 0.0, 2000.0, 2000.0)
        override val obstacles = emptyList<Obstacle>()
        override val temperatureField = TemperatureField(Vector2(500.0, 500.0), peak = 100.0, ambient = 10.0, sigma = 100.0)
        override fun startPose() = Pose(500.0, 500.0, 0.0)
    }

    @Test
    fun `temperature peaks at the source and cools with distance`() {
        val env = HeatWorld()
        assertTrue(env.temperatureAt(Vector2(500.0, 500.0)) >= 99.0)
        assertTrue(env.temperatureAt(Vector2(900.0, 500.0)) < 20.0)
    }

    private class LineWorld : AbstractEnvironment() {
        override val name = "line"
        override val bounds = Rectangle(0.0, 0.0, 2000.0, 2000.0)
        override val obstacles = emptyList<Obstacle>()
        override val lines = listOf(LineSegment(Vector2(100.0, 100.0), Vector2(400.0, 100.0)))
        override val lineWidth = 12.0
        override fun startPose() = Pose(100.0, 100.0, 0.0)
    }

    @Test
    fun `line sensor detects on vs off the line`() {
        val env = LineWorld()
        assertTrue(env.isOnLineAt(Vector2(250.0, 100.0)))       // on the line
        assertEquals(false, env.isOnLineAt(Vector2(250.0, 200.0))) // well off it
    }
}
