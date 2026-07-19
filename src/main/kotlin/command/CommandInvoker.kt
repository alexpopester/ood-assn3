package command

/**
 * The Invoker. It runs commands and keeps an undo/redo history — this is yours to implement.
 *
 * TODO(student): implement run / undo / redo using the two stacks below:
 *   - run:  execute the command, push it onto the undo stack, and clear the redo stack
 *   - undo: pop the last command, call undo() on it, and push it onto the redo stack
 *   - redo: pop from the redo stack, re-execute it, and push it back onto the undo stack
 *
 * Until you implement these, performing a command (from a button or a program) does nothing.
 */
class CommandInvoker {
    private val undoStack = ArrayDeque<Command>()
    private val redoStack = ArrayDeque<Command>()

    fun run(command: Command) {
        command.execute()
        undoStack.addLast(command)
        redoStack.clear()
    }

    fun undo() {
        if (!canUndo()) return
        val command = undoStack.removeLast()
        command.undo()
        redoStack.addLast(command)
    }

    fun redo() {
        if (!canRedo()) return
        val command = redoStack.removeLast()
        command.execute()
        undoStack.addLast(command)
    }

    fun canUndo() = undoStack.isNotEmpty()
    fun canRedo() = redoStack.isNotEmpty()
}
