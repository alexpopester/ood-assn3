package api

import command.SetVelocityCommand
import observer.Observer

/**
 * Climbs the temperature gradient toward the hot spot. Uses the temperature sensor as a heartbeat
 * and the sonar to avoid obstacles along the way.
 */
class HeatSeekerProgram : RobotProgram {
    override val name = "Heat Seeker"

    private val speed      = 100.0
    private val turn       = 65.0
    private val sonarClear = 55.0

    private var lastTemp   = Double.MIN_VALUE
    private var turnDir    = 1.0    // +1 = right, -1 = left
    private var sonarDist  = Double.MAX_VALUE

    private lateinit var tempObs:  Observer<Double>
    private lateinit var sonarObs: Observer<Double>
    private lateinit var api: RobotApi

    override fun startProgram(robot: RobotApi) {
        api = robot
        lastTemp  = Double.MIN_VALUE
        turnDir   = 1.0
        sonarDist = Double.MAX_VALUE

        sonarObs = Observer { sonarDist = it }

        tempObs = Observer { temp ->
            // If temperature dropped since last reading, flip turn direction.
            if (lastTemp != Double.MIN_VALUE && temp < lastTemp) turnDir = -turnDir
            lastTemp = temp
            react(temp)
        }

        robot.sensors.sonar.subscribe(sonarObs)
        robot.sensors.temperature.subscribe(tempObs)
    }

    override fun stopProgram(robot: RobotApi) {
        robot.sensors.sonar.unsubscribe(sonarObs)
        robot.sensors.temperature.unsubscribe(tempObs)
        robot.perform(SetVelocityCommand(robot.actuator, 0.0, 0.0))
    }

    private fun react(temp: Double) {
        val (l, r) = when {
            sonarDist < sonarClear -> speed * turnDir to -(speed * turnDir)  // obstacle — spin away
            temp >= lastTemp       -> speed to speed                          // climbing — go straight
            else                   -> turn * turnDir to -(turn * turnDir)     // descending — seek
        }
        api.perform(SetVelocityCommand(api.actuator, l, r))
    }
}
