package first.robot.subsystems;

import org.wpilib.command2.Command;
import org.wpilib.command2.SubsystemBase;

/** Desktop arm for the preseason controls lesson. */
public final class Arm extends SubsystemBase {
    public static final double LOW_DEGREES = 0.0;
    public static final double HIGH_DEGREES = 65.0;

    private final ArmSim sim = new ArmSim();

    // TODO 1: Add the TalonFX, configure it, and create a reusable position request.

    public Command goToDegrees(double goal) {
        return runOnce(() -> {
            // TODO 1: Clamp and save the goal, convert it to mechanism turns, and send it.
        });
    }

    @Override
    public void periodic() {
        // TODO 2: Publish the goal and TalonFX position in degrees.
    }

    @Override
    public void simulationPeriodic() {
        // TODO 1: Replace this zero-voltage preview with sim.step(motor.getSimState()).
        sim.update(0.0);
    }
}
