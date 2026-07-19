package api

import command.SetVelocityCommand
import javafx.scene.paint.Color
import observer.Observer

/** Uses the vision sensor to spot the red ball and sonar + collision to navigate the obstacle course. */
class BallFinderProgram : RobotProgram {
    override val name = "Ball Finder"

    private val speed      = 100.0
    private val turn       = 80.0
    private val sonarClear = 65.0

    private var sonarDist    = Double.MAX_VALUE
    private var visionColor: Color = Color.TRANSPARENT
    private var isColliding  = false
    private var recoveryTicks = 0
    private var recoveryDir   = 1.0  // flips sign on each collision so the robot doesn't grind the same wall

    private lateinit var sonarObs:     Observer<Double>
    private lateinit var visionObs:    Observer<Color>
    private lateinit var collisionObs: Observer<Boolean>
    private lateinit var api: RobotApi

    override fun startProgram(robot: RobotApi) {
        api = robot
        sonarDist     = Double.MAX_VALUE
        visionColor   = Color.TRANSPARENT
        isColliding   = false
        recoveryTicks = 0
        recoveryDir   = 1.0

        sonarObs     = Observer { sonarDist   = it; react() }
        visionObs    = Observer { visionColor = it; react() }
        collisionObs = Observer { isColliding = it; react() }
        robot.sensors.sonar.subscribe(sonarObs)
        robot.sensors.vision.subscribe(visionObs)
        robot.sensors.collision.subscribe(collisionObs)
    }

    override fun stopProgram(robot: RobotApi) {
        robot.sensors.sonar.unsubscribe(sonarObs)
        robot.sensors.vision.unsubscribe(visionObs)
        robot.sensors.collision.unsubscribe(collisionObs)
        robot.perform(SetVelocityCommand(robot.actuator, 0.0, 0.0))
    }

    private fun react() {
        // Recovery mode: back up + spin in alternating direction to escape walls.
        if (recoveryTicks > 0) {
            recoveryTicks--
            api.perform(SetVelocityCommand(api.actuator, -speed * 0.5, speed * recoveryDir))
            return
        }

        val (l, r) = when {
            isRed(visionColor)     -> speed to speed
            isColliding            -> {
                // Flip spin direction each time so the robot doesn't grind the same wall repeatedly.
                recoveryDir   = -recoveryDir
                recoveryTicks = 50
                -speed * 0.5 to speed * recoveryDir
            }
            sonarDist < sonarClear -> turn to -turn         // obstacle ahead — spin left
            else                   -> speed * 0.8 to speed  // clear — gentle right curve
        }
        api.perform(SetVelocityCommand(api.actuator, l, r))
    }

    private fun isRed(c: Color) = c.red > 0.55 && c.green < 0.35 && c.blue < 0.35
}
