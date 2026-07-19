package api

/**
 * The "register program" API. Students call [register] to add their programs; the UI reads
 * [programs] to populate the program dropdown.
 */
interface ProgramRegistry {
    fun register(program: RobotProgram)
    fun programs(): List<RobotProgram>
}

/** Provided registry backing the dropdown. */
class DefaultProgramRegistry : ProgramRegistry {
    private val registered = mutableListOf<RobotProgram>()
    override fun register(program: RobotProgram) { registered.add(program) }
    override fun programs(): List<RobotProgram> = registered.toList()
}
