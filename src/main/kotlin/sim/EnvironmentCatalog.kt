package sim

import environment.Environment
import environment.LineMazeEnvironment
import environment.ObstacleCourseEnvironment
import environment.TemperatureGradientEnvironment

/** The set of worlds offered in the environment dropdown. */
object EnvironmentCatalog {
    fun all(): List<Environment> = listOf(
        ObstacleCourseEnvironment(),
        LineMazeEnvironment(),
        TemperatureGradientEnvironment(),
    )
}
