package first.robot;

/** Mechanism dimensions, limits, and named goal presets. */
public final class Constants {
    private Constants() {}

    public static final class ElevatorConstants {
        private ElevatorConstants() {}

        public static final int MOTOR_CAN_ID = 20;

        // Goal presets used by the operator buttons.
        public static final double LOW_METERS = 0.0;
        public static final double HIGH_METERS = 0.65;

        // TODO 1: Set the gear ratio using the Mechanisms walkthrough.
        public static final double GEAR_RATIO = 1.0; // motor turns per drum turn
        public static final double MIN_METERS = 0.0;
        public static final double MAX_METERS = 0.9;
        public static final double DRUM_RADIUS_METERS = 0.025;
        public static final double METERS_PER_DRUM_TURN = 2.0 * Math.PI * DRUM_RADIUS_METERS;
    }

    public static final class ArmConstants {
        private ArmConstants() {}

        public static final int MOTOR_CAN_ID = 21;

        // Goal presets used by the operator buttons.
        public static final double LOW_DEGREES = 0.0;
        public static final double HIGH_DEGREES = 65.0;

        // TODO 1: Set the gear ratio using the Mechanisms walkthrough.
        public static final double GEAR_RATIO = 80.0; // motor turns per arm turn
        public static final double MIN_DEGREES = -20.0;
        public static final double MAX_DEGREES = 95.0;
        public static final double ARM_LENGTH_METERS = 0.55;
    }
}
