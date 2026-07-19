package api

import command.Command
import command.CommandInvoker
import command.RobotActuator
import sensor.RobotSensors

/**
 * Provided implementation of [RobotApi]: a thin facade over the [CommandInvoker].
 *
 * [actuatorProvider] and [sensorsProvider] are looked up on every access so the API always targets
 * the *current* robot (a fresh robot is created when the environment is reloaded or reset).
 */
class DefaultRobotApi(
    private val invoker: CommandInvoker,
    private val actuatorProvider: () -> RobotActuator,
    private val sensorsProvider: () -> RobotSensors,
) : RobotApi {

    override val actuator: RobotActuator get() = actuatorProvider()
    override val sensors: RobotSensors get() = sensorsProvider()

    override fun perform(command: Command) = invoker.run(command)
    override fun perform(commands: List<Command>) = commands.forEach { invoker.run(it) }
    override fun undo() = invoker.undo()
    override fun redo() = invoker.redo()
}
