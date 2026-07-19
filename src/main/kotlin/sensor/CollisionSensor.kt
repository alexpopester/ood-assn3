package sensor

import environment.Environment
import geometry.Pose

/**
 * A bump sensor: reports whether the robot's motion is currently blocked by an obstacle or a wall
 * (i.e. the robot tried to move into something this tick and couldn't).
 *
 * Unlike the ranged sensors, collision is a property of the whole robot body, so the robot supplies
 * its contact state via [isCollidingProvider] rather than it being measured from a point in the world.
 */
class CollisionSensor(
    private val isCollidingProvider: () -> Boolean,
) : Sensor<Boolean>("Collision", mountForward = 0.0) {

    override fun measure(env: Environment, sensorPose: Pose): Boolean = isCollidingProvider()
}
