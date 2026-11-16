package first.robot.subsystems;

import org.wpilib.command2.Command;
import org.wpilib.command2.SubsystemBase;
// TODO 6a: Add the telemetry imports.

import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.sim.TalonFXSimState;
import first.robot.generated.TunerConstants;

import first.robot.Constants.ArmConstants;

/** Desktop arm for the preseason controls lesson. */
public final class Arm extends SubsystemBase {
    private final ArmSim sim = new ArmSim();
    private final TalonFX motor = new TalonFX(ArmConstants.MOTOR_CAN_ID, TunerConstants.kCANBus);
    // TODO 3a: Add a field to store the target position.
    // TODO 6b: Add a telemetry table field.

    public Arm() {
        // TODO 7: Configure feedback, gains, and Motion Magic after the first graph checkpoint.

        // Setup Simulation
        // Sets the simulated motor model. This setup is prepared for you; leave it in place.
        motor.getSimState().setMotorType(TalonFXSimState.MotorType.KrakenX60);
    }

    public Command setAngleGoalCommand(double goal) {
        return runOnce(() -> {
            // TODO 3c: Call the private function that saves the goal.
        });
    }

    // TODO 3b: Add the private setAngleGoal(double goal) function here.

    @Override
    public void periodic() {
        // TODO 6c: Publish target and actual position in degrees.
    }

    @Override
    public void simulationPeriodic() {
        // Updates the simulated mechanism and sends its position back to the Talon.
        sim.step(motor.getSimState());
    }
}
