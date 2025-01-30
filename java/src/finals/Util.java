package finals;

import battlecode.common.*;

public class Util extends Globals {

    private static int symmetryCheckBytecodeThreshold = 2500;

    public static boolean useSecondary(MapLocation loc) {
        int x, y;
        if (loc.x <= midX1) x = loc.x % 4;
        else x = (loc.x - mapWidth + 1) % 4;
        if (loc.y <= midY1) y = loc.y % 4;
        else y = (loc.y - mapHeight + 1) % 4;
        if (x < 0) x += 4;
        if (y < 0) y += 4;
        int absSum = Math.abs(x-2) + Math.abs(y-2);
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

    public static boolean canMove(Direction direction) throws GameActionException {
        if (closestDefenseTower != null) {
            MapLocation location = rc.adjacentLocation(direction);
            if (location.distanceSquaredTo(closestDefenseTower) <= 16) {
                return false;
            }
        }
        if (closestEnemyTower != null) {
            MapLocation location = rc.adjacentLocation(direction);
            if (location.distanceSquaredTo(closestEnemyTower) <= 9) {
                return false;
            }
        }
        if (rc.getType() == UnitType.MOPPER) {
            MapLocation location = rc.adjacentLocation(direction);
            if (rc.canSenseLocation(location)) {
                PaintType paint = rc.senseMapInfo(location).getPaint();
                if (rc.getPaint() < 20 && !noNearbyAllyPaint) {
                    if (!paint.isAlly()) return false;
                } else {
                    if (paint.isEnemy()) return false;
                }
            }
        }
        return rc.canMove(direction);
    }

    public static int getUpgradeCost(UnitType type) {
        return switch (type) {
            case LEVEL_ONE_PAINT_TOWER -> 3500;
            case LEVEL_ONE_MONEY_TOWER -> 20000;
            case LEVEL_ONE_DEFENSE_TOWER -> 15000;
            case LEVEL_TWO_PAINT_TOWER -> 6000;
            case LEVEL_TWO_MONEY_TOWER -> 40000;
            case LEVEL_TWO_DEFENSE_TOWER -> 20000;
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
        } else if ((topPaint != PaintType.EMPTY || bottomPaint != PaintType.EMPTY) && rc.getNumberTowers() > 4) {
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
        if (rc.getChips() > 2500) return UnitType.LEVEL_ONE_PAINT_TOWER;
        else return UnitType.LEVEL_ONE_MONEY_TOWER;
//        if (isDefenseTowerLocation(ruin) && rc.getNumberTowers() > 4) {
//            return UnitType.LEVEL_ONE_DEFENSE_TOWER;
//        } else {
//            if (rc.getChips() > 2500) return UnitType.LEVEL_ONE_PAINT_TOWER;
//            else return UnitType.LEVEL_ONE_MONEY_TOWER;
//        }
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

    public static MapLocation getClosestSRPLocation(MapLocation[] rawRuinLocs) throws GameActionException {
        MapLocation loc = rc.getLocation();
        int x, y;
        if (loc.x <= midX1) x = (loc.x + 2) % 4;
        else x = (loc.x - mapWidth - 1) % 4;
        if (loc.y <= midY1) y = (loc.y + 2) % 4;
        else y = (loc.y - mapHeight - 1) % 4;
        if (x < 0) x += 4;
        if (y < 0) y += 4;
        MapLocation[] possibleSRPLocations = new MapLocation[8];
        MapLocation loc1 = new MapLocation(loc.x - x, loc.y - y);
        if (isValidSRPLocation(loc1)) possibleSRPLocations[0] = loc1;
        MapLocation loc2 = new MapLocation(loc.x - x, loc.y + 4 - y);
        if (isValidSRPLocation(loc2)) possibleSRPLocations[1] = loc2;
        MapLocation loc3 = new MapLocation(loc.x + 4 - x, loc.y - y);
        if (isValidSRPLocation(loc3)) possibleSRPLocations[2] = loc3;
        MapLocation loc4 = new MapLocation(loc.x + 4 - x, loc.y + 4 - y);
        if (isValidSRPLocation(loc4)) possibleSRPLocations[3] = loc4;
        MapLocation loc5 = new MapLocation(loc.x - 4 - x, loc.y - y);
        if (isValidSRPLocation(loc5)) possibleSRPLocations[4] = loc5;
        MapLocation loc6 = new MapLocation(loc.x - x, loc.y - 4 - y);
        if (isValidSRPLocation(loc6)) possibleSRPLocations[5] = loc6;
        MapLocation loc7 = new MapLocation(loc.x + 4 - x, loc.y - 4 - y);
        if (isValidSRPLocation(loc7)) possibleSRPLocations[6] = loc7;
        MapLocation loc8 = new MapLocation(loc.x - 4 - x, loc.y + 4 - y);
        if (isValidSRPLocation(loc8)) possibleSRPLocations[7] = loc8;
        for (MapLocation ruin : rawRuinLocs) {
            for (int i = 0; i < possibleSRPLocations.length; i++) {
                if (possibleSRPLocations[i] != null && Math.abs(ruin.x - possibleSRPLocations[i].x) <= 5 && Math.abs(ruin.y - possibleSRPLocations[i].y) <= 5) {
                    impossibleSRPLocations.add(possibleSRPLocations[i]);
                    possibleSRPLocations[i] = null;
                    break;
                }
            }
        }
        MapLocation closestSRPLocation = null;
        int minDist = Integer.MAX_VALUE;
        for (MapLocation possibleSRPLocation : possibleSRPLocations) {
            if (possibleSRPLocation != null) {
                int dist = loc.distanceSquaredTo(possibleSRPLocation);
                if (dist < minDist) {
                    minDist = dist;
                    closestSRPLocation = possibleSRPLocation;
                }
            }
        }
        return closestSRPLocation;
    }

    public static boolean isValidSRPLocation(MapLocation loc) throws GameActionException {
        if ((loc.x <= midX1 != rc.getLocation().x <= midX1) || (loc.y <= midY1 != rc.getLocation().y <= midY1)) return false;
        if (Math.abs(loc.x - midX1) < 2 || Math.abs(loc.y - midY1) < 2) return false;
        return rc.canSenseLocation(loc) && !impossibleSRPLocations.contains(loc) && !rc.senseMapInfo(loc).isResourcePatternCenter();
    }

    public static void checkSymmetry() throws GameActionException {
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
                            if (!symmetryVert || Clock.getBytecodesLeft() < symmetryCheckBytecodeThreshold) break;
                        }
                    }
                    if (!symmetryVert || Clock.getBytecodesLeft() < symmetryCheckBytecodeThreshold) break;
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
                            if (!symmetryVert || Clock.getBytecodesLeft() < symmetryCheckBytecodeThreshold) break;
                        }
                    }
                    if (!symmetryVert || Clock.getBytecodesLeft() < symmetryCheckBytecodeThreshold) break;
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
                            if (!symmetryHorz || Clock.getBytecodesLeft() < symmetryCheckBytecodeThreshold) break;
                        }
                    }
                    if (!symmetryHorz || Clock.getBytecodesLeft() < symmetryCheckBytecodeThreshold) break;
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
                            if (!symmetryHorz || Clock.getBytecodesLeft() < symmetryCheckBytecodeThreshold) break;
                        }
                    }
                    if (!symmetryHorz || Clock.getBytecodesLeft() < symmetryCheckBytecodeThreshold) break;
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
                        if (!symmetryDiag || Clock.getBytecodesLeft() < symmetryCheckBytecodeThreshold) break;
                    }
                    if (!symmetryDiag || Clock.getBytecodesLeft() < symmetryCheckBytecodeThreshold) break;
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
                        if (!symmetryDiag || Clock.getBytecodesLeft() < symmetryCheckBytecodeThreshold) break;
                    }
                    if (!symmetryDiag || Clock.getBytecodesLeft() < symmetryCheckBytecodeThreshold) break;
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
                        if (!symmetryDiag || Clock.getBytecodesLeft() < symmetryCheckBytecodeThreshold) break;
                    }
                    if (!symmetryDiag || Clock.getBytecodesLeft() < symmetryCheckBytecodeThreshold) break;
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
                        if (!symmetryDiag || Clock.getBytecodesLeft() < symmetryCheckBytecodeThreshold) break;
                    }
                    if (!symmetryDiag || Clock.getBytecodesLeft() < symmetryCheckBytecodeThreshold) break;
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
