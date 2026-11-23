package first.robot.subsystems;

import org.wpilib.command2.Command;
import org.wpilib.command2.SubsystemBase;
import org.wpilib.telemetry.Telemetry;
import org.wpilib.telemetry.TelemetryTable;
import org.wpilib.tunable.TunableDouble;
import org.wpilib.tunable.Tunables;
import com.ctre.phoenix6.configs.TalonFXConfiguration;

import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.sim.TalonFXSimState;
import com.ctre.phoenix6.controls.MotionMagicVoltage;
import com.ctre.phoenix6.signals.GravityTypeValue;
import first.robot.generated.TunerConstants;

import first.robot.Constants.ArmConstants;

/** Desktop arm for the preseason controls lesson. */
public final class Arm extends SubsystemBase {
    public enum Preset { LOW, HIGH }

    private final TunableDouble lowDegrees =
        Tunables.addDouble("Arm/Targets/Low_deg", ArmConstants.LOW_DEGREES);
    private final TunableDouble highDegrees =
        Tunables.addDouble("Arm/Targets/High_deg", ArmConstants.HIGH_DEGREES);
    private final ArmSim sim = new ArmSim();
    private final TalonFX motor = new TalonFX(ArmConstants.MOTOR_CAN_ID, TunerConstants.kCANBus);
    private double targetDegrees = ArmConstants.LOW_DEGREES;
    private final MotionMagicVoltage positionRequest = new MotionMagicVoltage(0.0);
    private final TelemetryTable telemetry =
        Telemetry.getTable("Arm");

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

        // Setup Simulation
        // Sets the simulated motor model. This setup is prepared for you; leave it in place.
        motor.getSimState().setMotorType(TalonFXSimState.MotorType.KrakenX60);
    }

    private double getAngleDegrees(Preset preset) {
        double requested = preset == Preset.LOW ? lowDegrees.get() : highDegrees.get();
        if (!Double.isFinite(requested)) {
            requested = preset == Preset.LOW ? ArmConstants.LOW_DEGREES : ArmConstants.HIGH_DEGREES;
        }
        return Math.clamp(requested, ArmConstants.MIN_DEGREES, ArmConstants.MAX_DEGREES);
    }

    public Command setAngleGoalCommand(Preset preset) {
        return runOnce(() -> setAngleGoal(getAngleDegrees(preset)));
    }

    private void setAngleGoal(double goal) {
        targetDegrees = Math.clamp(goal, ArmConstants.MIN_DEGREES, ArmConstants.MAX_DEGREES);
        double goalTurns = targetDegrees / 360.0;
        motor.setControl(positionRequest.withPosition(goalTurns));
    }

    @Override
    public void periodic() {
        telemetry.log("Target_deg", targetDegrees);
        double mechanismTurns = motor.getPosition().getValueAsDouble();
        telemetry.log("Actual_deg", mechanismTurns * 360.0);
    }

    @Override
    public void simulationPeriodic() {
        // Updates the simulated mechanism and sends its position back to the Talon.
        sim.step(motor.getSimState());
    }
}
