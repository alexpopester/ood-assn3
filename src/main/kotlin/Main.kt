import app.RobotSimulationApp
import javafx.application.Application

/**
 * Plain launcher. Kept separate from the [RobotSimulationApp] (an Application subclass) so the
 * non-modular run does not hit the "JavaFX runtime components are missing" error.
 */
fun main() {
    Application.launch(RobotSimulationApp::class.java)
}
