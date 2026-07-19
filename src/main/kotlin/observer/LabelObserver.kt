package observer

import javafx.scene.control.Label

/** Concrete observer that formats a sensor reading and writes it into a JavaFX [Label]. */
class LabelObserver<T>(
    private val label: Label,
    private val format: (T) -> String = { it.toString() },
) : Observer<T> {
    override fun onUpdate(value: T) {
        label.text = format(value)
    }
}
