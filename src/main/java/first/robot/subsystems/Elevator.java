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
    public enum Level {
        BOTTOM(ElevatorConstants.BOTTOM_METERS),
        MID(ElevatorConstants.MID_METERS),
        TOP(ElevatorConstants.TOP_METERS);

        private final double heightMeters;

        Level(double heightMeters) {
            this.heightMeters = heightMeters;
        }

        public Level increase() {
            return switch (this) {
                case BOTTOM -> MID;
                case MID, TOP -> TOP;
            };
        }

        public Level decrease() {
            return switch (this) {
                case TOP -> MID;
                case MID, BOTTOM -> BOTTOM;
            };
        }
    }

    private final ElevatorSim sim = new ElevatorSim();
    private final TalonFX motor = new TalonFX(ElevatorConstants.MOTOR_CAN_ID, TunerConstants.kCANBus);
    private Level selectedLevel = Level.BOTTOM;
    private double targetMeters = ElevatorConstants.BOTTOM_METERS;
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
        config.MotionMagic.MotionMagicCruiseVelocity = ElevatorConstants.CRUISE_METERS_PER_SECOND
            / ElevatorConstants.METERS_PER_DRUM_TURN;
        config.MotionMagic.MotionMagicAcceleration = ElevatorConstants.ACCELERATION_METERS_PER_SECOND_SQUARED
            / ElevatorConstants.METERS_PER_DRUM_TURN;

        var status = motor.getConfigurator().apply(config);
        if (!status.isOK()) {
            throw new IllegalStateException("Elevator Talon FX configuration failed: " + status);
        }

        // Setup Simulation
        // Sets the simulated motor model. This setup is prepared for you; leave it in place.
        motor.getSimState().setMotorType(TalonFXSimState.MotorType.KrakenX60);
    }

    public Command goToLevelCommand(Level level) {
        return new MoveToLevelCommand(level);
    }

    public Command increaseLevelCommand() {
        return defer(() -> goToLevelCommand(selectedLevel.increase()));
    }

    public Command decreaseLevelCommand() {
        return defer(() -> goToLevelCommand(selectedLevel.decrease()));
    }

    private final class MoveToLevelCommand extends Command {
        private final Level level;

        private MoveToLevelCommand(Level level) {
            this.level = level;
            addRequirements(Elevator.this);
        }

        @Override
        public void initialize() {
            selectedLevel = level;
            movementActive = true;
            setHeightGoal(level.heightMeters);
        }

        @Override
        public boolean isFinished() {
            double heightMeters = motor.getPosition().getValueAsDouble()
                * ElevatorConstants.METERS_PER_DRUM_TURN;
            double velocityMetersPerSecond = motor.getVelocity().getValueAsDouble()
                * ElevatorConstants.METERS_PER_DRUM_TURN;
            return Math.abs(heightMeters - level.heightMeters)
                    < ElevatorConstants.POSITION_TOLERANCE_METERS
                && Math.abs(velocityMetersPerSecond)
                    < ElevatorConstants.VELOCITY_TOLERANCE_METERS_PER_SECOND;
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
        telemetry.log("SelectedLevel", selectedLevel.name());
        telemetry.log("SelectedHeight_m", selectedLevel.heightMeters);
        telemetry.log("Velocity_mps", motor.getVelocity().getValueAsDouble()
            * ElevatorConstants.METERS_PER_DRUM_TURN);
    }

    @Override
    public void simulationPeriodic() {
        // Updates the simulated mechanism and sends its position back to the Talon.
        sim.step(motor.getSimState());
    }
}
