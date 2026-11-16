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

import first.robot.Constants.ArmConstants;
import first.robot.generated.TunerConstants;

/** Simulated arm used in the preseason controls lesson. */
public final class Arm extends SubsystemBase {
    public static final double LOW_DEGREES = 0.0;
    public static final double HIGH_DEGREES = 65.0;

    private final ArmSim sim = new ArmSim();
    private final TalonFX motor = new TalonFX(21, TunerConstants.kCANBus);
    private final MotionMagicVoltage positionRequest = new MotionMagicVoltage(0.0);
    private final TelemetryTable telemetry = Telemetry.getTable("Arm");
    private double targetDegrees = LOW_DEGREES;

    public Arm() {
        var config = new TalonFXConfiguration();
        config.Feedback.SensorToMechanismRatio = ArmConstants.GEAR_RATIO;
        config.Slot0.kP = 45.0;
        config.Slot0.kV = 15.0;
        config.Slot0.kG = 0.23;
        config.Slot0.GravityType = GravityTypeValue.Arm_Cosine;
        config.MotionMagic.MotionMagicCruiseVelocity = 0.45;
        config.MotionMagic.MotionMagicAcceleration = 1.5;
        var status = motor.getConfigurator().apply(config);
        if (!status.isOK()) {
            throw new IllegalStateException("Arm Talon FX configuration failed: " + status);
        }
        motor.getSimState().setMotorType(TalonFXSimState.MotorType.KrakenX60);
    }

    public Command setAngleGoalCommand(double degrees) {
        return runOnce(() -> setAngleGoal(degrees));
    }

    private void setAngleGoal(double degrees) {
        targetDegrees = Math.clamp(degrees, ArmConstants.MIN_DEGREES, ArmConstants.MAX_DEGREES);
        motor.setControl(positionRequest.withPosition(targetDegrees / 360.0));
    }

    @Override
    public void periodic() {
        double measuredTurns = motor.getPosition().getValueAsDouble();
        telemetry.log("Target_deg", targetDegrees);
        telemetry.log("Actual_deg", measuredTurns * 360.0);
    }

    @Override
    public void simulationPeriodic() {
        sim.step(motor.getSimState());
    }
}
