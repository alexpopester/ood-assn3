package api

import command.SetVelocityCommand
import observer.Observer

/** Follows the line using three line sensors (left / center / right). */
class LineFollowerProgram : RobotProgram {
    override val name = "Line Follower"

    private val speed      = 75.0
    private val turn       = 60.0
    private val searchSpin = 20.0

    private var lineLeft   = false
    private var lineCenter = false
    private var lineRight  = false

    // Remembers the last turn direction so the robot can search for the line after losing it.
    // -1 = last turned left, 0 = uninitialised, 1 = last turned right
    private var lastTurnDir = 0

    // When lastTurnDir is unknown the robot sweeps clockwise then counter-clockwise
    // until it finds the line, avoiding the "creep into a wall" failure.
    private var sweepDir   = 1.0   // +1 = clockwise, -1 = counter-clockwise
    private var sweepTicks = 0

    private var hasEverFoundLine = false
    private var lostLineTicks    = 0
    private var stopped          = false

    private lateinit var leftObs:   Observer<Boolean>
    private lateinit var centerObs: Observer<Boolean>
    private lateinit var rightObs:  Observer<Boolean>
    private lateinit var api: RobotApi

    override fun startProgram(robot: RobotApi) {
        api = robot
        lastTurnDir      = 0
        sweepDir         = 1.0
        sweepTicks       = 0
        hasEverFoundLine = false
        lostLineTicks    = 0
        stopped          = false
        leftObs   = Observer { lineLeft   = it; react() }
        centerObs = Observer { lineCenter = it; react() }
        rightObs  = Observer { lineRight  = it; react() }
        robot.sensors.lineLeft.subscribe(leftObs)
        robot.sensors.lineCenter.subscribe(centerObs)
        robot.sensors.lineRight.subscribe(rightObs)
    }

    override fun stopProgram(robot: RobotApi) {
        robot.sensors.lineLeft.unsubscribe(leftObs)
        robot.sensors.lineCenter.unsubscribe(centerObs)
        robot.sensors.lineRight.unsubscribe(rightObs)
        robot.perform(SetVelocityCommand(robot.actuator, 0.0, 0.0))
    }

    private fun react() {
        if (stopped) return

        val onLine = lineLeft || lineCenter || lineRight
        if (onLine) {
            hasEverFoundLine = true
            lostLineTicks    = 0
        } else if (hasEverFoundLine) {
            lostLineTicks++
        }

        // After losing the line for ~1.5 s (90 ticks at 60 fps), we've reached the end.
        if (lostLineTicks > 90) {
            stopped = true
            api.perform(SetVelocityCommand(api.actuator, 0.0, 0.0))
            return
        }

        // Track which side sensor fires even while center is active — catches corners.
        if (lineLeft && !lineRight) lastTurnDir = -1
        if (!lineLeft && lineRight) lastTurnDir = 1
        // Reset sweep state whenever the line is visible.
        if (lineLeft || lineCenter || lineRight) { sweepTicks = 0; sweepDir = 1.0 }

        val (l, r) = when {
            lineCenter              -> speed to speed
            lineLeft && !lineRight  -> turn to -turn
            !lineLeft && lineRight  -> -turn to turn
            else                    -> when (lastTurnDir) {
                -1   -> searchSpin to -searchSpin
                 1   -> -searchSpin to searchSpin
                else -> {
                    // lastTurnDir unknown (first straight segment): sweep clockwise then
                    // counter-clockwise in 75-tick phases until a corner is found.
                    if (++sweepTicks > 75) { sweepDir = -sweepDir; sweepTicks = 0 }
                    -searchSpin * sweepDir to searchSpin * sweepDir
                }
            }
        }
        api.perform(SetVelocityCommand(api.actuator, l, r))
    }
}
