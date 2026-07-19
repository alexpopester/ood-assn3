package sensor

import environment.Environment
import geometry.Pose
import observer.AbstractSubject
import kotlin.math.cos
import kotlin.math.sin

/**
 * A sensor mounted on the robot. It is a [observer.Subject] of its own reading type [T]: each
 * simulation tick [update] re-measures the world and, when the reading changes, notifies
 * subscribed observers.
 *
 * Mount is expressed in the robot's local frame: [mountForward] along the heading, [mountLateral]
 * to the robot's left, plus an optional [mountAngle] rotation relative to the robot heading.
 */
abstract class Sensor<T>(
    val label: String,
    val mountForward: Double,
    val mountLateral: Double = 0.0,
    val mountAngle: Double = 0.0,
) : AbstractSubject<T>() {

    var reading: T? = null
        private set

    /** World-space pose of this sensor given the robot's [robotPose]. */
    fun worldPose(robotPose: Pose): Pose {
        val h = robotPose.heading
        // forward = (cos h, sin h); left = (sin h, -cos h) in screen (y-down) coordinates
        val px = robotPose.x + cos(h) * mountForward + sin(h) * mountLateral
        val py = robotPose.y + sin(h) * mountForward - cos(h) * mountLateral
        return Pose(px, py, h + mountAngle)
    }

    /**
     * Re-measure and notify subscribers with the current reading. Notification happens EVERY tick
     * (not only when the value changes) so that a subscribed program has a steady control loop —
     * the sensor stream doubles as the program's clock. This is what lets programs be driven purely
     * by their subscriptions (see [api.RobotProgram]).
     */
    fun update(env: Environment, robotPose: Pose) {
        val newReading = measure(env, worldPose(robotPose))
        reading = newReading
        notifyObservers(newReading)
    }

    protected abstract fun measure(env: Environment, sensorPose: Pose): T
}
