package norush;

import battlecode.common.*;

public class Util extends Globals {

    public static boolean useSecondary(MapLocation location) {
        int x = (location.x % 4) - 2;
        int y = (location.y % 4) - 2;
        int absSum = Math.abs(x) + Math.abs(y);
        return absSum == 0 || absSum >= 3;
    }

    public static boolean useSecondaryForTower(MapLocation location, MapLocation ruin, UnitType type) {
        int x = location.x - ruin.x;
        int y = location.y - ruin.y;

        return switch (type) {
            case LEVEL_ONE_PAINT_TOWER -> Math.abs(x) == Math.abs(y);
            case LEVEL_ONE_MONEY_TOWER -> Math.abs(x) + Math.abs(y) == 2 || Math.abs(x) + Math.abs(y) == 3;
            case LEVEL_ONE_DEFENSE_TOWER -> Math.abs(x) + Math.abs(y) <= 2;
            default -> false;
        };
    }

    public static boolean useSecondaryGeneral(MapLocation location, MapLocation ruin, UnitType type, boolean forTower) {
        if (forTower) {
            return useSecondaryForTower(location, ruin, type);
        } else {
            return useSecondary(location);
        }
    }

    public static int getUpgradeCost(UnitType type) {
        return switch (type) {
            case LEVEL_ONE_PAINT_TOWER -> 3500;
            case LEVEL_ONE_MONEY_TOWER -> 20000;
            case LEVEL_ONE_DEFENSE_TOWER -> 4500;
            case LEVEL_TWO_PAINT_TOWER -> 6000;
            case LEVEL_TWO_MONEY_TOWER -> 40000;
            case LEVEL_TWO_DEFENSE_TOWER -> 7000;
            case LEVEL_THREE_PAINT_TOWER -> Integer.MAX_VALUE;
            case LEVEL_THREE_MONEY_TOWER -> Integer.MAX_VALUE;
            case LEVEL_THREE_DEFENSE_TOWER -> Integer.MAX_VALUE;
            default -> 0;
        };
    }

    public static int paintPenalty(MapLocation loc, PaintType paint) {
        int numAllies = 0;
        int x = loc.x - rc.getLocation().x;
        int y = loc.y - rc.getLocation().y;
        if (nearbyAllies[x+1][y+1]) numAllies++;
        if (nearbyAllies[x+1][y+2]) numAllies++;
        if (nearbyAllies[x+1][y+3]) numAllies++;
        if (nearbyAllies[x+2][y+1]) numAllies++;
        if (nearbyAllies[x+2][y+3]) numAllies++;
        if (nearbyAllies[x+3][y+1]) numAllies++;
        if (nearbyAllies[x+3][y+2]) numAllies++;
        if (nearbyAllies[x+3][y+3]) numAllies++;
        int paintPenalty = numAllies;
        if (paint == PaintType.EMPTY) {
            if (rc.getType() == UnitType.MOPPER) return 2 + numAllies;
            else return 1 + numAllies;
        } else if (paint.isEnemy()) {
            if (rc.getType() == UnitType.MOPPER) return 4 + 2 * numAllies;
            else paintPenalty = 2 + 2 * numAllies;
        }
        return paintPenalty;
    }

    public static UnitType towerType() {
        int numTowers = rc.getNumberTowers();
        if (numTowers < 6) {
            if (rc.getChips() > 1500 && rc.getRoundNum() > 100) return UnitType.LEVEL_ONE_PAINT_TOWER;
            else return UnitType.LEVEL_ONE_MONEY_TOWER;
        } else if (numTowers < 10) {
            return UnitType.LEVEL_ONE_PAINT_TOWER;
        } else if (numTowers < 12) {
            return UnitType.LEVEL_ONE_MONEY_TOWER;
        } else if (numTowers < 14) {
            return UnitType.LEVEL_ONE_PAINT_TOWER;
        } else if (numTowers < 16) {
            return UnitType.LEVEL_ONE_MONEY_TOWER;
        } else if (numTowers < 18) {
            return UnitType.LEVEL_ONE_PAINT_TOWER;
        } else if (numTowers < 20) {
            return UnitType.LEVEL_ONE_MONEY_TOWER;
        } else if (numTowers < 22) {
            return UnitType.LEVEL_ONE_PAINT_TOWER;
        } else if (numTowers < 24) {
            return UnitType.LEVEL_ONE_MONEY_TOWER;
        } else {
            return UnitType.LEVEL_ONE_PAINT_TOWER;
        }
    }
}
