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
 * Finds and touches the red ball in the obstacle course.
 *
 * The vision sensor is a single forward-facing ray, so the ball is only "seen" when the robot is
 * pointed at it with a clear line of sight. The explorer is a robust **bump-and-turn** roamer:
 *
 *  1. **DRIVE** — cruise forward. Watch vision every tick (red ⇒ HOME). Use sonar to turn away from
 *     obstacles *before* hitting them (AVOID). Every so often, pause to look around (SCAN).
 *  2. **AVOID** — an obstacle/wall is close: arc away toward whichever side has been *less visited*
 *     (odometry-backed) until the way ahead is clear again, then resume driving.
 *  3. **TURN-AWAY** — a collision slipped past the sonar (the single ray can thread a gap the wider
 *     robot body cannot). Rotate a committed angle toward the less-visited side and drive on. This
 *     is the key to not getting pinned on a corner — we never re-drive into the same clip.
 *  4. **SCAN** — spin a full turn in place, sweeping the vision ray across everything around us to
 *     catch the ball from this vantage point; red ⇒ HOME, otherwise carry on.
 *  5. **HOME** — line of sight is clear, so drive straight at the ball. Because the ball is not solid,
 *     a long "locked-on" run of red frames means we closed the distance and drove through it (touched
 *     it) when red finally drops — so we stop.
 *
 * All thresholds are physical (distance / angle / time), never tick counts. It subscribes to the
 * vision, sonar and collision sensors, caching each reading in a field as its observer fires, and
 * runs its control step off the collision callback — the last sensor updated each tick — so vision
 * and sonar are already refreshed when it reacts. The behaviour reads only those cached fields.
 * A light dead-reckoning [odometry] estimate backs the "less visited" bias and the arrival test; it
 * advances by [dtPerTick], the simulation's fixed timestep (the app runs a fixed-timestep loop, so
 * every react corresponds to exactly one physics step of this size — no wall-clock guessing needed).
 */
