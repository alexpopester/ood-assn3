package api

/**
 * A program that drives the robot. Students implement this.
 *
 * The program is an Observer: in [startProgram] it subscribes to whichever [RobotApi.sensors] it
 * needs and issues commands (via the [RobotApi]) in response to their readings; in [stopProgram]
 * it unsubscribes and typically stops the robot. There is no per-tick callback — a subscribed
 * sensor notifies every tick, so the sensor stream is the program's control loop.
 *
 * Register an instance with a [ProgramRegistry] to make it selectable in the UI's program dropdown.
 */
interface RobotProgram {
    /** Shown in the "run program" dropdown. */
    val name: String

    /** Called when the program is launched: subscribe to sensors and start driving. */
    fun startProgram(robot: RobotApi)

    /** Called when the program is stopped: unsubscribe observers and stop the robot. */
    fun stopProgram(robot: RobotApi)
}
