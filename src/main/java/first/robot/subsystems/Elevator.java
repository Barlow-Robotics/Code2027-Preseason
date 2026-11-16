package first.robot.subsystems;

import org.wpilib.command2.Command;
import org.wpilib.command2.SubsystemBase;

import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.sim.TalonFXSimState;
import first.robot.generated.TunerConstants;

import first.robot.Constants.ElevatorConstants;

/** Desktop elevator for the preseason controls lesson. */
public final class Elevator extends SubsystemBase {
    public static final double LOW_METERS = 0.0;
    public static final double HIGH_METERS = 0.65;

    private final ElevatorSim sim = new ElevatorSim();
    private final TalonFX motor = new TalonFX(20, TunerConstants.kCANBus);

    public Elevator() {
        // TODO 5: Configure feedback, gains, and Motion Magic after the target-only checkpoint.

        // Setup Simulation
        // Sets the simulated motor model. This setup is prepared for you; leave it in place.
        motor.getSimState().setMotorType(TalonFXSimState.MotorType.KrakenX60);
    }

    public Command setHeightGoalCommand(double goal) {
        return runOnce(() -> {
            // TODO 2: Add a target field and call a private method to clamp and save the goal.
        });
    }

    @Override
    public void periodic() {
        // TODO 4: Publish the goal in meters.
    }

    @Override
    public void simulationPeriodic() {
        // Prepared simulation connection: no motor output until we send a control request.
        sim.step(motor.getSimState());
    }
}
