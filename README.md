# Skid-Steer Robot Simulation — CS 5700 Homework 3

A 2D top-down simulation of a skid-steer (differential-drive) robot used to teach the
**Observer** and **Command** design patterns. See [PLAN.md](PLAN.md) for the full design.

> **This is the student starter.** The whole simulation engine, the JavaFX UI, the sensors,
> the three environments, and the application interfaces are provided and working. The
> **pattern code is left for you** as `// TODO(student)` stubs — the app compiles and runs
> from day one, but the robot won't move and the telemetry stays blank until you implement
> the patterns. Your job is the checklist below.

## Your tasks

1. **Observer pattern** — implement `observer/AbstractSubject` (subscribe / unsubscribe /
   notifyObservers), then in `ui/TelemetryPanel.bindTo` subscribe observers to the robot's
   sensors so the readouts go live.
2. **Command pattern** — design your own `command/Command` classes (you decide the set),
   implement `command/CommandInvoker` (run / undo / redo), and wire the drive buttons in
   `ui/ControlPanel` (and the keyboard in `app/RobotSimulationApp`) to perform them.
3. **Write & register a program** — implement one or more `api/RobotProgram`s (subscribe to
   the sensors you need in `startProgram`, act via commands in the callbacks, unsubscribe in
   `stopProgram`) and register them in `api/StudentPrograms` so they appear in the dropdown.

Search the code for `TODO(student)` to find every spot. See §10 of [PLAN.md](PLAN.md) for
the full deliverables and definition of done.

## Requirements

- JDK 25 is fetched automatically by the Gradle toolchain (Foojay resolver). JavaFX 25 is
  pulled from Maven Central by the `org.openjfx.javafxplugin`. No manual setup.

## Run

```bash
./gradlew run     # launch the simulation window
./gradlew test    # run the kinematics / sensor / pattern / integration tests
```

## Using the app

- **Environment** dropdown — switch between the three worlds:
  - *Obstacle Course* — find and touch the red ball (sonar + vision).
  - *Line Maze* — follow the winding line (line sensors).
  - *Temperature Gradient* — climb the heat map to the hot spot (temperature sensor).
- **Manual driving** — the ◄ Left / ▲ Forward / ▼ Back / ► Right / ■ Stop buttons (and the
  arrow keys + space) are meant to issue a **Command** through the robot API. **Reset**
  returns the robot to the start. *(These do nothing until you implement the Command pattern
  and wire them — task 2.)*
- **Program** dropdown + **Run Program** — launches a program you registered; **Stop** hands
  control back. *(Empty until you write and register a program — task 3.)*
- **Telemetry** (right) — sensor readouts. *(Blank "—" until you wire the observers — task 1.)*

## Package map

| Package        | Responsibility |
|----------------|----------------|
| `geometry`     | `Vector2`, `Pose`, `Rectangle`, `Ray` (ray/AABB + ray/circle). |
| `observer`     | `Observer`, `Subject`, `AbstractSubject` — the Observer pattern. |
| `command`      | `Command`, `CommandInvoker`, `RobotActuator` (receiver) — the Command pattern. |
| `sensor`       | `Sensor<T>` (a Subject) + `Sonar`, `Vision`, `Temperature`, `Line`, and `RobotSensors` (the suite programs subscribe to). |
| `environment`  | `Environment` + the three worlds and their features. |
| `model`        | `Robot` — skid-steer kinematics, collision, sensor suite. |
| `api`          | `RobotApi`, `RobotProgram` (subscribe/act via `startProgram`/`stopProgram`), `ProgramRegistry`, `StudentPrograms` (register yours here). |
| `sim`          | `Simulation`, `ProgramRunner`, `EnvironmentCatalog`. |
| `ui`           | JavaFX canvas + control / program / telemetry panels. |
| `app`          | `RobotSimulationApp` — wiring and the animation loop. |
# ood-assn3
