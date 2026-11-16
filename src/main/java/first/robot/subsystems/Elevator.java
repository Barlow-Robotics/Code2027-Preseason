package first.robot.subsystems;

import org.wpilib.command2.Command;
import org.wpilib.command2.SubsystemBase;

/** Desktop elevator for the preseason controls lesson. */
public final class Elevator extends SubsystemBase {
    public static final double LOW_METERS = 0.0;
    public static final double HIGH_METERS = 0.65;

    private final ElevatorSim sim = new ElevatorSim();

    // TODO 1: Add the TalonFX, configure it, and create a reusable position request.

    public Command goToMeters(double goal) {
        return runOnce(() -> {
            // TODO 1: Clamp and save the goal, convert it to mechanism turns, and send it.
        });
    }

    @Override
    public void periodic() {
        // TODO 2: Publish the goal and TalonFX position in meters.
    }

    @Override
    public void simulationPeriodic() {
        // TODO 1: Replace this zero-voltage preview with sim.step(motor.getSimState()).
        sim.update(0.0);
    }
}
