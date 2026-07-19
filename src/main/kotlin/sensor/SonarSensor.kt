package sensor

import environment.Environment
import geometry.Pose
import geometry.Ray
import geometry.Vector2
import kotlin.math.roundToInt

/**
 * Distance (world units) from the sensor to the nearest thing along its facing, up to [maxRange].
 * "Things" are the rectangular obstacles *and* the environment's outer walls (the edges of
 * `env.bounds`) — a ray cast from inside the bounds exits through the nearest wall.
 */
class SonarSensor(
    mountForward: Double,
    mountLateral: Double = 0.0,
    mountAngle: Double = 0.0,
    val maxRange: Double = 320.0,
) : Sensor<Double>("Sonar", mountForward, mountLateral, mountAngle) {

    override fun measure(env: Environment, sensorPose: Pose): Double {
        val ray = Ray(sensorPose.position, Vector2.fromAngle(sensorPose.heading))
        var nearest = maxRange
        for (obstacle in env.obstacles) {
            val d = ray.intersectRectangle(obstacle.bounds) ?: continue
            if (d in 0.0..nearest) nearest = d
        }
        // the outer walls: from inside env.bounds the ray's exit distance is the nearest wall
        ray.intersectRectangle(env.bounds)?.let { if (it in 0.0..nearest) nearest = it }
        // round to keep telemetry readable
        return nearest.roundToInt().toDouble()
    }
}
