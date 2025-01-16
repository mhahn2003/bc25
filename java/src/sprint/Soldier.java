package sprint;

import battlecode.common.*;

import static java.util.Objects.hash;

public class Soldier extends Unit {

    public void act() throws GameActionException {
//        System.out.println("beginning: " + Clock.getBytecodeNum());
        super.act();
//        System.out.println("init: " + Clock.getBytecodeNum());
        attack();
//        System.out.println("attack: " + Clock.getBytecodeNum());
        rush();
//        System.out.println("rush: " + Clock.getBytecodeNum());
        buildTower();
//        System.out.println("build: " + Clock.getBytecodeNum());
        buildSRP();
//        System.out.println("paint: " + Clock.getBytecodeNum());
        move();
//        System.out.println("move: " + Clock.getBytecodeNum());
        paintLeftover();
//        System.out.println("paintLeftover: " + Clock.getBytecodeNum());
        Comms.sendMessagesToTower();
    }

    public void attack() throws GameActionException {
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, opponentTeam);
        MapLocation closestNonDefenseTower = null;
        int minDist = 999999;
        MapLocation closestDefenseTower = null;
        int minDistDefense = 999999;
        for (RobotInfo robot : enemyRobots) {
            if (robot.getType().isTowerType()) {
                MapLocation robotLoc = robot.location;
                int dist = rc.getLocation().distanceSquaredTo(robotLoc);
                if (robot.getType().getBaseType() == UnitType.LEVEL_ONE_DEFENSE_TOWER) {
                    if (dist < minDistDefense) {
                        minDistDefense = dist;
                        closestDefenseTower = robotLoc;
                    }
                } else {
                    if (dist < minDist) {
                        minDist = dist;
                        closestNonDefenseTower = robotLoc;
                    }
                }
            }
        }

