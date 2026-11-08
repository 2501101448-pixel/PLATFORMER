
package utils;

public class Constants {

    public static class Directions {
        public static final int LEFT = 0;
        public static final int UP = 1;
        public static final int RIGHT = 2;
        public static final int DOWN = 3;
    }

    public static class PlayerConstants {

        public static final int IDLE = 0;
        public static final int WALK = 1;
        public static final int RUNNING = 2;
        public static final int ATTACK = 3;
        public static final int BLOCK = 4;
        public static final int HIT = 5;
        public static final int GROUND = 6;
        public static final int JUMP = 7;
        public static final int DOUBLE_JUMP = 8;
        public static final int FALL = 9;
        public static final int LAND = 10;
        public static final int DEATH = 11;
        public static final int VICTORY = 12;
        public static final int DASH = 13;
        public static final int SPECIAL_ATTACK = 14;

        public static final int[] FRAME_COUNTS = {
            6, 8, 8, 6, 4,
            4, 6, 4, 6, 3,
            4, 8, 6, 5, 8
        };

        public static int GetSpriteAmount(int action) {
            if (action < 0 || action >= FRAME_COUNTS.length) {
                return 1;
            }

            return FRAME_COUNTS[action];
        }
    }
}
