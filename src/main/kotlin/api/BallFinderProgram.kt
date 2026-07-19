package api

import command.SetVelocityCommand
import javafx.scene.paint.Color
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sin
import observer.Observer

/**
 * Finds the red ball in the obstacle course.
 *
 * The vision sensor is a single forward-facing ray, so the ball is only "spotted" when the robot is
 * pointed at it with a clear line of sight. The strategy is therefore a mostly-driving explorer that
 * keeps the vision ray sweeping across new ground:
 *
 *  1. **Explore** — CRUISE forward, and when an obstacle or wall looms (sonar) or is hit (collision),
 *     turn away (AVOID / BACKUP) toward whichever side is *less explored*. To decide "less explored"
 *     the program keeps its own dead-reckoning [odometry] and marks a coarse [visited] grid, biasing
 *     the robot into fresh territory. Because it is nearly always translating, it covers the arena
 *     quickly, and the forward ray naturally scans across the ball as the robot roams and turns.
 *
 *  2. **Home in** — vision is checked every tick; the instant it reports red, line of sight is clear,
 *     so the robot drives straight at the ball until it arrives (with a small sidestep if it clips an
 *     obstacle corner on the way in).
 *
 * It subscribes to a single sensor (sonar) as its control-loop clock — [react] must run exactly once
 * per tick for the odometry to stay in step with the simulation — and reads the rest via `.reading`.
 */