class BallFinderProgram(
    private val dtPerTick: Double = 1.0 / 60.0,
) : RobotProgram {
    override val name = "Ball Finder"

    private val trackWidth = 26.0    // mirror model.Robot so odometry matches the simulator

    private val cruise    = 140.0    // forward drive speed (near the 150 track-speed cap)
    private val spin      = 100.0    // in-place turn speed for scanning / turning away
    private val avoidDist = 78.0     // sonar ahead this close → arc away
    private val clearDist = 135.0    // sonar ahead this open → done avoiding, drive again
    private val turnAway  = 1.4      // radians to rotate after a collision (~80°)
    private val bigTurn   = 2.4      // radians for the stuck-escape turn (~140°)
    private val backupTime = 0.32    // seconds to reverse (to unpin) before turning away
    private val scanEvery = 330.0    // odometry distance driven between look-around scans (vision is
                                     // also checked every tick while driving, so scans are just a bonus)
    private val arriveDist = 55.0    // distance to close while locked on red before "touched" counts
    private val stuckTime = 2.5      // seconds without net progress → reverse clear and change tack
    private val cellSize  = 90.0     // visited-grid resolution (odometry units)

    private enum class Mode { DRIVE, AVOID, BACKUP, TURNAWAY, SCAN, HOME, DONE }
    private var mode = Mode.SCAN

    // dead-reckoning odometry (relative to the start pose) + a coarse visited grid for coverage bias
    private var odoX = 0.0
    private var odoY = 0.0
    private var odoHeading = 0.0
    private var lastDt = 0.0
    private val visited = HashMap<Long, Int>()

    private var scanSweep = 0.0        // heading swept so far this SCAN
    private var prevHeading = 0.0
    private var turnTarget = 0.0       // heading TURNAWAY is rotating toward
    private var avoidDir = 1.0         // +1 / -1 turn direction while avoiding
    private var backupSecs = 0.0       // seconds spent reversing this BACKUP
    private var backupDur = 0.32       // how long to reverse this BACKUP
    private var pendingTurn = 1.4      // radians to turn away after the current backup finishes
    private var scanRefX = 0.0         // where we last scanned from (drives scanEvery)
    private var scanRefY = 0.0

    // homing: redDist = distance closed while continuously locked on red; closing a long distance then
    // losing red means we drove through the (non-solid) ball — i.e. touched it.
    private var redDist = 0.0
    private var homePrevX = 0.0
    private var homePrevY = 0.0
    private var homeLost = 0

    // stuck escape (time + net displacement, frame-rate independent)
    private var stuckRefX = 0.0
    private var stuckRefY = 0.0
    private var stuckSecs = 0.0

    // Latest sensor state, kept up to date by the observer callbacks below rather than polled from the
    // sensor objects. The behaviour in react()/doHome()/updateOdometry() reads only these fields.
    private var visionColor: Color = Color.TRANSPARENT
    private var sonarDist: Double = Double.MAX_VALUE
    private var collided: Boolean = false

    private lateinit var visionObs: Observer<Color>
    private lateinit var sonarObs: Observer<Double>
    private lateinit var collisionObs: Observer<Boolean>
    private lateinit var api: RobotApi

    override fun startProgram(robot: RobotApi) {
        api = robot
        odoX = 0.0; odoY = 0.0; odoHeading = 0.0; lastDt = 0.0
        visited.clear()
        scanRefX = 0.0; scanRefY = 0.0
        redDist = 0.0; homeLost = 0
        stuckRefX = 0.0; stuckRefY = 0.0; stuckSecs = 0.0
        visionColor = Color.TRANSPARENT; sonarDist = Double.MAX_VALUE; collided = false
        beginScan()   // look around once before setting off

        // Subscribe to every sensor the behaviour depends on, caching each reading as it arrives.
        // Collision is the last sensor updated each tick, so reacting on it runs react() exactly once
        // per tick with vision and sonar already refreshed into their fields.
        visionObs = Observer { visionColor = it }
        sonarObs = Observer { sonarDist = it }
        collisionObs = Observer { collided = it; react() }
        robot.sensors.vision.subscribe(visionObs)
        robot.sensors.sonar.subscribe(sonarObs)
        robot.sensors.collision.subscribe(collisionObs)
    }

    override fun stopProgram(robot: RobotApi) {
        robot.sensors.vision.unsubscribe(visionObs)
        robot.sensors.sonar.unsubscribe(sonarObs)
        robot.sensors.collision.unsubscribe(collisionObs)
        robot.perform(SetVelocityCommand(robot.actuator, 0.0, 0.0))
    }

    private fun react() {
        if (mode == Mode.DONE) return
        updateOdometry()
        markVisited()

        val vision = visionColor
        val sonar  = sonarDist
        val hit    = collided

        // First sight of red starts homing (but don't interrupt a backup — let it finish separating
        // from whatever we just hit). Once in HOME, doHome owns the red / red-lost / arrival logic.
        if (isRed(vision) && mode != Mode.HOME && mode != Mode.BACKUP) beginHome()

        checkStuck()

        when (mode) {
            Mode.DRIVE    -> doDrive(sonar, hit)
            Mode.AVOID    -> doAvoid(sonar, hit)
            Mode.BACKUP   -> doBackup()
            Mode.TURNAWAY -> doTurnAway()
            Mode.SCAN     -> doScan()
            Mode.HOME     -> doHome(hit)
            Mode.DONE     -> {}
        }
    }

    // ---- explore -------------------------------------------------------------------------------

    private fun beginDrive() {
        mode = Mode.DRIVE
    }

    private fun doDrive(sonar: Double, hit: Boolean) {
        if (hit) { beginBackup(turnAway); return }
        if (sonar < avoidDist) { beginAvoid(); return }
        if (hypot(odoX - scanRefX, odoY - scanRefY) > scanEvery) { beginScan(); return }
        drive(cruise, cruise)
    }

    private fun beginAvoid() {
        mode = Mode.AVOID
        avoidDir = lessVisitedTurnDir()
    }

    private fun doAvoid(sonar: Double, hit: Boolean) {
        if (hit) { beginBackup(turnAway); return }
        if (sonar > clearDist) { beginDrive(); return }
        // Arc away (slow forward + turn) toward the less-visited side until the path opens up.
        val s = spin * avoidDir
        drive(cruise * 0.25 - s, cruise * 0.25 + s)
    }

    /** Recovery is two phases: reverse to physically separate from what we hit, then turn away from it. */
    private fun beginBackup(turnAmount: Double, dur: Double = backupTime) {
        mode = Mode.BACKUP
        backupSecs = 0.0
        pendingTurn = turnAmount
        backupDur = dur
    }

    private fun doBackup() {
        drive(-cruise * 0.5, -cruise * 0.5)   // reverse to unpin (translation away from the obstacle)
        backupSecs += lastDt
        if (backupSecs >= backupDur) {
            turnTarget = odoHeading + pendingTurn * lessVisitedTurnDir()
            mode = Mode.TURNAWAY
        }
    }

    private fun doTurnAway() {
        val diff = angleDiff(turnTarget, odoHeading)
        if (abs(diff) < 0.12) { beginDrive(); return }   // fixed target, so this always completes
        if (diff > 0) drive(-spin, spin) else drive(spin, -spin)
    }

    private fun beginScan() {
        mode = Mode.SCAN
        scanSweep = 0.0
        prevHeading = odoHeading
        scanRefX = odoX; scanRefY = odoY
    }

    private fun doScan() {
        scanSweep += abs(angleDiff(odoHeading, prevHeading))
        prevHeading = odoHeading
        if (scanSweep >= 2 * Math.PI) { beginDrive(); return }
        drive(spin, -spin)   // rotate one direction, sweeping the vision ray all the way round
    }

    /** Time-based escape: if we make no net progress for a while, force a big turn-away to break out. */
    private fun checkStuck() {
        if (mode == Mode.HOME || mode == Mode.SCAN) { stuckRefX = odoX; stuckRefY = odoY; stuckSecs = 0.0; return }
        if (hypot(odoX - stuckRefX, odoY - stuckRefY) > 55.0) {
            stuckRefX = odoX; stuckRefY = odoY; stuckSecs = 0.0
        } else {
            stuckSecs += lastDt
            if (stuckSecs >= stuckTime) {   // no real progress for a while → reverse well clear and change tack
                stuckSecs = 0.0
                beginBackup(bigTurn, dur = 0.7)
            }
        }
    }

    // ---- home in on the ball -------------------------------------------------------------------

    private fun beginHome() {
        mode = Mode.HOME
        redDist = 0.0; homeLost = 0
        homePrevX = odoX; homePrevY = odoY
    }

    private fun doHome(hit: Boolean) {
        val red = isRed(visionColor)

        // A collision after we've closed a good distance on the ball means we've arrived (the ball sits
        // in the corner, so we bump the walls right at it). Otherwise it's an obstacle in the way.
        if (hit) {
            if (redDist >= arriveDist) { mode = Mode.DONE; drive(0.0, 0.0); return }
            beginBackup(turnAway); return
        }
        if (red) {
            homeLost = 0
            redDist += hypot(odoX - homePrevX, odoY - homePrevY)   // distance closed while locked on
            homePrevX = odoX; homePrevY = odoY
            drive(cruise, cruise)   // clear sight line — drive straight at the ball
            return
        }
        // Red just dropped. If we'd closed a long distance locked on, we drove straight through the
        // (non-solid) ball — i.e. touched it — so stop. A short lock means it was a distant glimpse
        // that an obstacle occluded; turn to reacquire, or give up to exploring if it stays lost.
        if (redDist >= arriveDist) { mode = Mode.DONE; drive(0.0, 0.0); return }
        redDist = 0.0
        if (++homeLost > 55) { beginDrive(); return }
        drive(-spin * 0.5, spin * 0.5)
    }

    // ---- odometry & coverage -------------------------------------------------------------------

    /** Integrate the commanded velocities, matching model.Robot.step's kinematics. */
    private fun updateOdometry() {
        lastDt = dtPerTick   // the sim advances in fixed steps, so every react is exactly one step

        val l = api.actuator.leftTrackVelocity
        val r = api.actuator.rightTrackVelocity
        val v = (l + r) / 2.0
        val omega = (r - l) / trackWidth
        val moveHeading = odoHeading
        odoHeading += omega * lastDt
        if (!collided) {
            odoX += v * cos(moveHeading) * lastDt
            odoY += v * sin(moveHeading) * lastDt
        }
    }

    private fun markVisited() {
        val key = cellKey(odoX, odoY)
        visited[key] = (visited[key] ?: 0) + 1
    }

    /** Turn toward whichever side (left/right of current heading) leads into less-visited ground. */
    private fun lessVisitedTurnDir(): Double {
        val right = visitsAt(odoHeading + 1.2)
        val left  = visitsAt(odoHeading - 1.2)
        return if (right <= left) 1.0 else -1.0
    }

    private fun visitsAt(heading: Double): Int {
        val x = odoX + cos(heading) * cellSize * 1.6
        val y = odoY + sin(heading) * cellSize * 1.6
        return visited[cellKey(x, y)] ?: 0
    }

    private fun cellKey(x: Double, y: Double): Long {
        val cx = floor(x / cellSize).toInt()
        val cy = floor(y / cellSize).toInt()
        return (cx.toLong() shl 32) or (cy.toLong() and 0xffffffffL)
    }

    // ---- helpers -------------------------------------------------------------------------------

    private fun drive(l: Double, r: Double) = api.perform(SetVelocityCommand(api.actuator, l, r))

    private fun isRed(c: Color) = c.red > 0.55 && c.green < 0.35 && c.blue < 0.35

    /** Signed angle of [target] relative to [from], in (-π, π]. */
    private fun angleDiff(target: Double, from: Double): Double {
        val d = target - from
        return atan2(sin(d), cos(d))
    }
}
