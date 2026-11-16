package first.robot.subsystems;

import org.wpilib.system.RobotController;
import org.wpilib.math.system.DCMotor;
import org.wpilib.math.util.Units;
import org.wpilib.simulation.SingleJointedArmSim;
import org.wpilib.smartdashboard.Mechanism2d;
import org.wpilib.smartdashboard.MechanismLigament2d;
import org.wpilib.telemetry.Telemetry;
import org.wpilib.telemetry.TelemetryTable;
import org.wpilib.util.Color;
import org.wpilib.util.Color8Bit;

import com.ctre.phoenix6.sim.TalonFXSimState;

import first.robot.Constants.ArmConstants;

/** Physics and Mechanism2d drawing for the desktop arm. */
final class ArmSim {
    private final SingleJointedArmSim physics = new SingleJointedArmSim(
        DCMotor.getKrakenX60(1), ArmConstants.GEAR_RATIO,
        SingleJointedArmSim.estimateMOI(ArmConstants.ARM_LENGTH_METERS, 3.0), ArmConstants.ARM_LENGTH_METERS,
        Units.degreesToRadians(ArmConstants.MIN_DEGREES), Units.degreesToRadians(ArmConstants.MAX_DEGREES), true, 0.0);

    private final TelemetryTable telemetry = Telemetry.getTable("Arm");
    private final Mechanism2d drawing = new Mechanism2d(1.2, 1.2);
    private final MechanismLigament2d link = drawing.getRoot("Pivot", 0.45, 0.40)
        .append(new MechanismLigament2d("Arm", ArmConstants.ARM_LENGTH_METERS, 0.0, 6.0,
            new Color8Bit(Color.ORANGE)));

    void step(TalonFXSimState motor) {
        motor.setSupplyVoltage(RobotController.getBatteryVoltage());
        update(motor.getMotorVoltage());
        // Phoenix simulation needs raw motor rotations, before SensorToMechanismRatio is applied.
        motor.setRawRotorPosition(Units.radiansToRotations(physics.getAngle()) * ArmConstants.GEAR_RATIO);
        motor.setRotorVelocity(Units.radiansToRotations(physics.getVelocity()) * ArmConstants.GEAR_RATIO);
    }

    void update(double motorVolts) {
        physics.setInputVoltage(motorVolts);
        physics.update(0.020);
        link.setAngle(Units.radiansToDegrees(physics.getAngle()));
        telemetry.log("Mechanism", drawing);
    }
}
