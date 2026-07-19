package sim

import api.RobotApi
import api.RobotProgram

/**
 * Runs one registered [RobotProgram] at a time. Running a program calls its [RobotProgram.startProgram]
 * (where it subscribes to sensors); stopping calls [RobotProgram.stopProgram] (where it unsubscribes).
 * There is no per-tick call here — a running program drives itself from its sensor subscriptions.
 */
class ProgramRunner(private val api: RobotApi) {

    var active: RobotProgram? = null
        private set
    var running: Boolean = false
        private set

    fun run(program: RobotProgram) {
        stop() // tear down any previous program first
        active = program
        program.startProgram(api)
        running = true
    }

    fun stop() {
        val program = active
        if (program != null && running) program.stopProgram(api)
        running = false
    }

    val statusText: String
        get() = when {
            active == null -> "No program loaded"
            running -> "Running: ${active!!.name}"
            else -> "Stopped: ${active!!.name}"
        }
}
