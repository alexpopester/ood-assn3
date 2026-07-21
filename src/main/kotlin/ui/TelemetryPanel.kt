package ui

import javafx.geometry.Insets
import javafx.scene.control.Label
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import model.Robot
import observer.LabelObserver
import observer.Observer

/**
 * A live readout of the sensor values — the *consumer* side of the Observer pattern.
 *
 * The layout (labels) is provided. Making it live is your job: in [bindTo] you subscribe an
 * observer to each sensor so the matching label updates when the sensor reports a reading.
 */
class TelemetryPanel : VBox(6.0) {

    private val title = styledLabel("Telemetry", 15.0, bold = true)
    private val sonar = valueLabel()
    private val temperature = valueLabel()
    private val vision = valueLabel()
    private val line = valueLabel()
    private val collision = valueLabel()

    // The three line sensors share one label, so their observers share this cached state.
    private var lineL = false
    private var lineC = false
    private var lineR = false
    private fun updateLine() {
        val fmt = { v: Boolean -> if (v) "■" else "□" }
        line.text = "${fmt(lineL)} ${fmt(lineC)} ${fmt(lineR)}"
    }

    // Every observer is stored as a field (not created inline) so that bindTo can unsubscribe the
    // previous robot's subscriptions before subscribing to a new one — no lingering subscriptions on
    // discarded robots, and a clean teardown path via [unbind].
    private val sonarObs = LabelObserver<Double>(sonar) { "%.1f".format(it) }
    private val temperatureObs = LabelObserver<Double>(temperature) { "%.1f°".format(it) }
    private val visionObs = LabelObserver<Color>(vision) { it.toString() }
    private val collisionObs = LabelObserver<Boolean>(collision) { if (it) "HIT" else "—" }
    private val lineLeftObs = Observer<Boolean> { lineL = it; updateLine() }
    private val lineCenterObs = Observer<Boolean> { lineC = it; updateLine() }
    private val lineRightObs = Observer<Boolean> { lineR = it; updateLine() }

    /** The robot we are currently subscribed to, so [bindTo]/[unbind] can detach cleanly. */
    private var bound: Robot? = null

    init {
        padding = Insets(12.0)
        prefWidth = 210.0
        style = "-fx-background-color: #14171c;"
        children.addAll(
            title,
            captioned("Sonar (distance)", sonar),
            captioned("Temperature", temperature),
            captioned("Vision (color)", vision),
            captioned("Line L / C / R", line),
            captioned("Collision", collision),
        )
    }

    /**
     * Subscribe our observers to the given robot's sensors so the labels update live. Called whenever
     * the robot is (re)created — on startup, environment change, and reset. Any previous robot is
     * [unbind]ed first, so subscriptions never accumulate on discarded robots.
     *
     * All observers are stored as fields (including the three line-sensor observers), which is what
     * makes clean unsubscription possible.
     */
    fun bindTo(robot: Robot) {
        unbind()
        robot.sonar.subscribe(sonarObs)
        robot.temperature.subscribe(temperatureObs)
        robot.vision.subscribe(visionObs)
        robot.collision.subscribe(collisionObs)
        // The three line sensors are combined into one label (L / C / R).
        robot.lineLeft.subscribe(lineLeftObs)
        robot.lineCenter.subscribe(lineCenterObs)
        robot.lineRight.subscribe(lineRightObs)
        bound = robot
    }

    /** Detach every observer from the currently bound robot's sensors. Safe to call when unbound. */
    fun unbind() {
        val robot = bound ?: return
        robot.sonar.unsubscribe(sonarObs)
        robot.temperature.unsubscribe(temperatureObs)
        robot.vision.unsubscribe(visionObs)
        robot.collision.unsubscribe(collisionObs)
        robot.lineLeft.unsubscribe(lineLeftObs)
        robot.lineCenter.unsubscribe(lineCenterObs)
        robot.lineRight.unsubscribe(lineRightObs)
        bound = null
    }

    private fun captioned(caption: String, value: Label): VBox =
        VBox(2.0, styledLabel(caption, 11.0, color = "#8b949e"), value)

    private fun valueLabel() = styledLabel("—", 18.0, bold = true)

    private fun styledLabel(text: String, size: Double, bold: Boolean = false, color: String = "#e6edf3"): Label =
        Label(text).apply {
            style = "-fx-font-size: ${size}px; -fx-text-fill: $color;" +
                if (bold) " -fx-font-weight: bold;" else ""
        }
}
