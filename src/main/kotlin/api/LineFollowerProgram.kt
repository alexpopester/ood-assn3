package api

import command.SetVelocityCommand
import observer.Observer

/**
 * Follows the line to the end of the maze using the three line sensors (left / center / right).
 *
 * The maze is a single line with sharp 90° corners. The control law reacts once per tick and steers
 * as `left = base + s`, `right = base - s`: the `±s` cancels in the average, so the robot always
 * advances at `base` no matter how hard it turns — it can never deadlock rotating in place at a
 * corner junction (the failure the previous version hit).
 *
 *  - **Center on the line** → hold nearly straight, nudging gently toward whichever side sensor also
 *    fires. This keeps a straight run smooth instead of weaving.
 *  - **Center off, one side on** → a real 90° corner: the line has swung off to that side, so turn
 *    hard toward it until the center re-acquires the line.
 *  - **Line lost entirely** → arc back toward the side that last had it. At a corner this re-acquires
 *    within a short arc. Only when the line is lost coming off a *straight* run (no corner for a
 *    while) and stays lost do we conclude we drove off the true end, and stop.
 */
class LineFollowerProgram : RobotProgram {
    override val name = "Line Follower"

    private val base        = 55.0  // forward speed, held constant while steering
    private val nudge       = 16.0  // gentle centering correction while the center is on the line
    private val turnHard    = 85.0  // hard turn into a corner once the center leaves the line
    private val recoverBase = 12.0  // slow forward creep while searching for a lost line

    private var left   = false
    private var center = false
    private var right  = false

    // Which side last saw the line (-1 = left, +1 = right); steers corner recovery.
    private var lastSide   = -1  // the maze's first corner bends toward the left sensor
    private var lostTicks  = 0   // consecutive ticks with no line at all
    private var sinceCorner = 999 // ticks since the last real corner (center off, a side on)
    private var finished   = false

    private lateinit var leftObs:   Observer<Boolean>
    private lateinit var centerObs: Observer<Boolean>
    private lateinit var rightObs:  Observer<Boolean>
    private lateinit var api: RobotApi

    override fun startProgram(robot: RobotApi) {
        api = robot
        left = false; center = false; right = false
        lastSide = -1; lostTicks = 0; sinceCorner = 999; finished = false

        // Sensors notify in suite order (…, lineLeft, lineCenter, lineRight, …), so reacting on
        // lineRight runs react() exactly once per tick with all three values fresh.
        leftObs   = Observer { left   = it }
        centerObs = Observer { center = it }
        rightObs  = Observer { right  = it; react() }
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
        if (finished) return

        val onlyLeft  = left && !right
        val onlyRight = right && !left
        val corner    = !center && (left || right)   // center left the line, a side still sees it
        sinceCorner = if (corner) 0 else sinceCorner + 1
        if (onlyLeft)  lastSide = -1
        if (onlyRight) lastSide = 1

        if (left || center || right) {
            lostTicks = 0
            val s = when {
                corner && onlyLeft  ->  turnHard   // hard turn into a left corner
                corner && onlyRight -> -turnHard   // hard turn into a right corner
                center && onlyLeft  ->  nudge      // drifting right of the line — ease left
                center && onlyRight -> -nudge      // drifting left of the line — ease right
                else                ->  0.0        // centered (010 / 111) — go straight
            }
            drive(base + s, base - s)
            return
        }

        // Line lost.
        lostTicks++
        if (sinceCorner > 20) {
            // Lost on a straight run (not at a corner) → we drove off the end of the maze. Coast
            // straight a moment to rule out a blip, then stop. Crucially we do NOT rotate here: turning
            // would swing the sensors back onto the line we just left and send the robot back down it.
            if (lostTicks > 10) { finished = true; drive(0.0, 0.0); return }
            drive(recoverBase, recoverBase)
            return
        }
        // Otherwise we overshot a corner — arc back toward the side that last had the line.
        val s = if (lastSide < 0) turnHard else -turnHard
        drive(recoverBase + s, recoverBase - s)
    }

    private fun drive(l: Double, r: Double) = api.perform(SetVelocityCommand(api.actuator, l, r))
}
