import api.BallFinderProgram
import api.DefaultRobotApi
import command.CommandInvoker
import environment.ObstacleCourseEnvironment
import model.Robot
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Drives the real [BallFinderProgram] against the real [ObstacleCourseEnvironment] headlessly and
 * asserts it explores the arena, touches the red ball, and then stops (rather than orbiting it).
 * A virtual clock keeps the run deterministic and independent of wall-clock speed.
 */
class BallFinderIntegrationTest {

    @Test
    fun `ball finder explores, touches the ball, and stops`() {
        val env = ObstacleCourseEnvironment()
        val robot = Robot(env.startPose())
        robot.updateSensors(env)

        val dt = 1.0 / 60.0
        val api = DefaultRobotApi(CommandInvoker(), { robot }, { robot })
        // The app runs a fixed-timestep loop, so the program's odometry advances by this same dt.
        val program = BallFinderProgram(dtPerTick = dt)
        program.startProgram(api)

        val ball = env.ball!!
        var touchTick = -1
        for (tick in 0 until 9000) {   // up to 150 simulated seconds
            robot.step(dt, env)
            val gap = robot.pose.position.distanceTo(ball.center) - robot.radius - ball.radius
            if (gap <= 0.0) { touchTick = tick; break }
        }
        assertTrue(touchTick >= 0, "should touch the ball; final pos ${robot.pose.position}")

        // After touching, it should come to rest near the ball, not orbit it forever.
        repeat(200) { robot.step(dt, env) }
        val stopped = robot.leftTrackVelocity == 0.0 && robot.rightTrackVelocity == 0.0
        val gap = robot.pose.position.distanceTo(ball.center) - robot.radius - ball.radius
        assertTrue(stopped, "should stop after reaching the ball; velocities were " +
            "(${robot.leftTrackVelocity}, ${robot.rightTrackVelocity})")
        assertTrue(gap <= 0.0, "should still be touching the ball after stopping; gap=$gap")

        program.stopProgram(api)
    }
}
