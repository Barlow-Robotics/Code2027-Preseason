package first.robot.subsystems;

import org.wpilib.command2.Command;
import org.wpilib.command2.SubsystemBase;
import org.wpilib.telemetry.Telemetry;
import org.wpilib.telemetry.TelemetryTable;
import com.ctre.phoenix6.configs.TalonFXConfiguration;

import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.sim.TalonFXSimState;
import com.ctre.phoenix6.controls.MotionMagicVoltage;
import com.ctre.phoenix6.controls.PositionVoltage;
import com.ctre.phoenix6.signals.GravityTypeValue;
import first.robot.generated.TunerConstants;

import first.robot.Constants.ElevatorConstants;

/** Desktop elevator for the preseason controls lesson. */
public final class Elevator extends SubsystemBase {
    private final ElevatorSim sim = new ElevatorSim();
    private final TalonFX motor = new TalonFX(ElevatorConstants.MOTOR_CAN_ID, TunerConstants.kCANBus);
    private double targetMeters = ElevatorConstants.LOW_METERS;
    private final MotionMagicVoltage positionRequest = new MotionMagicVoltage(0.0);
    private final PositionVoltage holdRequest = new PositionVoltage(0.0);
    private boolean movementActive = false;
    private final TelemetryTable telemetry =
        Telemetry.getTable("Elevator");

    public Elevator() {
        var config = new TalonFXConfiguration();
        config.Feedback.SensorToMechanismRatio = ElevatorConstants.GEAR_RATIO;
        config.Slot0.kP = 5.0;
        config.Slot0.kV = 2.4;
        config.Slot0.kG = 0.28;
        config.Slot0.GravityType = GravityTypeValue.Elevator_Static;
        config.MotionMagic.MotionMagicCruiseVelocity = ElevatorConstants.CRUISE_ROTATIONS_PER_SECOND;
        config.MotionMagic.MotionMagicAcceleration = ElevatorConstants.ACCELERATION_ROTATIONS_PER_SECOND_SQUARED;

        var status = motor.getConfigurator().apply(config);
        if (!status.isOK()) {
            throw new IllegalStateException("Elevator Talon FX configuration failed: " + status);
        }

        // Setup Simulation
        // Sets the simulated motor model. This setup is prepared for you; leave it in place.
        motor.getSimState().setMotorType(TalonFXSimState.MotorType.KrakenX60);
    }

    public Command setHeightGoalCommand(double goal) {
        return new MoveToHeightCommand(goal);
    }

    private final class MoveToHeightCommand extends Command {
        private final double goal;

        private MoveToHeightCommand(double goal) {
            this.goal = goal;
            addRequirements(Elevator.this);
        }

        @Override
        public void initialize() {
            movementActive = true;
            setHeightGoal(goal);
        }

        @Override
        public boolean isFinished() {
            return false;
        }

        @Override
        public void end(boolean interrupted) {
            stopAndHold();
            movementActive = false;
        }
    }

    private void stopAndHold() {
        double currentTurns = motor.getPosition().getValueAsDouble();
        targetMeters = currentTurns * ElevatorConstants.METERS_PER_DRUM_TURN;
        motor.setControl(holdRequest.withPosition(currentTurns));
    }

    private void setHeightGoal(double goal) {
        targetMeters = Math.clamp(goal, ElevatorConstants.MIN_METERS, ElevatorConstants.MAX_METERS);
        double goalTurns = targetMeters / ElevatorConstants.METERS_PER_DRUM_TURN;
        motor.setControl(positionRequest.withPosition(goalTurns));
    }

    @Override
    public void periodic() {
        telemetry.log("Target_m", targetMeters);
        double mechanismTurns = motor.getPosition().getValueAsDouble();
        telemetry.log("Actual_m", mechanismTurns * ElevatorConstants.METERS_PER_DRUM_TURN);
        telemetry.log("MovementActive", movementActive);
        telemetry.log("Velocity_mps", motor.getVelocity().getValueAsDouble()
            * ElevatorConstants.METERS_PER_DRUM_TURN);
    }

    @Override
    public void simulationPeriodic() {
        // Updates the simulated mechanism and sends its position back to the Talon.
        sim.step(motor.getSimState());
    }
}
