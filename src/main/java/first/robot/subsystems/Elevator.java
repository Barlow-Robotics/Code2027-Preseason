package first.robot.subsystems;

import org.wpilib.command2.Command;
import org.wpilib.command2.SubsystemBase;
import org.wpilib.telemetry.Telemetry;
import org.wpilib.telemetry.TelemetryTable;

import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.MotionMagicVoltage;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.GravityTypeValue;
import com.ctre.phoenix6.sim.TalonFXSimState;

import first.robot.Constants.ElevatorConstants;
import first.robot.generated.TunerConstants;

/** Simulated elevator used in the preseason controls lesson. */
public final class Elevator extends SubsystemBase {
    private final ElevatorSim sim = new ElevatorSim();
    private final TalonFX motor = new TalonFX(ElevatorConstants.MOTOR_CAN_ID, TunerConstants.kCANBus);
    private final MotionMagicVoltage positionRequest = new MotionMagicVoltage(0.0);
    private final TelemetryTable telemetry = Telemetry.getTable("Elevator");
    private double targetMeters = ElevatorConstants.LOW_METERS;

    public Elevator() {
        var config = new TalonFXConfiguration();
        config.Feedback.SensorToMechanismRatio = ElevatorConstants.GEAR_RATIO;
        config.Slot0.kP = 5.0;
        config.Slot0.kV = 2.4;
        config.Slot0.kG = 0.28;
        config.Slot0.GravityType = GravityTypeValue.Elevator_Static;
        config.MotionMagic.MotionMagicCruiseVelocity = 2.0;
        config.MotionMagic.MotionMagicAcceleration = 6.0;
        var status = motor.getConfigurator().apply(config);
        if (!status.isOK()) {
            throw new IllegalStateException("Elevator Talon FX configuration failed: " + status);
        }
        motor.getSimState().setMotorType(TalonFXSimState.MotorType.KrakenX60);
    }

    public Command setHeightGoalCommand(double meters) {
        return runOnce(() -> setHeightGoal(meters));
    }

    private void setHeightGoal(double meters) {
        targetMeters = Math.clamp(meters, ElevatorConstants.MIN_METERS, ElevatorConstants.MAX_METERS);
        motor.setControl(positionRequest.withPosition(targetMeters / ElevatorConstants.METERS_PER_DRUM_TURN));
    }

    @Override
    public void periodic() {
        double measuredTurns = motor.getPosition().getValueAsDouble();
        telemetry.log("Target_m", targetMeters);
        telemetry.log("Actual_m", measuredTurns * ElevatorConstants.METERS_PER_DRUM_TURN);
    }

    @Override
    public void simulationPeriodic() {
        sim.step(motor.getSimState());
    }
}
