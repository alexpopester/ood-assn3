package sensor

import environment.Environment
import geometry.Pose
import kotlin.math.roundToInt

/** Reads the environment's temperature field at the sensor's location. */
class TemperatureSensor(
    mountForward: Double,
    mountLateral: Double = 0.0,
    mountAngle: Double = 0.0,
) : Sensor<Double>("Temperature", mountForward, mountLateral, mountAngle) {

    // Round to 1 decimal: stable enough for "changed?" / telemetry, but preserves the gradient
    // signal a temperature-seeking program relies on.
    override fun measure(env: Environment, sensorPose: Pose): Double =
        (env.temperatureAt(sensorPose.position) * 10).roundToInt() / 10.0
}
