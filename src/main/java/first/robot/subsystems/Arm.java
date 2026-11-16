package first.robot.subsystems;

import org.wpilib.command2.Command;
import org.wpilib.command2.SubsystemBase;

import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.sim.TalonFXSimState;
import first.robot.generated.TunerConstants;

import first.robot.Constants.ArmConstants;

/** Desktop arm for the preseason controls lesson. */
public final class Arm extends SubsystemBase {
    private final ArmSim sim = new ArmSim();
    private final TalonFX motor = new TalonFX(ArmConstants.MOTOR_CAN_ID, TunerConstants.kCANBus);
    // TODO 3: Add a field to store the target position.

    public Arm() {
        // TODO 7: Configure feedback, gains, and Motion Magic after the target-only checkpoint.

        // Setup Simulation
        // Sets the simulated motor model. This setup is prepared for you; leave it in place.
        motor.getSimState().setMotorType(TalonFXSimState.MotorType.KrakenX60);
    }

    public Command setAngleGoalCommand(double goal) {
        return runOnce(() -> {
            // TODO 3: Call the private function that saves the goal.
        });
    }

    // TODO 3: Add the private setAngleGoal(double goal) function here.

    @Override
    public void periodic() {
        // TODO 6: Publish the goal in degrees.
    }

    @Override
    public void simulationPeriodic() {
        // Updates the simulated mechanism and sends its position back to the Talon.
        sim.step(motor.getSimState());
    }
}
