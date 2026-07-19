package ui

import javafx.geometry.Insets
import javafx.scene.control.Label
import javafx.scene.layout.VBox
import model.Robot
import observer.LabelObserver

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
     * Subscribe observers to the given robot's sensors so the labels update live. Called whenever
     * the robot is (re)created — on startup, environment change, and reset.
     *
     * TODO(student): subscribe an observer to each sensor and update the matching label, e.g.:
     * You can change the text of one of the Labels above by modifying the `text` property,
     * e.g: `vision.text = "The new text to display"`
     *
     * The labels (`sonar`, `temperature`, `vision`, `line`, `collision`) are ready to write to.
     * Until you do this, they stay "—". (This depends on your Observer pattern working — see
     * AbstractSubject.)
     */
    fun bindTo(robot: Robot) {
        robot.sonar.subscribe(LabelObserver(sonar) { "%.1f".format(it) })
        robot.temperature.subscribe(LabelObserver(temperature) { "%.1f°".format(it) })
        robot.vision.subscribe(LabelObserver(vision) { it.toString() })
        robot.collision.subscribe(LabelObserver(collision) { if (it) "HIT" else "—" })

        // Combine the three line sensors into one label (L / C / R)
        var l = false; var c = false; var r = false
        val fmt = { v: Boolean -> if (v) "■" else "□" }
        val updateLine = { line.text = "${fmt(l)} ${fmt(c)} ${fmt(r)}" }
        robot.lineLeft.subscribe   { l = it; updateLine() }
        robot.lineCenter.subscribe { c = it; updateLine() }
        robot.lineRight.subscribe  { r = it; updateLine() }
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
