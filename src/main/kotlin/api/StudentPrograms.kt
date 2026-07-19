package api

/**
 * The one place programs are registered with the system. Each program you register shows up in the
 * "Program" dropdown and can be launched with "Run Program".
 *
 * TODO(student): write one or more [RobotProgram] implementations. Each one, in `startProgram`,
 * subscribes to the sensors it needs (`robot.sensors.…`) and issues commands in the observer
 * callbacks; in `stopProgram` it unsubscribes and stops the robot. Then register them here, e.g.:
 *
 *     registry.register(MyLineFollowerProgram())
 *     registry.register(MyBallFinderProgram())
 *
 * Until you register a program, the dropdown shows "(no programs registered)".
 */
object StudentPrograms {
    fun registerAll(registry: ProgramRegistry) {
        registry.register(LineFollowerProgram())
        registry.register(HeatSeekerProgram())
        registry.register(BallFinderProgram())
    }
}
