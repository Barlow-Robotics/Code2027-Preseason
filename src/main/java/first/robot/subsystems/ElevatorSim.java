package first.robot.subsystems;

import org.wpilib.system.RobotController;
import org.wpilib.math.system.DCMotor;
import org.wpilib.telemetry.Telemetry;
import org.wpilib.telemetry.TelemetryTable;

import com.ctre.phoenix6.sim.TalonFXSimState;

import first.robot.Constants.ElevatorConstants;

/** Physics and Mechanism2d drawing for the desktop elevator. */
final class ElevatorSim {
    // Physical gearbox in this model, independent of the student's controller configuration.
    private static final double MODEL_GEAR_RATIO = 12.0;

    private final org.wpilib.simulation.ElevatorSim physics = new org.wpilib.simulation.ElevatorSim(
        DCMotor.getKrakenX60(1), MODEL_GEAR_RATIO, 5.0, ElevatorConstants.DRUM_RADIUS_METERS,
        ElevatorConstants.MIN_METERS, ElevatorConstants.MAX_METERS, true, 0.0);

    private final TelemetryTable telemetry = Telemetry.getTable("Elevator");

    void step(TalonFXSimState motor) {
        motor.setSupplyVoltage(RobotController.getBatteryVoltage());
        update(motor.getMotorVoltage());
        // Phoenix simulation needs raw motor rotations, before SensorToMechanismRatio is applied.
        motor.setRawRotorPosition(physics.getPosition() / ElevatorConstants.METERS_PER_DRUM_TURN * MODEL_GEAR_RATIO);
        motor.setRotorVelocity(physics.getVelocity() / ElevatorConstants.METERS_PER_DRUM_TURN * MODEL_GEAR_RATIO);
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
        var root = drawing.getTable("Base");
        root.log("x", 0.6);
        root.log("y", 0.1);
        var segment = root.getTable("Extension");
        segment.log(".type", "line");
        segment.log("angle", 90.0);
        segment.log("length", 0.10 + physics.getPosition());
        segment.log("color", "#008000");
        segment.log("weight", 6.0);
    }
}
