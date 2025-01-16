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
        int x = (location.x+2) % 4;
        int y = (location.y+2) % 4;
        int absSum =  Math.abs(x) + Math.abs(y);
        return absSum == 0 || absSum >= 3;
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
