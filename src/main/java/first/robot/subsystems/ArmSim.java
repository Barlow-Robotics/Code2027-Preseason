package first.robot.subsystems;

import org.wpilib.system.RobotController;
import org.wpilib.math.system.DCMotor;
import org.wpilib.math.util.Units;
import org.wpilib.simulation.SingleJointedArmSim;
import org.wpilib.telemetry.Telemetry;
import org.wpilib.telemetry.TelemetryTable;

import com.ctre.phoenix6.sim.TalonFXSimState;

import first.robot.Constants.ArmConstants;

/** Physics and Mechanism2d drawing for the desktop arm. */
final class ArmSim {
    // Physical gearbox in this model, independent of the student's controller configuration.
    private static final double MODEL_GEAR_RATIO = 80.0;

    private final SingleJointedArmSim physics = new SingleJointedArmSim(
        DCMotor.getKrakenX60(1), MODEL_GEAR_RATIO,
        SingleJointedArmSim.estimateMOI(ArmConstants.ARM_LENGTH_METERS, 3.0), ArmConstants.ARM_LENGTH_METERS,
        Units.degreesToRadians(ArmConstants.MIN_DEGREES), Units.degreesToRadians(ArmConstants.MAX_DEGREES), true, 0.0);

    private final TelemetryTable telemetry = Telemetry.getTable("Arm");

    void step(TalonFXSimState motor) {
        motor.setSupplyVoltage(RobotController.getBatteryVoltage());
        update(motor.getMotorVoltage());
        // Phoenix simulation needs raw motor rotations, before SensorToMechanismRatio is applied.
        motor.setRawRotorPosition(Units.radiansToRotations(physics.getAngle()) * MODEL_GEAR_RATIO);
        motor.setRotorVelocity(Units.radiansToRotations(physics.getVelocity()) * MODEL_GEAR_RATIO);
    }

    void update(double motorVolts) {
        physics.setInputVoltage(motorVolts);
        physics.update(0.020);
        // WPILib alpha 8 publishes root "position" arrays, but bundled AdvantageScope
        // still expects Mechanism2d roots with separate x/y fields. Publish that format.
        var drawing = telemetry.getTable("Mechanism");
        drawing.log(".type", "Mechanism2d");
        drawing.log("dims", new double[] {1.2, 1.2});
        drawing.log("backgroundColor", "#000020");
        var root = drawing.getTable("Pivot");
        root.log("x", 0.45);
        root.log("y", 0.40);
        var segment = root.getTable("Arm");
        segment.log(".type", "line");
        segment.log("angle", Units.radiansToDegrees(physics.getAngle()));
        segment.log("length", ArmConstants.ARM_LENGTH_METERS);
        segment.log("color", "#FFA500");
        segment.log("weight", 6.0);
    }
}