        if (closestNonDefenseTower != null) {
            aggressiveMode = true;
            if (rc.getLocation().distanceSquaredTo(closestNonDefenseTower) <= 9) {
                if (rc.canAttack(closestNonDefenseTower)) {
                    rc.attack(closestNonDefenseTower);
                }
                Direction dir = rc.getLocation().directionTo(closestNonDefenseTower).opposite();
                if (rc.canMove(dir)) {
                    rc.move(dir);
                }
                Direction left = dir.rotateLeft();
                if (rc.canMove(left)) {
                    rc.move(left);
                }
                Direction right = dir.rotateRight();
                if (rc.canMove(right)) {
                    rc.move(right);
                }
            } else {
                for (Direction dir : Globals.adjacentDirections) {
                    MapLocation loc = rc.getLocation().add(dir);
                    if (rc.canMove(dir) && loc.distanceSquaredTo(closestNonDefenseTower) <= 9) {
                        rc.move(dir);
                        break;
                    }
                }
                if (rc.isMovementReady()) {
                    Navigator.moveTo(closestNonDefenseTower);
                }
                if (rc.canAttack(closestNonDefenseTower)) {
                    rc.attack(closestNonDefenseTower);
                }
            }
        } else if (closestDefenseTower != null) {
            if (!aggressiveHold) {
                aggressiveMode = false;
                MapLocation opposite = new MapLocation(2 * closestDefenseTower.x - rc.getLocation().x, 2 * closestDefenseTower.y - rc.getLocation().y);
                Navigator.moveTo(opposite);
            } else {
                Movement.attackDefenseTower();
            }
        } else {
            aggressiveMode = false;
            aggressiveHold = false;
        }
    }

    public void rush() throws GameActionException {
        if (rc.getRoundNum() <= 3 && !rushSoldier) {
            rushSoldier = true;
            MapLocation[] ruins = rc.senseNearbyRuins(4);
            if (ruins.length == 0) {
                rushSoldier = false;
            } else {
                MapLocation base = ruins[0];
                symmetryLocations[0] = new MapLocation(mapWidth - base.x - 1, base.y);
                symmetryLocations[1] = new MapLocation(base.x, mapHeight - base.y - 1);
                symmetryLocations[2] = new MapLocation(mapWidth - base.x - 1, mapHeight - base.y - 1);
            }
        }
        if (rushSoldier) {
            if (aggressiveMode) {
                rushSoldier = false;
                return;
            }
            MapLocation closestPossibleEnemyBase = null;
            int minDist = 999999;
            for (int i = 0; i < 3; i++) {
                if (symmetryLocationsVisited[i]) {
                    continue;
                }
                if (rc.getLocation().distanceSquaredTo(symmetryLocations[i]) < minDist) {
                    minDist = rc.getLocation().distanceSquaredTo(symmetryLocations[i]);
                    closestPossibleEnemyBase = symmetryLocations[i];
                }
            }
            if (closestPossibleEnemyBase != null) {
                Navigator.moveTo(closestPossibleEnemyBase);
            } else {
                rushSoldier = false;
            }
        }
    }

    public void buildTower() throws GameActionException {
        if (aggressiveMode || aggressiveHold || rushSoldier || buildSRP) {
            return;
        }

        if (!buildTower) {
            MapLocation[] ruins = rc.senseNearbyRuins(-1);

            if (ruins.length == 0) {
                return;
            }

            MapLocation closestRuin = null;
            int minDist = GameConstants.VISION_RADIUS_SQUARED;
            for (MapLocation ruin : ruins) {
                if (impossibleRuins.contains(ruin)) {
                    continue;
                }
                RobotInfo robot = rc.senseRobotAtLocation(ruin);
                if (robot == null) {
                    int dist = rc.getLocation().distanceSquaredTo(ruin);
                    if (dist <= minDist) {
                        minDist = dist;
                        closestRuin = ruin;
                    }
                }
            }

            if (closestRuin == null) {
                return;
            }
            MapLocation markLoc = closestRuin.add(Direction.EAST);
            UnitType towerType = towerType(closestRuin);
            if (rc.canSenseLocation(markLoc)) {
                if (rc.senseMapInfo(markLoc).getMark() == PaintType.EMPTY) {
                    if (towerType != UnitType.LEVEL_ONE_DEFENSE_TOWER) {
                        if (rc.getLocation().distanceSquaredTo(markLoc) <= 2) {
                            if (rc.canMark(markLoc)) {
                                // false = money, true = paint
                                rc.mark(markLoc, towerType == UnitType.LEVEL_ONE_PAINT_TOWER);
                            }
                        } else {
                            Navigator.moveTo(markLoc);
                            return;
                        }
                    }
                } else {
                    if (rc.senseMapInfo(markLoc).getMark().isSecondary()) {
                        towerType = UnitType.LEVEL_ONE_PAINT_TOWER;
                    } else {
                        towerType = UnitType.LEVEL_ONE_MONEY_TOWER;
                    }
                }
            } else {
                Navigator.moveTo(markLoc);
                return;
            }

            buildTower = true;
            buildTowerType = towerType;
            buildRuinLocation = closestRuin;
            noPaintCounter = 0;
        }

        if (rc.canSenseRobotAtLocation(buildRuinLocation)) {
            RobotInfo robot = rc.senseRobotAtLocation(buildRuinLocation);
            if (robot != null) {
                buildTower = false;
                impossibleRuins.add(buildRuinLocation);
                return;
            }
        }

        UnitType type = towerType(buildRuinLocation);
        MapInfo[] nearbyLocations = rc.senseNearbyMapInfos(buildRuinLocation, 8);

        MapLocation bestPaintLocation = null;
        int closestDist = 9;
        int numLocsPaintable = 0;

        for (MapInfo info : nearbyLocations) {
            if (!info.getPaint().isEnemy() && !info.getMapLocation().equals(buildRuinLocation) && !(info.getPaint().isAlly() && info.getPaint().isSecondary() == Util.useSecondaryForTower(info.getMapLocation(), buildRuinLocation, type))) {
                if (rc.getLocation().distanceSquaredTo(info.getMapLocation()) <= closestDist) {
                    closestDist = rc.getLocation().distanceSquaredTo(info.getMapLocation());
                    bestPaintLocation = info.getMapLocation();
                    numLocsPaintable++;
                }
            }
        }

        if (bestPaintLocation != null) {
            noPaintCounter = 0;
            if (rc.isActionReady() && rc.canAttack(bestPaintLocation)) {
                rc.attack(bestPaintLocation, Util.useSecondaryForTower(bestPaintLocation, buildRuinLocation, type));
                if (numLocsPaintable == 1) {
                    Navigator.moveTo(bestPaintLocation);
                }
            } else {
                Navigator.moveTo(bestPaintLocation);
            }
        }
        if (rc.getLocation().distanceSquaredTo(buildRuinLocation) <= 2) {
            if (rc.isActionReady() && rc.canCompleteTowerPattern(type, buildRuinLocation)) {
                rc.completeTowerPattern(type, buildRuinLocation);
                buildTower = false;
            } else {
                noPaintCounter += 1;
                if (noPaintCounter >= noPaintTowerThreshold) {
                    buildTower = false;
                    impossibleRuins.add(buildRuinLocation);
                } else {
                    MapLocation opposite = new MapLocation(2 * buildRuinLocation.x - rc.getLocation().x, 2 * buildRuinLocation.y - rc.getLocation().y);
                    Navigator.moveTo(opposite);
                }
            }
        } else {
            Navigator.moveTo(buildRuinLocation);
        }
    }

    public void buildSRP() throws GameActionException {
        if (aggressiveMode || aggressiveHold || rushSoldier || buildTower) {
            return;
        }

        if (!buildSRP) {
            MapLocation loc = rc.getLocation();
            int x = (loc.x + 2) % 4;
            int y = (loc.y + 2) % 4;
            MapLocation[] possibleSRPLocations = new MapLocation[5];
            MapLocation[] ruinLocs = ruinLocations.getLocations();
            if (x == 0 && y == 0 && !impossibleSRPLocations.contains(loc) && !rc.senseMapInfo(loc).isResourcePatternCenter()) {
                possibleSRPLocations[4] = loc;
            }
            MapLocation loc1 = new MapLocation(loc.x - x, loc.y - y);
            if (rc.canSenseLocation(loc1) && !impossibleSRPLocations.contains(loc1) && !rc.senseMapInfo(loc1).isResourcePatternCenter()) {
                possibleSRPLocations[0] = loc1;
            }
            MapLocation loc2 = new MapLocation(loc.x - x, loc.y + 4 - y);
            if (rc.canSenseLocation(loc2) && !impossibleSRPLocations.contains(loc2) && !rc.senseMapInfo(loc2).isResourcePatternCenter()) {
                possibleSRPLocations[1] = loc2;
            }
            MapLocation loc3 = new MapLocation(loc.x + 4 - x, loc.y - y);
            if (rc.canSenseLocation(loc3) && !impossibleSRPLocations.contains(loc3) && !rc.senseMapInfo(loc3).isResourcePatternCenter()) {
                possibleSRPLocations[2] = loc3;
            }
            MapLocation loc4 = new MapLocation(loc.x + 4 - x, loc.y + 4 - y);
            if (rc.canSenseLocation(loc4) && !impossibleSRPLocations.contains(loc4) && !rc.senseMapInfo(loc4).isResourcePatternCenter()) {
                possibleSRPLocations[3] = loc4;
            }

            for (MapLocation ruin : ruinLocs) {
                boolean rawRuin = true;
                for (MapLocation friendlyPaintTowerLocation : friendlyPaintTowerLocations) {
                    if (friendlyPaintTowerLocation == null || ruin.equals(friendlyPaintTowerLocation)) {
                        rawRuin = false;
                        break;
                    }
                }
                for (MapLocation friendlyNonPaintTowerLocation : friendlyNonPaintTowerLocations) {
                    if (friendlyNonPaintTowerLocation == null || ruin.equals(friendlyNonPaintTowerLocation)) {
                        rawRuin = false;
                        break;
                    }
                }
                if (rawRuin) {
                    for (int i = 0; i < 5; i++) {
                        if (possibleSRPLocations[i] != null && Math.abs(ruin.x - possibleSRPLocations[i].x) <= 5 && Math.abs(ruin.y - possibleSRPLocations[i].y) <= 5) {
                            impossibleSRPLocations.add(possibleSRPLocations[i]);
                            possibleSRPLocations[i] = null;
                        }
                    }
                }
            }

            MapLocation closestSRPLocation = null;
            int minDist = 999999;
            for (MapLocation possibleSRPLocation : possibleSRPLocations) {
                if (possibleSRPLocation != null) {
                    int dist = loc.distanceSquaredTo(possibleSRPLocation);
                    if (dist < minDist) {
                        minDist = dist;
                        closestSRPLocation = possibleSRPLocation;
                    }
                }
            }

            if (closestSRPLocation != null) {
                buildSRP = true;
                buildSRPLocation = closestSRPLocation;
                noPaintCounter = 0;
            } else {
                return;
            }
        }

        if (rc.canSenseLocation(buildSRPLocation)) {
            if (rc.senseMapInfo(buildSRPLocation).isResourcePatternCenter()) {
                impossibleSRPLocations.add(buildSRPLocation);
                buildSRP = false;
                return;
            }
        }

        if (rc.getLocation().distanceSquaredTo(buildSRPLocation) <= 2) {
            if (!rc.canMarkResourcePattern(buildSRPLocation)) {
                impossibleSRPLocations.add(buildSRPLocation);
            }
        } else {
            Navigator.moveTo(buildSRPLocation);
        }

        if (!impossibleSRPLocations.contains(buildSRPLocation)) {
            MapInfo[] nearbyPaintLocations = rc.senseNearbyMapInfos(buildSRPLocation, 8);
            MapLocation bestPaintLocation = null;
            int closestDist = UnitType.SOLDIER.actionRadiusSquared;
            for (MapInfo info : nearbyPaintLocations) {
                if (!info.getPaint().isEnemy() && !(info.getPaint().isAlly() && info.getPaint().isSecondary() == Util.useSecondary(info.getMapLocation()))) {
                    if (rc.getLocation().distanceSquaredTo(info.getMapLocation()) <= closestDist) {
                        closestDist = rc.getLocation().distanceSquaredTo(info.getMapLocation());
                        bestPaintLocation = info.getMapLocation();
                    }
                }
            }
            if (bestPaintLocation != null) {
                noPaintCounter = 0;
                if (rc.isActionReady() && rc.canPaint(bestPaintLocation)) {
                    rc.attack(bestPaintLocation, Util.useSecondary(bestPaintLocation));
                } else {
                    Navigator.moveTo(bestPaintLocation);
                }
            } else {
                if (rc.isActionReady() && rc.canCompleteResourcePattern(buildSRPLocation)) {
                    rc.completeResourcePattern(buildSRPLocation);
                } else if (rc.getLocation().distanceSquaredTo(buildSRPLocation) > 2) {
                    Navigator.moveTo(buildSRPLocation);
                } else {
                    noPaintCounter += 1;
                    if (noPaintCounter >= noPaintSRPThreshold) {
                        impossibleSRPLocations.add(buildSRPLocation);
                        buildSRP = false;
                    }
                }
            }
        } else {
            buildSRP = false;
        }
    }

    public void move() throws GameActionException {
        if (aggressiveMode || aggressiveHold || rushSoldier || buildTower || !rc.isMovementReady()) {
            return;
        }

        RobotInfo[] adjacentAllies = rc.senseNearbyRobots(2, myTeam);
        if (adjacentAllies.length >= 3) {
            Movement.scatter();
        } else if (!explored) {
            Movement.wander();
        } else {
            Movement.venture();
        }
    }

    public void paintLeftover() throws GameActionException {
        if (aggressiveMode || rushSoldier || buildTower || !rc.isActionReady()) {
            return;
        }
        if (aggressiveHold && rc.getPaint() < UnitType.SOLDIER.paintCapacity * 0.75) {
            return;
        }
        MapInfo locInfo = rc.senseMapInfo(rc.getLocation());
        if (locInfo.getPaint() == PaintType.EMPTY) {
            MapLocation[] nearbyRuins = rc.senseNearbyRuins(8);
            if (nearbyRuins.length == 0) {
                if (rc.canAttack(rc.getLocation())) {
                    rc.attack(rc.getLocation(), Util.useSecondary(rc.getLocation()));
                }
            } else {
                MapLocation closestRuin = nearbyRuins[0];
                if (rc.canAttack(rc.getLocation())) {
                    rc.attack(rc.getLocation(), Util.useSecondaryForTower(rc.getLocation(), closestRuin, towerType(closestRuin)));
                }
            }
        } else {
            MapInfo[] nearbyPaintLocations = rc.senseNearbyMapInfos(9);
            MapLocation[] nearbyRuins = rc.senseNearbyRuins(-1);
            for (MapInfo info : nearbyPaintLocations) {
                if (info.getPaint() == PaintType.EMPTY) {
                    boolean nearRuin = false;
                    for (MapLocation ruin : nearbyRuins) {
                        if (info.getMapLocation().distanceSquaredTo(ruin) <= 8) {
                            nearRuin = true;
                            break;
                        }
                    }
                    if (!nearRuin) {
                        if (rc.canAttack(info.getMapLocation())) {
                            rc.attack(info.getMapLocation(), Util.useSecondary(info.getMapLocation()));
                            return;
                        }
                    }
                }
            }
            for (MapInfo info : nearbyPaintLocations) {
                if (info.getPaint() == PaintType.EMPTY) {
                    for (MapLocation ruin : nearbyRuins) {
                        if (info.getMapLocation().distanceSquaredTo(ruin) <= 8) {
                            MapLocation markLoc = ruin.add(Direction.EAST);
                            if (rc.canSenseLocation(markLoc)) {
                                if (rc.senseMapInfo(markLoc).getMark() == PaintType.EMPTY) {
                                    if (rc.canAttack(info.getMapLocation())) {
                                        rc.attack(info.getMapLocation(), Util.useSecondaryForTower(info.getMapLocation(), ruin, towerType(ruin)));
                                        return;
                                    }
                                } else {
                                    UnitType type = rc.senseMapInfo(markLoc).getMark().isSecondary() ? UnitType.LEVEL_ONE_PAINT_TOWER : UnitType.LEVEL_ONE_MONEY_TOWER;
                                    if (rc.canAttack(info.getMapLocation())) {
                                        rc.attack(info.getMapLocation(), Util.useSecondaryForTower(info.getMapLocation(), ruin, type));
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    public static UnitType towerType(MapLocation ruin) {
        int rand = hash(ruin.x, ruin.y) % 100;
        if (rc.getRoundNum() < 300) {
            int numTowers = rc.getNumberTowers();
            if (numTowers < 4) {
                return UnitType.LEVEL_ONE_MONEY_TOWER;
            } else if (numTowers % 2 == 0) {
                return UnitType.LEVEL_ONE_PAINT_TOWER;
            } else {
                return UnitType.LEVEL_ONE_MONEY_TOWER;
            }
        } else {
            if (rand < 70) {
                return UnitType.LEVEL_ONE_PAINT_TOWER;
            } else if (rand < 100) {
                return UnitType.LEVEL_ONE_MONEY_TOWER;
            } else {
                return UnitType.LEVEL_ONE_DEFENSE_TOWER;
            }
        }
    }

    public static double randomFunction(int x, int y) {
        int hash = (x * 0x1f1f1f1f) ^ (y * 0x2e2e2e2e);
        hash = ((hash >>> 16) ^ hash) * 0x45d9f3b;
        hash = ((hash >>> 16) ^ hash) * 0x45d9f3b;
        hash = (hash >>> 16) ^ hash;
        return (hash) / (double) 0xFFFFFFFF;
    }
}
