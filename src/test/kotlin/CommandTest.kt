import command.Command
import command.CommandInvoker
import command.CompositeCommand
import command.RobotActuator
import command.SetVelocityCommand
import kotlin.test.Test
import kotlin.test.assertEquals

class CommandTest {

    /** Minimal stand-in for the robot's actuator — records the current track velocities. */
    private class FakeActuator(
        override var leftTrackVelocity: Double = 0.0,
        override var rightTrackVelocity: Double = 0.0,
    ) : RobotActuator {
        override fun setTrackVelocities(left: Double, right: Double) {
            leftTrackVelocity = left
            rightTrackVelocity = right
        }
    }

    // ---- SetVelocityCommand ---------------------------------------------------------------------

    @Test
    fun `execute sets both track velocities`() {
        val actuator = FakeActuator()
        SetVelocityCommand(actuator, 50.0, -30.0).execute()
        assertEquals(50.0, actuator.leftTrackVelocity)
        assertEquals(-30.0, actuator.rightTrackVelocity)
    }

    @Test
    fun `undo restores the velocities captured at execute time`() {
        val actuator = FakeActuator(leftTrackVelocity = 20.0, rightTrackVelocity = 80.0)
        val cmd = SetVelocityCommand(actuator, 100.0, 100.0)

        cmd.execute()
        assertEquals(100.0, actuator.leftTrackVelocity)
        assertEquals(100.0, actuator.rightTrackVelocity)

        cmd.undo()
        assertEquals(20.0, actuator.leftTrackVelocity, "left should return to its pre-execute value")
        assertEquals(80.0, actuator.rightTrackVelocity, "right should return to its pre-execute value")
    }

    @Test
    fun `undo without execute restores the command's default previous values`() {
        val actuator = FakeActuator(leftTrackVelocity = 40.0, rightTrackVelocity = 40.0)
        // prevLeft/prevRight default to 0.0 until execute() captures the real values.
        SetVelocityCommand(actuator, 100.0, 100.0).undo()
        assertEquals(0.0, actuator.leftTrackVelocity)
        assertEquals(0.0, actuator.rightTrackVelocity)
    }

    @Test
    fun `re-executing captures the latest state so undo is idempotent to that snapshot`() {
        val actuator = FakeActuator()
        val a = SetVelocityCommand(actuator, 10.0, 10.0)
        val b = SetVelocityCommand(actuator, 60.0, 60.0)

        a.execute()          // prev(0,0) -> (10,10)
        b.execute()          // prev(10,10) -> (60,60)
        b.undo()             // restores (10,10)
        assertEquals(10.0, actuator.leftTrackVelocity)
        assertEquals(10.0, actuator.rightTrackVelocity)
        a.undo()             // restores (0,0)
        assertEquals(0.0, actuator.leftTrackVelocity)
        assertEquals(0.0, actuator.rightTrackVelocity)
    }

    // ---- CompositeCommand -----------------------------------------------------------------------

    @Test
    fun `composite executes its children in order`() {
        val order = mutableListOf<String>()
        val composite = CompositeCommand(listOf(
            recordingCommand(order, "a"),
            recordingCommand(order, "b"),
            recordingCommand(order, "c"),
        ))
        composite.execute()
        assertEquals(listOf("execute-a", "execute-b", "execute-c"), order)
    }

    @Test
    fun `composite undoes its children in reverse order`() {
        val order = mutableListOf<String>()
        val composite = CompositeCommand(listOf(
            recordingCommand(order, "a"),
            recordingCommand(order, "b"),
            recordingCommand(order, "c"),
        ))
        composite.undo()
        assertEquals(listOf("undo-c", "undo-b", "undo-a"), order)
    }

    @Test
    fun `composite of velocity commands restores original state after execute then undo`() {
        val actuator = FakeActuator(leftTrackVelocity = 5.0, rightTrackVelocity = 5.0)
        val composite = CompositeCommand(listOf(
            SetVelocityCommand(actuator, 30.0, -30.0),   // turn
            SetVelocityCommand(actuator, 90.0, 90.0),    // advance
        ))
        composite.execute()
        assertEquals(90.0, actuator.leftTrackVelocity)
        assertEquals(90.0, actuator.rightTrackVelocity)

        composite.undo()
        assertEquals(5.0, actuator.leftTrackVelocity, "undo must unwind both children back to the start")
        assertEquals(5.0, actuator.rightTrackVelocity)
    }

    // ---- Command + Invoker together -------------------------------------------------------------

    @Test
    fun `invoker undo then redo round-trips actuator state`() {
        val actuator = FakeActuator(leftTrackVelocity = 15.0, rightTrackVelocity = 25.0)
        val invoker = CommandInvoker()

        invoker.run(SetVelocityCommand(actuator, 100.0, 100.0))
        assertEquals(100.0, actuator.leftTrackVelocity)

        invoker.undo()
        assertEquals(15.0, actuator.leftTrackVelocity)
        assertEquals(25.0, actuator.rightTrackVelocity)

        invoker.redo()
        assertEquals(100.0, actuator.leftTrackVelocity)
        assertEquals(100.0, actuator.rightTrackVelocity)
    }

    private fun recordingCommand(log: MutableList<String>, tag: String) = object : Command {
        override fun execute() { log.add("execute-$tag") }
        override fun undo()    { log.add("undo-$tag") }
    }
}
