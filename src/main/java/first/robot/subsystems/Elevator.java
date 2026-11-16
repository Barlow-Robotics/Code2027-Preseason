package first.robot.subsystems;

import org.wpilib.command2.Command;
import org.wpilib.command2.SubsystemBase;

/** Desktop elevator for the preseason controls lesson. */
public final class Elevator extends SubsystemBase {
    public static final double LOW_METERS = 0.0;
    public static final double HIGH_METERS = 0.65;

    private final ElevatorSim sim = new ElevatorSim();

    public Command setHeightGoalCommand(double goal) {
        return runOnce(() -> {
            // TODO 1: Add a target field and call a private method to clamp and save the goal.
        });
    }

    @Override
    public void periodic() {
        // TODO 3: Publish the goal in meters.
    }

    @Override
    public void simulationPeriodic() {
        // TODO 4: Connect the Talon FX simulation feedback after the target-only checkpoint.
        sim.update(0.0);
    }
}
