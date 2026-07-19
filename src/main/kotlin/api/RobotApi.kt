package api

import command.Command
import command.RobotActuator
import sensor.RobotSensors

/**
 * The application interface students program the robot against:
 *  - subscribe to the [sensors] you need (the Observer pattern),
 *  - build commands against the [actuator] (the receiver),
 *  - [perform] those commands (they run through the invoker, so undo/redo works uniformly).
 *
 * Both the manual control panel and a [RobotProgram] are clients of this one interface.
 */
interface RobotApi {
    val sensors: RobotSensors
    val actuator: RobotActuator

    fun perform(command: Command)
    fun perform(commands: List<Command>)
    fun undo()
    fun redo()
}
