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

/** Physics and Mechanism2d drawing for the desktop elevator. */
final class ElevatorSim {
    static final double GEAR_RATIO = 12.0; // motor turns per mechanism turn
    static final double MIN_METERS = 0.0;
    static final double MAX_METERS = 0.9;
    private static final double DRUM_RADIUS_METERS = 0.025;
    static final double METERS_PER_DRUM_TURN = 2.0 * Math.PI * DRUM_RADIUS_METERS;
    private final org.wpilib.simulation.ElevatorSim physics = new org.wpilib.simulation.ElevatorSim(
        DCMotor.getKrakenX60(1), GEAR_RATIO, 5.0, DRUM_RADIUS_METERS,
        MIN_METERS, MAX_METERS, true, 0.0);

    private final TelemetryTable telemetry = Telemetry.getTable("Preseason").getTable("Elevator");
    private final Mechanism2d drawing = new Mechanism2d(1.2, 1.2);
    private final MechanismLigament2d extension = drawing.getRoot("Base", 0.6, 0.1)
        .append(new MechanismLigament2d("Extension", 0.10, 90.0, 6.0,
            new Color8Bit(Color.GREEN)));

    void step(TalonFXSimState motor) {
        motor.setSupplyVoltage(RobotController.getBatteryVoltage());
        update(motor.getMotorVoltage());
        // Phoenix simulation needs raw motor rotations, before SensorToMechanismRatio is applied.
        motor.setRawRotorPosition(physics.getPosition() / METERS_PER_DRUM_TURN * GEAR_RATIO);
        motor.setRotorVelocity(physics.getVelocity() / METERS_PER_DRUM_TURN * GEAR_RATIO);
    }

    void update(double motorVolts) {
        physics.setInputVoltage(motorVolts);
        physics.update(0.020);
        extension.setLength(0.10 + physics.getPosition());
        telemetry.log("Mechanism", drawing);
    }
}
