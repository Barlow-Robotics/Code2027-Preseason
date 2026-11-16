package first.robot.subsystems;

import org.wpilib.command2.Command;
import org.wpilib.command2.SubsystemBase;

import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.sim.TalonFXSimState;
import first.robot.generated.TunerConstants;

import first.robot.Constants.ElevatorConstants;

/** Desktop elevator for the preseason controls lesson. */
public final class Elevator extends SubsystemBase {
    private final ElevatorSim sim = new ElevatorSim();
    private final TalonFX motor = new TalonFX(20, TunerConstants.kCANBus);

    public Elevator() {
        // TODO 7: Configure feedback, gains, and Motion Magic after the target-only checkpoint.

        // Setup Simulation
        // Sets the simulated motor model. This setup is prepared for you; leave it in place.
        motor.getSimState().setMotorType(TalonFXSimState.MotorType.KrakenX60);
    }

    public Command setHeightGoalCommand(double goal) {
        return runOnce(() -> {
            // TODO 3: Add a target field and call a private method to clamp and save the goal.
        });
    }

    @Override
    public void periodic() {
        // TODO 6: Publish the goal in meters.
    }

    @Override
    public void simulationPeriodic() {
        // Updates the simulated mechanism and sends its position back to the Talon.
        sim.step(motor.getSimState());
    }
}
