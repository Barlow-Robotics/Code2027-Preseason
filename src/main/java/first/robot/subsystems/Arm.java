package first.robot.subsystems;

import org.wpilib.command2.Command;
import org.wpilib.command2.SubsystemBase;
import org.wpilib.telemetry.Telemetry;
import org.wpilib.telemetry.TelemetryTable;
import com.ctre.phoenix6.configs.TalonFXConfiguration;

import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.sim.TalonFXSimState;
// TODO 7a: Add the Motion Magic and gravity imports.
import first.robot.generated.TunerConstants;

import first.robot.Constants.ArmConstants;

/** Desktop arm for the preseason controls lesson. */
public final class Arm extends SubsystemBase {
    private final ArmSim sim = new ArmSim();
    private final TalonFX motor = new TalonFX(ArmConstants.MOTOR_CAN_ID, TunerConstants.kCANBus);
    private double targetDegrees = ArmConstants . LOW_DEGREES;
    private final TelemetryTable telemetry =
    Telemetry.getTable("Arm");

    public Arm() {
         var config = new TalonFXConfiguration();
        config.Feedback.SensorToMechanismRatio = ArmConstants.GEAR_RATIO;
        // TODO 7c: Add gains and Motion Magic settings here.

         var status = motor.getConfigurator().apply(config);
        if (!status.isOK()) {
        throw new IllegalStateException("Arm Talon FX configuration failed: " + status);
    }
    
        // Setup Simulation
        // Sets the simulated motor model. This setup is prepared for you; leave it in place.
        motor.getSimState().setMotorType(TalonFXSimState.MotorType.KrakenX60);
    }

    public Command setAngleGoalCommand(double goal) {
        return runOnce(() -> {setAngleGoal(goal));
    }

    private void setAngleGoal(double goal) {
        targetDegrees = Math.clamp(goal, ArmConstants.MIN_DEGREES, ArmConstants.MAX_DEGREES);
        //TODO 7d: Send the goal to the motor controller 
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
