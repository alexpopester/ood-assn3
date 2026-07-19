package environment

import geometry.Pose
import geometry.Rectangle
import geometry.Vector2
import kotlin.math.roundToInt

/** A heat field with a single hot source; drive up the gradient to the hot spot. */
class TemperatureGradientEnvironment : AbstractEnvironment() {
    override val name = "Temperature Gradient — find the hot spot"

    override val bounds = Rectangle(0.0, 0.0, 880.0, 600.0)

    // A couple of obstacles as scenery, kept off the start→source diagonal so gradient ascent
    // isn't trapped behind a wall.
    override val obstacles = listOf(
        Obstacle(Rectangle(250.0, 90.0, 60.0, 150.0)),
        Obstacle(Rectangle(470.0, 430.0, 190.0, 55.0)),
    )

    // Broad field so there is a usable gradient across the whole arena (gradient ascent needs one).
    override val temperatureField = TemperatureField(
        source = Vector2(730.0, 150.0),
        peak = 110.0,
        ambient = 15.0,
        sigma = 520.0,
    )

    override fun startPose() = Pose(70.0, 520.0, Math.toRadians(-40.0))

    override fun objectiveStatus(robotPos: Vector2, robotRadius: Double): String {
        val t = temperatureAt(robotPos)
        return if (t >= 92) "🔥 At the hot spot! (${t.roundToInt()}°)"
        else "Warmer is closer — current temperature ${t.roundToInt()}°"
    }
}