class BallFinderProgram(
    // Wall-clock source for the per-tick timestep. Defaults to the real clock; a headless test can
    // inject a virtual clock to run the simulation deterministically and faster than real time.
    private val clockNanos: () -> Long = System::nanoTime,
) : RobotProgram {
    override val name = "Ball Finder"

    // Mirror model.Robot so the internal odometry matches the simulator's motion.
    private val trackWidth = 26.0

    private val cruiseSpeed = 130.0    // forward drive speed (max track speed is 150)
    private val spinSpeed   = 65.0     // in-place turn speed for avoidance

    private val avoidDist = 62.0       // sonar distance ahead that triggers an avoidance turn
    private val clearDist = 120.0      // sonar distance ahead that counts as clear again
    private val cellSize  = 80.0       // visited-grid resolution, in odometry units

    private val reorientEvery = 200   // ticks of cruising before steering toward fresh ground

    private enum class Mode { CRUISE, ORIENT, AVOID, BACKUP, HOMING }
    private var mode = Mode.CRUISE

    // --- dead-reckoning odometry (relative to the start pose) ---
    private var odoX = 0.0
    private var odoY = 0.0
    private var odoHeading = 0.0
    private var lastClock = -1L
    private var lastDt = 0.0
    private val visited = HashMap<Long, Int>()

    // --- manoeuvre bookkeeping ---
    private var turnDir = 1.0          // +1 = increase heading (turn right on screen), -1 = left
    private var cruisePhase = 0.0      // drives the cruise weave that sweeps the vision ray
    private var cruiseTicks = 0        // cruising time since the last reorient
    private var targetHeading = 0.0    // heading ORIENT steers toward
    private var avoidTicks = 0
    private var backupTicks = 0
    private var homingLostTicks = 0

    // --- stuck detection: break out of local-minimum pockets ---
    private var stuckRefX = 0.0
    private var stuckRefY = 0.0
    private var stuckTicks = 0
    private var ignoreRedTicks = 0

    private lateinit var clockObs: Observer<Double>
    private lateinit var api: RobotApi

    override fun startProgram(robot: RobotApi) {
        api = robot
        mode = Mode.CRUISE
        odoX = 0.0; odoY = 0.0; odoHeading = 0.0
        lastClock = -1L; lastDt = 0.0
        visited.clear()
        turnDir = 1.0
        cruisePhase = 0.0; cruiseTicks = 0; targetHeading = 0.0
        avoidTicks = 0; backupTicks = 0; homingLostTicks = 0
        stuckRefX = 0.0; stuckRefY = 0.0; stuckTicks = 0
        ignoreRedTicks = 0

        clockObs = Observer { react() }
        robot.sensors.sonar.subscribe(clockObs)
    }

    override fun stopProgram(robot: RobotApi) {
        robot.sensors.sonar.unsubscribe(clockObs)
        robot.perform(SetVelocityCommand(robot.actuator, 0.0, 0.0))
    }

    private fun react() {
        updateOdometry()
        markVisited()

        val vision = api.sensors.vision.reading ?: Color.TRANSPARENT
        val sonar = api.sensors.sonar.reading ?: Double.MAX_VALUE
        if (ignoreRedTicks > 0) ignoreRedTicks--

        // Stuck detection: barely moved for a while (a pocket, or wedged on a corner while homing) —
        // force a strong escape: a long reverse and a turn the other way. When it fires mid-homing we
        // briefly ignore red so the robot can pull off the obstacle and re-approach from a new angle.
        if (hypot(odoX - stuckRefX, odoY - stuckRefY) > 70.0) {
            stuckRefX = odoX; stuckRefY = odoY; stuckTicks = 0
        } else if (++stuckTicks > 300) {
            stuckTicks = 0; ignoreRedTicks = 120
            turnDir = -turnDir; backupTicks = 30; mode = Mode.BACKUP
        }

        // Seeing red overrides everything else: line of sight is clear, so home straight in.
        if (ignoreRedTicks == 0 && isRed(vision)) {
            homingLostTicks = 0; mode = Mode.HOMING; driveHoming(); return
        }

        when (mode) {
            Mode.CRUISE -> doCruise(sonar)
            Mode.ORIENT -> doOrient()
            Mode.AVOID  -> doAvoid(sonar)
            Mode.BACKUP -> doBackup()
            Mode.HOMING -> reacquire()   // lost the ball mid-approach
        }
    }

    // ---- odometry ------------------------------------------------------------------------------

    /** Integrate the velocities we last commanded, matching model.Robot.step's kinematics. */
    private fun updateOdometry() {
        // Compute dt exactly as model.Robot does (wall clock, capped at 0.033) so the estimate tracks
        // the simulator at any frame rate.
        val now = clockNanos()
        lastDt = if (lastClock < 0L) 0.0 else ((now - lastClock) / 1e9).coerceAtMost(0.033)
        lastClock = now

        val l = api.actuator.leftTrackVelocity
        val r = api.actuator.rightTrackVelocity
        val v = (l + r) / 2.0
        val omega = (r - l) / trackWidth
        val moveHeading = odoHeading           // translation uses the pre-turn heading, as the sim does
        odoHeading += omega * lastDt
        if (api.sensors.collision.reading != true) {
            odoX += v * cos(moveHeading) * lastDt
            odoY += v * sin(moveHeading) * lastDt
        }
    }

    private fun markVisited() {
        val key = cellKey(odoX, odoY)
        visited[key] = (visited[key] ?: 0) + 1
    }

    private fun cellKey(x: Double, y: Double): Long {
        val cx = floor(x / cellSize).toInt()
        val cy = floor(y / cellSize).toInt()
        return (cx.toLong() shl 32) or (cy.toLong() and 0xffffffffL)
    }

    private fun visitsAt(heading: Double, dist: Double): Int =
        visited[cellKey(odoX + cos(heading) * dist, odoY + sin(heading) * dist)] ?: 0

    // ---- explore: cruise, avoid, recover -------------------------------------------------------

    private fun doCruise(sonar: Double) {
        if (api.sensors.collision.reading == true) { backupTicks = 15; mode = Mode.BACKUP; return }
        // Periodically steer toward the least-visited direction so exploration progresses into fresh
        // ground instead of drifting back over old ground.
        if (++cruiseTicks > reorientEvery) {
            cruiseTicks = 0
            targetHeading = bestExploreHeading()
            mode = Mode.ORIENT
            return
        }
        if (sonar < avoidDist) {
            // Turn toward whichever side leads into less-visited territory (odometry-based, no extra
            // sensing) so avoidance doubles as exploration.
            val right = visitsAt(odoHeading + 1.0, 150.0)   // +heading = clockwise on screen
            val left  = visitsAt(odoHeading - 1.0, 150.0)
            turnDir = if (right <= left) 1.0 else -1.0
            avoidTicks = 0
            mode = Mode.AVOID
            return
        }
        // Weave while cruising: the differential term cancels in the average (forward speed is
        // unchanged) but swings the heading ±~30°, sweeping the forward vision ray so it catches the
        // ball even when the robot passes to one side of it.
        cruisePhase += lastDt
        val w = 24.0 * sin(cruisePhase * 3.0)
        drive(cruiseSpeed - w, cruiseSpeed + w)
    }

    /** Least-visited compass direction, sampling the odometry grid a short way out along each. */
    private fun bestExploreHeading(): Double {
        var best = odoHeading
        var bestVisits = Int.MAX_VALUE
        val dirs = 12
        for (i in 0 until dirs) {
            val h = i * (2 * Math.PI / dirs)
            val v = visitsAt(h, 120.0) + visitsAt(h, 240.0)
            if (v < bestVisits) { bestVisits = v; best = h }
        }
        return best
    }

    private fun doOrient() {
        if (api.sensors.collision.reading == true) { backupTicks = 15; mode = Mode.BACKUP; return }
        val diff = normalize(targetHeading - odoHeading)
        if (abs(diff) < 0.15) { mode = Mode.CRUISE; return }
        if (diff > 0) drive(-spinSpeed, spinSpeed) else drive(spinSpeed, -spinSpeed)
    }

    private fun doAvoid(sonar: Double) {
        if (api.sensors.collision.reading == true) { backupTicks = 15; mode = Mode.BACKUP; return }
        // Turned a long way and still boxed in — back out and try the other way.
        if (++avoidTicks > 130) { turnDir = -turnDir; backupTicks = 15; mode = Mode.BACKUP; return }
        if (sonar > clearDist) { mode = Mode.CRUISE; return }
        // +turnDir spins heading up (right on screen): right track leads.
        if (turnDir > 0) drive(-spinSpeed, spinSpeed) else drive(spinSpeed, -spinSpeed)
    }

    private fun doBackup() {
        // Reverse while curving in the current turn direction, opening a new heading to cruise on.
        if (turnDir > 0) drive(-cruiseSpeed * 0.6, -cruiseSpeed * 0.35)
        else drive(-cruiseSpeed * 0.35, -cruiseSpeed * 0.6)
        if (--backupTicks <= 0) mode = Mode.CRUISE
    }

    // ---- home in on the ball -------------------------------------------------------------------

    private fun driveHoming() {
        // Line of sight to the ball is clear, so drive straight at it. A collision means the body
        // clipped an obstacle corner beside the path; reverse *while turning* to re-approach at a new
        // angle. If it truly can't get through, stuck detection escalates. The simulator ends the run
        // once the ball is touched.
        if (api.sensors.collision.reading == true) drive(-cruiseSpeed * 0.6, -cruiseSpeed * 0.3)
        else drive(cruiseSpeed, cruiseSpeed)
    }

    /** Ball slipped out of the vision ray mid-approach: turn to re-find it, then fall back to cruise. */
    private fun reacquire() {
        if (++homingLostTicks > 40) { homingLostTicks = 0; mode = Mode.CRUISE; return }
        drive(-spinSpeed * 0.5, spinSpeed * 0.5)
    }

    // ---- helpers -------------------------------------------------------------------------------

    private fun drive(l: Double, r: Double) = api.perform(SetVelocityCommand(api.actuator, l, r))

    private fun isRed(c: Color) = c.red > 0.55 && c.green < 0.35 && c.blue < 0.35

    private fun normalize(a: Double) = atan2(sin(a), cos(a))
}
