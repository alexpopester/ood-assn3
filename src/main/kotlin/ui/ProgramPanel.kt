package ui

import api.ProgramRegistry
import api.RobotProgram
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import sim.ProgramRunner

/**
 * The registry-driven program controller: a dropdown of everything registered via the
 * [ProgramRegistry] plus Run / Stop buttons. Fully provided plumbing — students only need to
 * *write and register* a program for it to appear here.
 */
class ProgramPanel(
    registry: ProgramRegistry,
    private val programRunner: ProgramRunner,
) : HBox(8.0) {

    init {
        padding = Insets(10.0, 10.0, 0.0, 10.0)
        alignment = Pos.CENTER_LEFT
        style = "-fx-background-color: #1b1f24;"

        val programs = registry.programs()
        val combo = ComboBox<RobotProgram>().apply {
            items.addAll(programs)
            if (programs.isNotEmpty()) selectionModel.selectFirst()
            setCellFactory { cell() }
            buttonCell = cell()
            prefWidth = 260.0
        }

        val runButton = Button("▶ Run Program").apply {
            setOnAction { combo.value?.let { programRunner.run(it) } }
        }
        val stopButton = Button("■ Stop").apply {
            setOnAction { programRunner.stop() }
        }

        if (programs.isEmpty()) {
            combo.isDisable = true
            runButton.isDisable = true
            combo.promptText = "(no programs registered)"
        }

        children.addAll(caption("Program:"), combo, runButton, stopButton)
    }

    private fun caption(text: String) = Label(text).apply { style = "-fx-text-fill: #c9d1d9;" }

    private fun cell() = object : javafx.scene.control.ListCell<RobotProgram>() {
        override fun updateItem(item: RobotProgram?, empty: Boolean) {
            super.updateItem(item, empty)
            text = if (empty || item == null) null else item.name
        }
    }
}
