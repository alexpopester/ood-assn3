package model

import command.RobotActuator
import environment.Environment
import geometry.Pose
import geometry.Vector2
import sensor.CollisionSensor
import sensor.LineSensor
import sensor.RobotSensors
import sensor.Sensor
import sensor.SonarSensor
import sensor.TemperatureSensor
import sensor.VisionSensor
import kotlin.math.cos
import kotlin.math.sin

/**
 * The skid-steer (differential-drive) robot. It is the Command pattern's *receiver*
 * (via [RobotActuator]) and owns the sensor suite (each sensor is an Observer Subject,
 * exposed through [RobotSensors]).
 *
 * Modeled as a disc of [radius]; the two tracks are [trackWidth] apart.
 */
class Robot(
    startPose: Pose,
    val radius: Double = 16.0,
    val trackWidth: Double = 26.0,
    val maxTrackSpeed: Double = 150.0,
) : RobotActuator, RobotSensors {

    var pose: Pose = startPose
        private set

    override var leftTrackVelocity: Double = 0.0
        private set
    override var rightTrackVelocity: Double = 0.0
        private set

    /** True when the last [step] had its translation blocked by an obstacle or wall. */
    var isColliding: Boolean = false
        private set

    // Standard sensor suite. Line sensors are spread across the front for line-following.
    override val sonar = SonarSensor(mountForward = radius, maxRange = 320.0)
    override val vision = VisionSensor(mountForward = radius)
    override val temperature = TemperatureSensor(mountForward = 0.0)
    override val lineLeft = LineSensor(mountForward = radius, mountLateral = 8.0)
    override val lineCenter = LineSensor(mountForward = radius, mountLateral = 0.0)
    override val lineRight = LineSensor(mountForward = radius, mountLateral = -8.0)
    override val collision = CollisionSensor { isColliding }

    val sensors: List<Sensor<*>> =
        listOf(sonar, vision, temperature, lineLeft, lineCenter, lineRight, collision)

    override fun setTrackVelocities(left: Double, right: Double) {
        leftTrackVelocity = left.coerceIn(-maxTrackSpeed, maxTrackSpeed)
        rightTrackVelocity = right.coerceIn(-maxTrackSpeed, maxTrackSpeed)
    }

    fun stop() = setTrackVelocities(0.0, 0.0)

    /** Advance the robot [dt] seconds using skid-steer kinematics, then re-read the sensors. */
    fun step(dt: Double, env: Environment) {
        val v = (leftTrackVelocity + rightTrackVelocity) / 2.0
        val omega = (rightTrackVelocity - leftTrackVelocity) / trackWidth

        val newHeading = pose.heading + omega * dt
        val candidate = Vector2(
            pose.x + v * cos(pose.heading) * dt,
            pose.y + v * sin(pose.heading) * dt,
        )

        // Rotation always applies; translation is rejected if it would collide.
        isColliding = collides(candidate, env)
        val nextPos = if (isColliding) pose.position else candidate
        pose = Pose(nextPos.x, nextPos.y, newHeading)

        updateSensors(env)
    }

    fun updateSensors(env: Environment) = sensors.forEach { it.update(env, pose) }

    private fun collides(center: Vector2, env: Environment): Boolean {
        val b = env.bounds
        val outOfBounds = center.x - radius < b.minX || center.x + radius > b.maxX ||
            center.y - radius < b.minY || center.y + radius > b.maxY
        if (outOfBounds) return true
        return env.obstacles.any { it.bounds.intersectsCircle(center, radius) }
    }
}
