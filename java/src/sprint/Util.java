package sprint;

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

    public static void checkSymmetry() throws GameActionException {
        int midX1, midX2;
        if (mapWidth % 2 == 1) {
            midX1 = mapWidth / 2;
            midX2 = mapWidth / 2;
        } else {
            midX1 = mapWidth / 2 - 1;
            midX2 = mapWidth / 2;
        }
        int midY1, midY2;
        if (mapHeight % 2 == 1) {
            midY1 = mapHeight / 2;
            midY2 = mapHeight / 2;
        } else {
            midY1 = mapHeight / 2 - 1;
            midY2 = mapHeight / 2;
        }
        int x = rc.getLocation().x;
        int y = rc.getLocation().y;

        if (!symmetryLocationsVisited[1]) {
            boolean symmetryVert = true;
            if (x <= midX1) {
                for (int i = 4; i >= 0; i--) {
                    if (x+i >= midX2) {
                        for (int j = 0; j < 5; j++) {
                            if (j == 0) {
                                MapLocation loc1 = new MapLocation(x + i, y);
                                MapLocation loc2 = new MapLocation(mapWidth - 1 - x - i, y);
                                if (!rc.canSenseLocation(loc1)) break;
                                if (vertCheckedLocations.contains(loc1)) continue;
                                vertCheckedLocations.add(loc1);
                                if (checkSymmetry(loc1, loc2, 1)) symmetryVert = false;
                            } else {
                                MapLocation loc1 = new MapLocation(x + i, y + j);
                                MapLocation loc2 = new MapLocation(mapWidth - 1 - x - i, y + j);
                                if (!rc.canSenseLocation(loc1)) break;
                                if (!(vertCheckedLocations.contains(loc1))) {
                                    vertCheckedLocations.add(loc1);
                                    if (checkSymmetry(loc1, loc2, 1)) symmetryVert = false;
                                    loc1 = new MapLocation(x + i, y - j);
                                    loc2 = new MapLocation(mapWidth - 1 - x - i, y - j);
                                    if (vertCheckedLocations.contains(loc1)) continue;
                                    vertCheckedLocations.add(loc1);
                                    if (checkSymmetry(loc1, loc2, 1)) symmetryVert = false;
                                }
                            }
                            if (!symmetryVert) break;
                        }
                    }
                    if (!symmetryVert) break;
                }
            } else {
                for (int i = 4; i >= 0; i--) {
                    if (x-i <= midX1) {
                        for (int j = 0; j < 5; j++) {
                            if (j == 0) {
                                MapLocation loc1 = new MapLocation(x - i, y);
                                MapLocation loc2 = new MapLocation(mapWidth - 1 - x + i, y);
                                if (!rc.canSenseLocation(loc1)) break;
                                if (vertCheckedLocations.contains(loc1)) continue;
                                vertCheckedLocations.add(loc1);
                                if (checkSymmetry(loc1, loc2, 1)) symmetryVert = false;
                            } else {
                                MapLocation loc1 = new MapLocation(x - i, y + j);
                                MapLocation loc2 = new MapLocation(mapWidth - 1 - x + i, y + j);
                                if (!rc.canSenseLocation(loc1)) break;
                                if (!(vertCheckedLocations.contains(loc1))) {
                                    vertCheckedLocations.add(loc1);
                                    if (checkSymmetry(loc1, loc2, 1)) symmetryVert = false;
                                    loc1 = new MapLocation(x - i, y - j);
                                    loc2 = new MapLocation(mapWidth - 1 - x + i, y - j);
                                    if (vertCheckedLocations.contains(loc1)) continue;
                                    vertCheckedLocations.add(loc1);
                                    if (checkSymmetry(loc1, loc2, 1)) symmetryVert = false;
                                }
                            }
                            if (!symmetryVert) break;
                        }
                    }
                    if (!symmetryVert) break;
                }
            }
        }

        if (!symmetryLocationsVisited[2]) {
            boolean symmetryHorz = true;
            if (y <= midY1) {
                for (int i = 4; i >= 0; i--) {
                    if (y+i >= midY2) {
                        for (int j = 0; j < 5; j++) {
                            if (j == 0) {
                                MapLocation loc1 = new MapLocation(x, y + i);
                                MapLocation loc2 = new MapLocation(x, mapHeight - 1 - y - i);
                                if (!rc.canSenseLocation(loc1)) break;
                                if (horzCheckedLocations.contains(loc1)) continue;
                                horzCheckedLocations.add(loc1);
                                if (checkSymmetry(loc1, loc2, 2)) symmetryHorz = false;
                            } else {
                                MapLocation loc1 = new MapLocation(x + j, y + i);
                                MapLocation loc2 = new MapLocation(x + j, mapHeight - 1 - y - i);
                                if (!rc.canSenseLocation(loc1)) break;
                                if (!(horzCheckedLocations.contains(loc1))) {
                                    horzCheckedLocations.add(loc1);
                                    if (checkSymmetry(loc1, loc2, 2)) symmetryHorz = false;
                                    loc1 = new MapLocation(x - j, y + i);
                                    loc2 = new MapLocation(x - j, mapHeight - 1 - y - i);
                                    if (horzCheckedLocations.contains(loc1)) continue;
                                    horzCheckedLocations.add(loc1);
                                    if (checkSymmetry(loc1, loc2, 2)) symmetryHorz = false;
                                }
                            }
                            if (!symmetryHorz) break;
                        }
                    }
                    if (!symmetryHorz) break;
                }
            } else {
                for (int i = 4; i >= 0; i--) {
                    if (y - i <= midY1) {
                        for (int j = 0; j < 5; j++) {
                            if (j == 0) {
                                MapLocation loc1 = new MapLocation(x, y - i);
                                MapLocation loc2 = new MapLocation(x, mapHeight - 1 - y + i);
                                if (!rc.canSenseLocation(loc1)) break;
                                if (horzCheckedLocations.contains(loc1)) continue;
                                horzCheckedLocations.add(loc1);
                                if (checkSymmetry(loc1, loc2, 2)) symmetryHorz = false;
                            } else {
                                MapLocation loc1 = new MapLocation(x + j, y - i);
                                MapLocation loc2 = new MapLocation(x + j, mapHeight - 1 - y + i);
                                if (!rc.canSenseLocation(loc1)) break;
                                if (!(horzCheckedLocations.contains(loc1))) {
                                    horzCheckedLocations.add(loc1);
                                    if (checkSymmetry(loc1, loc2, 2)) symmetryHorz = false;
                                    loc1 = new MapLocation(x - j, y - i);
                                    loc2 = new MapLocation(x - j, mapHeight - 1 - y + i);
                                    if (horzCheckedLocations.contains(loc1)) continue;
                                    horzCheckedLocations.add(loc1);
                                    if (checkSymmetry(loc1, loc2, 2)) symmetryHorz = false;
                                }
                            }
                            if (!symmetryHorz) break;
                        }
                    }
                    if (!symmetryHorz) break;
                }
            }
        }

        if (!symmetryLocationsVisited[0]) {
            boolean symmetryDiag = true;
            if (x <= midX1 && y <= midY1) {
                for (int i = 4; i >= 0; i--) {
                    for (int j = 0; j < 5; j++) {
                        MapLocation loc1 = new MapLocation(x + i, y + j);
                        MapLocation loc2 = new MapLocation(mapWidth - 1 - x - i, mapHeight - 1 - y - j);
                        if (!rc.canSenseLocation(loc1)) break;
                        if (diagCheckedLocations.contains(loc1)) continue;
                        diagCheckedLocations.add(loc1);
                        if (checkSymmetry(loc1, loc2, 0)) symmetryDiag = false;
                        if (!symmetryDiag) break;
                    }
                    if (!symmetryDiag) break;
                }
            } else if (x <= midX1 && y >= midY2) {
                for (int i = 4; i >= 0; i--) {
                    for (int j = 0; j < 5; j++) {
                        MapLocation loc1 = new MapLocation(x + i, y - j);
                        MapLocation loc2 = new MapLocation(mapWidth - 1 - x - i, mapHeight - 1 - y + j);
                        if (!rc.canSenseLocation(loc1)) break;
                        if (diagCheckedLocations.contains(loc1)) continue;
                        diagCheckedLocations.add(loc1);
                        if (checkSymmetry(loc1, loc2, 0)) symmetryDiag = false;
                        if (!symmetryDiag) break;
                    }
                    if (!symmetryDiag) break;
                }
            } else if (x >= midX2 && y <= midY1) {
                for (int i = 4; i >= 0; i--) {
                    for (int j = 0; j < 5; j++) {
                        MapLocation loc1 = new MapLocation(x - i, y + j);
                        MapLocation loc2 = new MapLocation(mapWidth - 1 - x + i, mapHeight - 1 - y - j);
                        if (!rc.canSenseLocation(loc1)) break;
                        if (diagCheckedLocations.contains(loc1)) continue;
                        diagCheckedLocations.add(loc1);
                        if (checkSymmetry(loc1, loc2, 0)) symmetryDiag = false;
                        if (!symmetryDiag) break;
                    }
                    if (!symmetryDiag) break;
                }
            } else {
                for (int i = 4; i >= 0; i--) {
                    for (int j = 0; j < 5; j++) {
                        MapLocation loc1 = new MapLocation(x - i, y - j);
                        MapLocation loc2 = new MapLocation(mapWidth - 1 - x + i, mapHeight - 1 - y + j);
                        if (!rc.canSenseLocation(loc1)) break;
                        if (diagCheckedLocations.contains(loc1)) continue;
                        diagCheckedLocations.add(loc1);
                        if (checkSymmetry(loc1, loc2, 0)) symmetryDiag = false;
                        if (!symmetryDiag) break;
                    }
                    if (!symmetryDiag) break;
                }
            }
        }
    }

    public static boolean checkSymmetry(MapLocation loc1, MapLocation loc2, int symmetryIndex) throws GameActionException {
        if (loc1.equals(loc2)) return false;
        if (rc.canSenseLocation(loc1) && rc.canSenseLocation(loc2)) {
            MapInfo info1 = rc.senseMapInfo(loc1);
            MapInfo info2 = rc.senseMapInfo(loc2);
            if (info1.hasRuin() != info2.hasRuin()) {
                symmetryLocationsVisited[symmetryIndex] = true;
                return true;
            }
            if (info1.isWall() != info2.isWall()) {
                symmetryLocationsVisited[symmetryIndex] = true;
                return true;
            }
        }
        return false;
    }
}
