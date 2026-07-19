package sensor

import environment.Environment
import geometry.Pose
import geometry.Ray
import geometry.Vector2
import javafx.scene.paint.Color

/**
 * A forward-looking color "camera": casts a ray from the sensor and reports the color of the
 * nearest thing it hits within [range] — the ball, an obstacle, an outer wall, or the floor if
 * nothing is close. This lets a program spot the ball at a distance (telemetry shows RED when the
 * ball is ahead) and also tell when it is looking straight at a wall.
 */
class VisionSensor(
    mountForward: Double,
    mountLateral: Double = 0.0,
    mountAngle: Double = 0.0,
    val range: Double = 340.0,
) : Sensor<Color>("Vision", mountForward, mountLateral, mountAngle) {

    override fun measure(env: Environment, sensorPose: Pose): Color {
        val ray = Ray(sensorPose.position, Vector2.fromAngle(sensorPose.heading))

        val ballHit = env.ball?.let { ray.intersectCircle(it.center, it.radius) }
        var obstacleHit = Double.POSITIVE_INFINITY
        for (o in env.obstacles) {
            val d = ray.intersectRectangle(o.bounds) ?: continue
            if (d < obstacleHit) obstacleHit = d
        }
        // outer walls: exit distance from inside env.bounds
        val wallHit = ray.intersectRectangle(env.bounds) ?: Double.POSITIVE_INFINITY

        val nearestSolid = minOf(obstacleHit, wallHit)
        val ball = env.ball
        if (ball != null && ballHit != null && ballHit <= range && ballHit <= nearestSolid) {
            return ball.color
        }
        if (obstacleHit <= range && obstacleHit <= wallHit) return OBSTACLE_COLOR
        if (wallHit <= range) return WALL_COLOR

        // nothing solid ahead — report what is under the look-ahead point (floor or a line)
        return env.colorAt(sensorPose.position + Vector2.fromAngle(sensorPose.heading, 12.0))
    }

    companion object {
        val OBSTACLE_COLOR: Color = Color.web("#4a4f57")
        val WALL_COLOR: Color = Color.web("#6b5b4f")
    }
}
