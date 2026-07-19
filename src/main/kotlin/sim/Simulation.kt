package sim

import environment.Environment
import geometry.Pose
import model.Robot

/** Owns the current [environment] and [robot] and advances them each tick. */
class Simulation(environment: Environment) {

    var environment: Environment = environment
        private set

    var robot: Robot = Robot(environment.startPose())
        private set

    init {
        robot.updateSensors(this.environment)
    }

    /** Advance the physics by [dt] seconds. */
    fun step(dt: Double) {
        robot.step(dt, environment)
    }

    /** Replace the world and drop in a fresh robot at its start pose. */
    fun loadEnvironment(newEnvironment: Environment) {
        environment = newEnvironment
        robot = Robot(newEnvironment.startPose())
        robot.updateSensors(newEnvironment)
    }

    /** Reset the robot to the current environment's start pose. */
    fun resetRobot() {
        robot = Robot(environment.startPose())
        robot.updateSensors(environment)
    }

    /** Human-readable objective/progress for the current robot. */
    fun objectiveStatus(): String =
        environment.objectiveStatus(robot.pose.position, robot.radius)

    fun startPose(): Pose = environment.startPose()
}
