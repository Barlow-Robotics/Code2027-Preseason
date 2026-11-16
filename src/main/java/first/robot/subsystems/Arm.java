package first.robot.subsystems;

import org.wpilib.command2.Command;
import org.wpilib.command2.SubsystemBase;

import first.robot.Constants.ArmConstants;

/** Desktop arm for the preseason controls lesson. */
public final class Arm extends SubsystemBase {
    public static final double LOW_DEGREES = 0.0;
    public static final double HIGH_DEGREES = 65.0;

    private final ArmSim sim = new ArmSim();

    public Command setAngleGoalCommand(double goal) {
        return runOnce(() -> {
            // TODO 1: Add a target field and call a private method to clamp and save the goal.
        });
    }

    @Override
    public void periodic() {
        // TODO 3: Publish the goal in degrees.
    }

    @Override
    public void simulationPeriodic() {
        // TODO 4: Connect the Talon FX simulation feedback after the target-only checkpoint.
        sim.update(0.0);
    }
}
