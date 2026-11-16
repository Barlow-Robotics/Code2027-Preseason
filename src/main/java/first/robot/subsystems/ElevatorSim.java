package first.robot.subsystems;

import org.wpilib.system.RobotController;
import org.wpilib.math.system.DCMotor;
import org.wpilib.smartdashboard.Mechanism2d;
import org.wpilib.smartdashboard.MechanismLigament2d;
import org.wpilib.telemetry.Telemetry;
import org.wpilib.telemetry.TelemetryTable;
import org.wpilib.util.Color;
import org.wpilib.util.Color8Bit;

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
    private final Mechanism2d drawing = new Mechanism2d(1.2, 1.2);
    private final MechanismLigament2d extension = drawing.getRoot("Base", 0.6, 0.1)
        .append(new MechanismLigament2d("Extension", 0.10, 90.0, 6.0,
            new Color8Bit(Color.GREEN)));

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
        extension.setLength(0.10 + physics.getPosition());
        telemetry.log("Mechanism", drawing);
    }
}
