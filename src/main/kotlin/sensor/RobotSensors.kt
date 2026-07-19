package sensor

import javafx.scene.paint.Color

/**
 * The robot's sensor suite, exposed so a program can subscribe to whichever sensors it needs.
 * Each sensor is a [observer.Subject] (via [Sensor]); subscribe an [observer.Observer] to receive
 * its reading every tick, and read [Sensor.reading] for the latest value.
 */
interface RobotSensors {
    val sonar: Sensor<Double>
    val vision: Sensor<Color>
    val temperature: Sensor<Double>
    val lineLeft: Sensor<Boolean>
    val lineCenter: Sensor<Boolean>
    val lineRight: Sensor<Boolean>
    val collision: Sensor<Boolean>
}
