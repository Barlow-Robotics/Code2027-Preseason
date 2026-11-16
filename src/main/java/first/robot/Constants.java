package first.robot;

/** Dimensions and limits shared by the subsystems and their simulation models. */
public final class Constants {
    private Constants() {}

    public static final class ElevatorConstants {
        private ElevatorConstants() {}

        public static final double GEAR_RATIO = 12.0; // motor turns per drum turn
        public static final double MIN_METERS = 0.0;
        public static final double MAX_METERS = 0.9;
        public static final double DRUM_RADIUS_METERS = 0.025;
        public static final double METERS_PER_DRUM_TURN = 2.0 * Math.PI * DRUM_RADIUS_METERS;
    }

    public static final class ArmConstants {
        private ArmConstants() {}

        public static final double GEAR_RATIO = 80.0; // motor turns per arm turn
        public static final double MIN_DEGREES = -20.0;
        public static final double MAX_DEGREES = 95.0;
        public static final double ARM_LENGTH_METERS = 0.55;
    }
}
