package sprint;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.UnitType;

public class Util extends Globals {

    public static void paint(MapLocation location) throws GameActionException {
        if (rc.canPaint(location) && !rc.senseMapInfo(location).getPaint().isAlly()) {
            rc.attack(location, useSecondary(location));
        }
    }
    public static boolean useSecondary(MapLocation location) {
        int x = location.x % 4;
        int y = location.y % 4;
        switch (x) {
            case 0:
                switch (y) {
                    case 0, 1, 3:
                        return true;
                    case 2:
                        return false;
                }
            case 1, 3:
                switch (y) {
                    case 0:
                        return true;
                    case 1, 2, 3:
                        return false;
                }
            case 2:
                switch (y) {
                    case 2:
                        return true;
                    case 0, 1, 3:
                        return false;
                }
        }
        return false;
    }

    public static boolean useSecondaryForTower(MapLocation location, MapLocation ruin, UnitType type) {
        int x = location.x - ruin.x;
        int y = location.y - ruin.y;

        return switch (type) {
            case LEVEL_ONE_PAINT_TOWER -> Math.abs(x) == Math.abs(y);
            case LEVEL_ONE_MONEY_TOWER -> Math.abs(x) + Math.abs(y) == 1 || Math.abs(x) + Math.abs(y) == 4;
            case LEVEL_ONE_DEFENSE_TOWER -> Math.abs(x) + Math.abs(y) <= 2;
            default -> false;
        };

    }
}
