package app

import api.DefaultProgramRegistry
import api.DefaultRobotApi
import api.StudentPrograms
import command.CommandInvoker
import command.SetVelocityCommand
import javafx.animation.AnimationTimer
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.BorderPane
import javafx.scene.layout.VBox
import javafx.stage.Stage
import sim.EnvironmentCatalog
import sim.ProgramRunner
import sim.Simulation
import ui.ControlPanel
import ui.ProgramPanel
import ui.SimulationCanvas
import ui.TelemetryPanel

/** Wires the model, the application interface, and the JavaFX UI together and runs the sim loop. */
class RobotSimulationApp : Application() {

    private val worldWidth = 880.0
    private val worldHeight = 600.0

    private val simulation = Simulation(EnvironmentCatalog.all().first())
    private val invoker = CommandInvoker()
    private val api = DefaultRobotApi(
        invoker = invoker,
        actuatorProvider = { simulation.robot },
        sensorsProvider = { simulation.robot },
    )
    private val programRunner = ProgramRunner(api)

    private val telemetry = TelemetryPanel()
    private lateinit var canvas: SimulationCanvas

    private var lastNanos = -1L
    private var accumulator = 0.0

    companion object {
        /** The simulation's fixed physics timestep (60 Hz). */
        const val FIXED_DT = 1.0 / 60.0
    }

    override fun start(stage: Stage) {
        val registry = DefaultProgramRegistry()
        StudentPrograms.registerAll(registry)

        canvas = SimulationCanvas(simulation, programRunner, worldWidth, worldHeight)
        telemetry.bindTo(simulation.robot)

        val controlPanel = ControlPanel(
            api = api,
            environments = EnvironmentCatalog.all(),
            onSelectEnvironment = { env -> switchEnvironment(env.javaClass) },
            onReset = { resetRobot() },
        )
        val programPanel = ProgramPanel(registry, programRunner)

        val root = BorderPane().apply {
            center = canvas
            right = telemetry
            bottom = VBox(programPanel, controlPanel)
        }

        val scene = Scene(root)
        installKeyboardControls(scene)
        stage.title = "Skid-Steer Robot Simulation"
        stage.scene = scene
        stage.show()

        object : AnimationTimer() {
            override fun handle(now: Long) {
                // Fixed-timestep loop ("fix your timestep"): advance the simulation in constant [FIXED_DT]
                // increments regardless of the display's refresh rate. This keeps the physics — and so
                // every autonomous program's control loop — deterministic and frame-rate independent,
                // which is what lets the programs behave the same on any machine.
                val elapsed = if (lastNanos < 0) 0.0 else (now - lastNanos) / 1_000_000_000.0
                lastNanos = now
                accumulator = (accumulator + elapsed).coerceAtMost(0.25)  // clamp to avoid a spiral of death
                while (accumulator >= FIXED_DT) {
                    // A running program reacts through its sensor subscriptions, which fire from
                    // simulation.step -> robot.updateSensors. No explicit program tick needed.
                    simulation.step(FIXED_DT)
                    accumulator -= FIXED_DT
                }
                canvas.render()
            }
        }.start()
    }

    private fun installKeyboardControls(scene: Scene) {
        val speed = 120.0
        val turn = 60.0
        // addEventFilter (capture phase) fires before any focused node can consume the event,
        // which matters because buttons consume SPACE and arrow keys for focus navigation.
        scene.addEventFilter(KeyEvent.KEY_PRESSED) { e ->
            when (e.code) {
                KeyCode.UP    -> { drive(speed, speed);    e.consume() }
                KeyCode.DOWN  -> { drive(-speed, -speed);  e.consume() }
                KeyCode.LEFT  -> { drive(turn, -turn);     e.consume() }
                KeyCode.RIGHT -> { drive(-turn, turn);     e.consume() }
                KeyCode.SPACE -> { drive(0.0, 0.0);        e.consume() }
                else -> {}
            }
        }
    }

    private fun drive(left: Double, right: Double) {
        api.perform(SetVelocityCommand(api.actuator, left, right))
    }

    private fun switchEnvironment(envClass: Class<*>) {
        val env = EnvironmentCatalog.all().first { it.javaClass == envClass }
        programRunner.stop()
        simulation.loadEnvironment(env)
        telemetry.bindTo(simulation.robot)
    }

    private fun resetRobot() {
        programRunner.stop()
        simulation.resetRobot()
        telemetry.bindTo(simulation.robot)
    }
}
