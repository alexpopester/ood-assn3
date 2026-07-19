import command.Command
import command.CommandInvoker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CommandInvokerTest {

    private class TrackingCommand : Command {
        val log = mutableListOf<String>()
        override fun execute() { log.add("execute") }
        override fun undo()    { log.add("undo") }
    }

    @Test
    fun `run executes command and enables undo`() {
        val invoker = CommandInvoker()
        val cmd = TrackingCommand()
        assertFalse(invoker.canUndo())
        invoker.run(cmd)
        assertEquals(listOf("execute"), cmd.log)
        assertTrue(invoker.canUndo())
        assertFalse(invoker.canRedo())
    }

    @Test
    fun `undo calls undo on command and enables redo`() {
        val invoker = CommandInvoker()
        val cmd = TrackingCommand()
        invoker.run(cmd)
        invoker.undo()
        assertEquals(listOf("execute", "undo"), cmd.log)
        assertFalse(invoker.canUndo())
        assertTrue(invoker.canRedo())
    }

    @Test
    fun `redo re-executes and re-enables undo`() {
        val invoker = CommandInvoker()
        val cmd = TrackingCommand()
        invoker.run(cmd)
        invoker.undo()
        invoker.redo()
        assertEquals(listOf("execute", "undo", "execute"), cmd.log)
        assertTrue(invoker.canUndo())
        assertFalse(invoker.canRedo())
    }

    @Test
    fun `run clears redo stack`() {
        val invoker = CommandInvoker()
        val cmd1 = TrackingCommand()
        val cmd2 = TrackingCommand()
        invoker.run(cmd1)
        invoker.undo()
        assertTrue(invoker.canRedo())
        invoker.run(cmd2)
        assertFalse(invoker.canRedo())
    }

    @Test
    fun `undo on empty stack is no-op`() {
        val invoker = CommandInvoker()
        invoker.undo()
        assertFalse(invoker.canUndo())
        assertFalse(invoker.canRedo())
    }

    @Test
    fun `redo on empty stack is no-op`() {
        val invoker = CommandInvoker()
        invoker.redo()
        assertFalse(invoker.canUndo())
        assertFalse(invoker.canRedo())
    }

    @Test
    fun `multiple commands undo in reverse order`() {
        val invoker = CommandInvoker()
        val a = TrackingCommand()
        val b = TrackingCommand()
        invoker.run(a)
        invoker.run(b)
        invoker.undo() // undoes b
        invoker.undo() // undoes a
        assertEquals(listOf("execute", "undo"), b.log)
        assertEquals(listOf("execute", "undo"), a.log)
    }
}
