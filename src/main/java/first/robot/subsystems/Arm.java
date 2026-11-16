package first.robot.subsystems;

import org.wpilib.command2.Command;
import org.wpilib.command2.SubsystemBase;

import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.sim.TalonFXSimState;
import first.robot.generated.TunerConstants;

import first.robot.Constants.ArmConstants;

/** Desktop arm for the preseason controls lesson. */
public final class Arm extends SubsystemBase {
    public static final double LOW_DEGREES = 0.0;
    public static final double HIGH_DEGREES = 65.0;

    private final ArmSim sim = new ArmSim();
    private final TalonFX motor = new TalonFX(21, TunerConstants.kCANBus);

    public Arm() {
        // TODO 5: Configure feedback, gains, and Motion Magic after the target-only checkpoint.

        // Setup Simulation
        // Sets the simulated motor model. This setup is prepared for you; leave it in place.
        motor.getSimState().setMotorType(TalonFXSimState.MotorType.KrakenX60);
    }

    public Command setAngleGoalCommand(double goal) {
        return runOnce(() -> {
            // TODO 2: Add a target field and call a private method to clamp and save the goal.
        });
    }

    @Override
    public void periodic() {
        // TODO 4: Publish the goal in degrees.
    }

    @Override
    public void simulationPeriodic() {
        // Updates the simulated mechanism and sends its position back to the Talon.
        sim.step(motor.getSimState());
    }
}
