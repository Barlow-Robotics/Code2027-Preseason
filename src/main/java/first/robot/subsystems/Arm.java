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
    private final TunableDouble kP = Tunables.addDouble("Arm/Gains/kP", 45.0);
    private final TunableDouble kD = Tunables.addDouble("Arm/Gains/kD", 0.0);
    private final TunableDouble kG = Tunables.addDouble("Arm/Gains/kG", 0.23);
    private final TunableDouble kV = Tunables.addDouble("Arm/Gains/kV", 15.0);
    private final TunableDouble kA = Tunables.addDouble("Arm/Gains/kA", 0.0);
    private final TunableBoolean applyGains =
        Tunables.addBoolean("Arm/Gains/Apply", false);
    private String gainStatus = "Using code defaults";
    private Slot0Configs appliedGains;
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
        appliedGains = config.Slot0;
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
        telemetry.log("Target_deg", targetDegrees);
        telemetry.log("ProfileTarget_deg", motor.getClosedLoopReference().getValueAsDouble() * 360.0);
        telemetry.log("MotorVoltage_V", motor.getMotorVoltage().getValueAsDouble());
        telemetry.log("GainStatus", gainStatus);
        telemetry.log("Applied_kP", appliedGains.kP);
        telemetry.log("Applied_kD", appliedGains.kD);
        telemetry.log("Applied_kG", appliedGains.kG);
        telemetry.log("Applied_kV", appliedGains.kV);
        telemetry.log("Applied_kA", appliedGains.kA);
        double mechanismTurns = motor.getPosition().getValueAsDouble();
        telemetry.log("Actual_deg", mechanismTurns * 360.0);
    }

    @Override
    public void simulationPeriodic() {
        // Updates the simulated mechanism and sends its position back to the Talon.
        sim.step(motor.getSimState());
    }
}
