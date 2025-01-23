package quals;

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
            case LEVEL_ONE_DEFENSE_TOWER -> 4000;
            case LEVEL_TWO_PAINT_TOWER -> 6000;
            case LEVEL_TWO_MONEY_TOWER -> 40000;
            case LEVEL_TWO_DEFENSE_TOWER -> 6500;
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

    // left/right true = paint, false = money
    public static UnitType getTowerType(MapLocation ruin) throws GameActionException {
        MapLocation leftMark = ruin.add(Direction.WEST);
        MapLocation rightMark = ruin.add(Direction.EAST);
        MapLocation topMark = ruin.add(Direction.NORTH);
        MapLocation bottomMark = ruin.add(Direction.SOUTH);
        if (!rc.canSenseLocation(leftMark) || !rc.canSenseLocation(rightMark) || !rc.canSenseLocation(topMark) || !rc.canSenseLocation(bottomMark)) return null;
        PaintType leftPaint = rc.senseMapInfo(leftMark).getMark();
        PaintType rightPaint = rc.senseMapInfo(rightMark).getMark();
        PaintType topPaint = rc.senseMapInfo(topMark).getMark();
        PaintType bottomPaint = rc.senseMapInfo(bottomMark).getMark();
        if (leftPaint == PaintType.EMPTY && rightPaint == PaintType.EMPTY && topPaint == PaintType.EMPTY && bottomPaint == PaintType.EMPTY) {
            if (rc.getLocation().distanceSquaredTo(ruin) <= 2) {
                Logger.log("all empty");
                UnitType towerType = newTowerType(ruin);
                switch (towerType) {
                    case LEVEL_ONE_PAINT_TOWER -> {
                        if (rc.getLocation().distanceSquaredTo(leftMark) <= 2 && rc.canMark(leftMark)) rc.mark(leftMark, true);
                        else if (rc.canMark(rightMark)) rc.mark(rightMark, true);
                    }
                    case LEVEL_ONE_MONEY_TOWER -> {
                        if (rc.getLocation().distanceSquaredTo(leftMark) <= 2 && rc.canMark(leftMark)) rc.mark(leftMark, false);
                        else if (rc.canMark(rightMark)) rc.mark(rightMark, false);
                    }
                    case LEVEL_ONE_DEFENSE_TOWER -> {
                        if (rc.getLocation().distanceSquaredTo(topMark) <= 2 && rc.canMark(topMark)) rc.mark(topMark, true);
                        else if (rc.canMark(bottomMark)) rc.mark(bottomMark, true);
                    }
                }
                return towerType;
            } else {
                return null;
            }
        } else if (topPaint != PaintType.EMPTY || bottomPaint != PaintType.EMPTY) {
            Logger.log("top bottom nonempty");
            return UnitType.LEVEL_ONE_DEFENSE_TOWER;
        } else {
            Logger.log("left right nonempty");
            if (leftPaint != PaintType.EMPTY) {
                if (leftPaint.isSecondary()) return UnitType.LEVEL_ONE_PAINT_TOWER;
                else return UnitType.LEVEL_ONE_MONEY_TOWER;
            } else {
                if (rightPaint.isSecondary()) return UnitType.LEVEL_ONE_PAINT_TOWER;
                else return UnitType.LEVEL_ONE_MONEY_TOWER;
            }
        }
    }

    public static UnitType newTowerType(MapLocation ruin) {
        int numTowers = rc.getNumberTowers();
        if (numTowers < 6) {
            if (rc.getChips() > 1500 && rc.getRoundNum() > 100) return UnitType.LEVEL_ONE_PAINT_TOWER;
            else return UnitType.LEVEL_ONE_MONEY_TOWER;
        } else if (isDefenseTowerLocation(ruin)) {
            return UnitType.LEVEL_ONE_DEFENSE_TOWER;
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

    public static boolean isDefenseTowerLocation(MapLocation loc) {
        if (Math.abs(loc.x - exploreLocations[4].x) <= 5 && Math.abs(loc.y - exploreLocations[4].y) <= 5) return true;
        if (symmetry == 1) {
            return Math.abs(loc.x - exploreLocations[4].x) <= 5 && Math.abs(loc.y - exploreLocations[4].y) <= mapHeight / 4;
        } else if (symmetry == 2) {
            return Math.abs(loc.x - exploreLocations[4].x) <= mapWidth / 4 && Math.abs(loc.y - exploreLocations[4].y) <= 5;
        }
        return false;
    }

    public static MapLocation getBaseToVisit() throws GameActionException {
        MapLocation closestPossibleEnemyBase = null;
        int minDist = Integer.MAX_VALUE;
        for (int i = 0; i < 3; i++) {
            if (symmetryLocationsVisited[i]) {
                continue;
            }
            if (rc.getLocation().distanceSquaredTo(symmetryLocations[i]) <= minDist) {
                minDist = rc.getLocation().distanceSquaredTo(symmetryLocations[i]);
                closestPossibleEnemyBase = symmetryLocations[i];
            }
        }
        if (closestPossibleEnemyBase == null) return null;
        if (minDist <= 100) return closestPossibleEnemyBase;
        if (symmetryLocationsVisited[0]) {
            if (symmetryLocationsVisited[1]) return symmetryLocations[2];
            else if (symmetryLocationsVisited[2]) return symmetryLocations[1];
            else if (rc.getLocation().distanceSquaredTo(symmetryLocations[1]) <= rc.getLocation().distanceSquaredTo(symmetryLocations[2])) return symmetryLocations[1];
            else return symmetryLocations[2];
        } else {
            return symmetryLocations[0];
        }
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
            if (info1.hasRuin() != info2.hasRuin() || info1.isWall() != info2.isWall()) {
                symmetryLocationsVisited[symmetryIndex] = true;
                symmetryBroken[symmetryIndex] = true;
                Logger.log("Symmetry broken at " + symmetryIndex);
                if (symmetry == -1) {
                    int numSymmetriesUnbroken = 0;
                    for (int i = 0; i < 3; i++) {
                        if (!symmetryBroken[i]) {
                            symmetry = i;
                            numSymmetriesUnbroken++;
                        }
                    }
                    if (numSymmetriesUnbroken != 1) symmetry = -1;
                    Logger.log("Symmetry is " + symmetry);
                }
                return true;
            }
        }
        return false;
    }
}
