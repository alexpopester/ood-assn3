package ui

import geometry.Vector2
import javafx.scene.canvas.Canvas
import javafx.scene.paint.Color
import javafx.scene.text.Font
import sim.ProgramRunner
import sim.Simulation
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders the top-down 2D view every frame: environment underlay (heat map / lines / obstacles /
 * ball), the robot with its heading and sensor rays, and a status line. Pure drawing — no game
 * state lives here.
 */
class SimulationCanvas(
    private val simulation: Simulation,
    private val programRunner: ProgramRunner,
    width: Double,
    height: Double,
) : Canvas(width, height) {

    private val gc get() = graphicsContext2D

    fun render() {
        val env = simulation.environment

        // floor
        gc.fill = env.floorColor
        gc.fillRect(0.0, 0.0, width, height)

        drawHeatMap()
        drawLines()
        drawObstacles()
        drawBall()
        drawSensors()
        drawRobot()
        drawHud()
    }

    private fun drawHeatMap() {
        val field = simulation.environment.temperatureField ?: return
        val cell = 16.0
        var y = 0.0
        while (y < height) {
            var x = 0.0
            while (x < width) {
                val t = field.valueAt(Vector2(x + cell / 2, y + cell / 2))
                val f = ((t - field.ambient) / (field.peak - field.ambient)).coerceIn(0.0, 1.0)
                gc.fill = Color.color(0.05 + 0.9 * f, 0.1 + 0.15 * (1 - f), 0.35 * (1 - f), 1.0)
                gc.fillRect(x, y, cell, cell)
                x += cell
            }
            y += cell
        }
    }

    private fun drawLines() {
        val env = simulation.environment
        if (env.lines.isEmpty()) return
        gc.stroke = env.lineColor
        gc.lineWidth = env.lineWidth
        for (seg in env.lines) gc.strokeLine(seg.a.x, seg.a.y, seg.b.x, seg.b.y)
    }

    private fun drawObstacles() {
        gc.fill = Color.web("#4a4f57")
        gc.stroke = Color.web("#6f7680")
        gc.lineWidth = 2.0
        for (o in simulation.environment.obstacles) {
            gc.fillRect(o.bounds.x, o.bounds.y, o.bounds.width, o.bounds.height)
            gc.strokeRect(o.bounds.x, o.bounds.y, o.bounds.width, o.bounds.height)
        }
    }

    private fun drawBall() {
        val ball = simulation.environment.ball ?: return
        gc.fill = ball.color
        gc.fillOval(ball.center.x - ball.radius, ball.center.y - ball.radius, ball.radius * 2, ball.radius * 2)
    }

    private fun drawSensors() {
        val robot = simulation.robot

        // Sonar ray
        val sonarPose = robot.sonar.worldPose(robot.pose)
        val dist = robot.sonar.reading ?: robot.sonar.maxRange
        val end = sonarPose.position + Vector2.fromAngle(sonarPose.heading, dist)
        gc.stroke = Color.web("#59c3ff")
        gc.lineWidth = 1.5
        gc.strokeLine(sonarPose.x, sonarPose.y, end.x, end.y)
        gc.fill = Color.web("#59c3ff")
        gc.fillOval(end.x - 3, end.y - 3, 6.0, 6.0)

        // Line sensors as small dots (green when on the line)
        for (s in listOf(robot.lineLeft, robot.lineCenter, robot.lineRight)) {
            val p = s.worldPose(robot.pose)
            gc.fill = if (s.reading == true) Color.LIMEGREEN else Color.web("#888888")
            gc.fillOval(p.x - 2.5, p.y - 2.5, 5.0, 5.0)
        }
    }

    private fun drawRobot() {
        val robot = simulation.robot
        val p = robot.pose
        val r = robot.radius

        gc.fill = Color.web("#f2c14e")
        gc.fillOval(p.x - r, p.y - r, r * 2, r * 2)
        gc.stroke = Color.web("#2b2b2b")
        gc.lineWidth = 2.0
        gc.strokeOval(p.x - r, p.y - r, r * 2, r * 2)

        // heading indicator
        val nose = Vector2(p.x + cos(p.heading) * r, p.y + sin(p.heading) * r)
        gc.stroke = Color.web("#2b2b2b")
        gc.lineWidth = 3.0
        gc.strokeLine(p.x, p.y, nose.x, nose.y)
    }

    private fun drawHud() {
        gc.font = Font.font(14.0)
        gc.fill = Color.WHITE
        gc.fillText(simulation.objectiveStatus(), 12.0, 22.0)
        gc.fill = Color.web("#c9d1d9")
        gc.fillText(programRunner.statusText, 12.0, 42.0)
    }
}
