package first.robot.subsystems;

import org.wpilib.command2.Command;
import org.wpilib.command2.SubsystemBase;
import org.wpilib.telemetry.Telemetry;
import org.wpilib.telemetry.TelemetryTable;
import org.wpilib.tunable.TunableDouble;
import org.wpilib.tunable.TunableBoolean;
import com.ctre.phoenix6.configs.Slot0Configs;
import org.wpilib.tunable.Tunables;
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
    private final TunableDouble bottomMeters =
        Tunables.addDouble("Elevator/Targets/Bottom_m", ElevatorConstants.BOTTOM_METERS);
    private final TunableDouble midMeters =
        Tunables.addDouble("Elevator/Targets/Mid_m", ElevatorConstants.MID_METERS);
    private final TunableDouble topMeters =
        Tunables.addDouble("Elevator/Targets/Top_m", ElevatorConstants.TOP_METERS);
    private Level selectedLevel = Level.BOTTOM;
    private double targetMeters = ElevatorConstants.BOTTOM_METERS;
    private final TunableDouble kP = Tunables.addDouble("Elevator/Gains/kP", 5.0);
    private final TunableDouble kD = Tunables.addDouble("Elevator/Gains/kD", 0.0);
    private final TunableDouble kG = Tunables.addDouble("Elevator/Gains/kG", 0.28);
    private final TunableDouble kV = Tunables.addDouble("Elevator/Gains/kV", 2.4);
    private final TunableDouble kA = Tunables.addDouble("Elevator/Gains/kA", 0.0);
    private final TunableBoolean applyGains =
        Tunables.addBoolean("Elevator/Gains/Apply", false);
    private String gainStatus = "Using code defaults";
    private Slot0Configs appliedGains;
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
        appliedGains = config.Slot0;
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

    private double getHeightMeters(Level level) {
        double requested = switch (level) {
            case BOTTOM -> bottomMeters.get();
            case MID -> midMeters.get();
            case TOP -> topMeters.get();
        };
        if (!Double.isFinite(requested)) {
            requested = level.heightMeters;
        }
        return Math.clamp(requested, ElevatorConstants.MIN_METERS, ElevatorConstants.MAX_METERS);
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
        private double goalMeters;

        private MoveToLevelCommand(Level level) {
            this.level = level;
            addRequirements(Elevator.this);
        }

        @Override
        public void initialize() {
            selectedLevel = level;
            goalMeters = getHeightMeters(level);
            movementActive = true;
            setHeightGoal(goalMeters);
        }

        @Override
        public boolean isFinished() {
            double heightMeters = motor.getPosition().getValueAsDouble()
                * ElevatorConstants.METERS_PER_DRUM_TURN;
            double velocityMetersPerSecond = motor.getVelocity().getValueAsDouble()
                * ElevatorConstants.METERS_PER_DRUM_TURN;
            return Math.abs(heightMeters - goalMeters)
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

    private void updateGains() {
        if (!applyGains.get()) {
            return;
        }
        applyGains.set(false);
        double[] values = {kP.get(), kD.get(), kG.get(), kV.get(), kA.get()};
        for (double value : values) {
            if (!Double.isFinite(value) || value < 0.0 || value > 1000.0) {
                gainStatus = "Not applied: use numbers from 0 to 1000";
                return;
            }
        }
        var next = new Slot0Configs();
        next.GravityType = appliedGains.GravityType;
        next.kP = values[0];
        next.kD = values[1];
        next.kG = values[2];
        next.kV = values[3];
        next.kA = values[4];
        var status = motor.getConfigurator().apply(next, 0.020);
        if (status.isOK()) {
            appliedGains = next;
            gainStatus = "Applied";
        } else {
            gainStatus = "Not applied: " + status + "; switch Apply on to retry";
        }
    }

    @Override
    public void periodic() {
        updateGains();
        telemetry.log("Target_m", targetMeters);
        telemetry.log("ProfileTarget_m", motor.getClosedLoopReference().getValueAsDouble() * ElevatorConstants.METERS_PER_DRUM_TURN);
        telemetry.log("MotorVoltage_V", motor.getMotorVoltage().getValueAsDouble());
        telemetry.log("GainStatus", gainStatus);
        telemetry.log("Applied_kP", appliedGains.kP);
        telemetry.log("Applied_kD", appliedGains.kD);
        telemetry.log("Applied_kG", appliedGains.kG);
        telemetry.log("Applied_kV", appliedGains.kV);
        telemetry.log("Applied_kA", appliedGains.kA);
        double mechanismTurns = motor.getPosition().getValueAsDouble();
        telemetry.log("Actual_m", mechanismTurns * ElevatorConstants.METERS_PER_DRUM_TURN);
        telemetry.log("MovementActive", movementActive);
        telemetry.log("SelectedLevel", selectedLevel.name());
        telemetry.log("SelectedHeight_m", getHeightMeters(selectedLevel));
        telemetry.log("Velocity_mps", motor.getVelocity().getValueAsDouble()
            * ElevatorConstants.METERS_PER_DRUM_TURN);
    }

    @Override
    public void simulationPeriodic() {
        // Updates the simulated mechanism and sends its position back to the Talon.
        sim.step(motor.getSimState());
    }
}
