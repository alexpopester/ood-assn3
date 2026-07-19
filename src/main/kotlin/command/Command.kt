package command

/**
 * The Command pattern: an action encapsulated as an object. [execute] performs it;
 * [undo] reverses whatever state [execute] changed.
 */
interface Command {
    fun execute()
    fun undo()
}
