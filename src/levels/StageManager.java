package levels;

import java.nio.file.Path;
import java.nio.file.Paths;

public class StageManager {

    public static final int STAGE_COUNT = 5;
    public static final int BOSS_STAGE = 5;

    private StageManager() {
    }

    public static String getStageName(int stageNumber) {
        checkStageNumber(stageNumber);
        if (stageNumber == BOSS_STAGE) {
            return "Stage 5 - Boss Fight";
        }
        return "Stage " + stageNumber;
    }

    public static Path getMapFile(int stageNumber) {
        checkStageNumber(stageNumber);
        return Paths.get("levels", "level" + stageNumber + ".map");
    }

    private static void checkStageNumber(int stageNumber) {
        if (stageNumber < 1 || stageNumber > STAGE_COUNT) {
            throw new IllegalArgumentException(
                    "Stage must be between 1 and " + STAGE_COUNT + "."
            );
        }
    }
}
