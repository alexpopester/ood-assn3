package command

/**
 * The narrow receiver interface that commands act upon. The [model.Robot] implements it, but
 * commands (and the code that builds them) only see this small surface — the skid-steer robot's
 * two track velocities. Reading the current velocities lets a command capture state for undo.
 */
interface RobotActuator {
    val leftTrackVelocity: Double
    val rightTrackVelocity: Double
    fun setTrackVelocities(left: Double, right: Double)
}
