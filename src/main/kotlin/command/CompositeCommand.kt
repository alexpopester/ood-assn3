package command

/** Composite pattern: batches several commands into one undoable unit. */
class CompositeCommand(private val commands: List<Command>) : Command {
    override fun execute() {
        commands.forEach { it.execute() }
    }

    override fun undo() {
        commands.reversed().forEach { it.undo() }
    }
}
