package sensor

import environment.Environment
import geometry.Pose

/** Reports whether the sensor is currently over a floor line (for line-following). */
class LineSensor(
    mountForward: Double,
    mountLateral: Double = 0.0,
    mountAngle: Double = 0.0,
) : Sensor<Boolean>("Line", mountForward, mountLateral, mountAngle) {

    override fun measure(env: Environment, sensorPose: Pose): Boolean =
        env.isOnLineAt(sensorPose.position)
}
