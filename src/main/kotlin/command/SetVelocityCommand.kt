package command

/** Sets both track velocities; captures the previous values so [undo] can restore them. */
class SetVelocityCommand(
    private val actuator: RobotActuator,
    private val left: Double,
    private val right: Double,
) : Command {
    private var prevLeft = 0.0
    private var prevRight = 0.0

    override fun execute() {
        prevLeft  = actuator.leftTrackVelocity
        prevRight = actuator.rightTrackVelocity
        actuator.setTrackVelocities(left, right)
    }

    override fun undo() {
        actuator.setTrackVelocities(prevLeft, prevRight)
    }
}
