import api.DefaultRobotApi
import api.LineFollowerProgram
import command.CommandInvoker
import environment.LineMazeEnvironment
import geometry.Vector2
import model.Robot
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Drives the real [LineFollowerProgram] against the real [LineMazeEnvironment] headlessly and
 * asserts it reaches the end of the line (rather than falsely stopping at a corner).
 */
class LineFollowerIntegrationTest {

    @Test
    fun `line follower drives the maze to the end`() {
        val env = LineMazeEnvironment()
        val robot = Robot(env.startPose())
        robot.updateSensors(env)   // seed sensor readings before the program subscribes

        val api = DefaultRobotApi(CommandInvoker(), { robot }, { robot })
        val program = LineFollowerProgram()
        program.startProgram(api)  // subscribes; first sensor pass already primed the loop

        val goal = Vector2(790.0, 110.0)
        val dt = 1.0 / 60.0
        var closest = Double.MAX_VALUE
        var reached = false
        for (tick in 0 until 9000) {   // up to 150 simulated seconds
            robot.step(dt, env)
            val d = robot.pose.position.distanceTo(goal)
            closest = minOf(closest, d)
            if (d < 25.0) { reached = true; break }
        }
        program.stopProgram(api)

        assertTrue(reached, "should reach the goal; closest approach was ${"%.1f".format(closest)} at ${robot.pose.position}")
    }
}
