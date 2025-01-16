package sprint;

import battlecode.common.*;

public class Soldier extends Unit {

    public void act() throws GameActionException {
        super.act();
        attack();
        rush();
        build();
        paint();
        move();
        paintLeftover();
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
                MapLocation opposite = new MapLocation(2 * closestNonDefenseTower.x - rc.getLocation().x, 2 * closestNonDefenseTower.y - rc.getLocation().y);
                Navigator.moveTo(opposite);
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
            if (rc.getPaint() >= UnitType.SOLDIER.paintCapacity * 0.25) {
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
    }

    public void build() throws GameActionException {
        if (aggressiveMode || aggressiveHold || rushSoldier) {
            return;
        }

        MapLocation[] ruins = rc.senseNearbyRuins(-1);

        if (ruins.length == 0) {
            builder = false;
            return;
        }

        MapLocation closestRuin = null;
        int minDist = GameConstants.VISION_RADIUS_SQUARED;
        for (MapLocation ruin : ruins) {
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

        builder = true;
        UnitType type = towerType(rc.getLocation(), closestRuin);
        MapInfo[] nearbyLocations = rc.senseNearbyMapInfos(closestRuin, 8);

        MapLocation bestPaintLocation = null;
        int closestDist = UnitType.SOLDIER.actionRadiusSquared;

        for (MapInfo info : nearbyLocations) {
            if (!(info.getPaint().isAlly() && info.getPaint().isSecondary() == Util.useSecondaryForTower(info.getMapLocation(), closestRuin, type))) {
                if (rc.getLocation().distanceSquaredTo(info.getMapLocation()) <= closestDist) {
                    closestDist = rc.getLocation().distanceSquaredTo(info.getMapLocation());
                    bestPaintLocation = info.getMapLocation();
                }
            }
        }

        if (bestPaintLocation != null) {
            if (rc.canPaint(bestPaintLocation)) {
                rc.attack(bestPaintLocation, Util.useSecondaryForTower(bestPaintLocation, closestRuin, type));
            } else {
                if (rc.getPaint() < UnitType.SOLDIER.attackCost) {
                    builder = false;
                } else {
                    Navigator.moveTo(bestPaintLocation);
                }
            }
        }
        if (rc.getLocation().distanceSquaredTo(closestRuin) <= GameConstants.BUILD_TOWER_RADIUS_SQUARED) {
            if (rc.canCompleteTowerPattern(type, closestRuin)) {
                rc.completeTowerPattern(type, closestRuin);
                builder = false;
            }
        } else {
            MapLocation opposite = new MapLocation(2 * closestRuin.x - rc.getLocation().x, 2 * closestRuin.y - rc.getLocation().y);
            Navigator.moveTo(opposite);
        }
    }

    public void paint() throws GameActionException {
        if (aggressiveMode || aggressiveHold || rushSoldier || builder) {
            return;
        }

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
            for (int i = 0; i < 5; i++) {
                if (possibleSRPLocations[i] != null && Math.abs(ruin.x - possibleSRPLocations[i].x) <= 5 && Math.abs(ruin.y - possibleSRPLocations[i].y) <= 5) {
                    impossibleSRPLocations.add(possibleSRPLocations[i]);
                    possibleSRPLocations[i] = null;
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
            if (rc.getLocation().distanceSquaredTo(closestSRPLocation) <= 2) {
                if (!rc.canMarkResourcePattern(closestSRPLocation)) {
                    impossibleSRPLocations.add(closestSRPLocation);
                }
            } else {
                Navigator.moveTo(closestSRPLocation);
            }

            if (!impossibleSRPLocations.contains(closestSRPLocation)) {
                MapInfo[] nearbyPaintLocations = rc.senseNearbyMapInfos(closestSRPLocation, 8);
                MapLocation bestPaintLocation = null;
                int closestDist = UnitType.SOLDIER.actionRadiusSquared;
                for (MapInfo info : nearbyPaintLocations) {
                    if (!(info.getPaint().isAlly() && info.getPaint().isSecondary() == Util.useSecondary(info.getMapLocation()))) {
                        if (rc.getLocation().distanceSquaredTo(info.getMapLocation()) <= closestDist) {
                            closestDist = rc.getLocation().distanceSquaredTo(info.getMapLocation());
                            bestPaintLocation = info.getMapLocation();
                        }
                    }
                }
                if (bestPaintLocation != null) {
                    if (rc.canPaint(bestPaintLocation)) {
                        rc.attack(bestPaintLocation, Util.useSecondary(bestPaintLocation));
                    } else {
                        Navigator.moveTo(bestPaintLocation);
                    }
                } else {
                    if (rc.canCompleteResourcePattern(closestSRPLocation)) {
                        rc.completeResourcePattern(closestSRPLocation);
                    } else if (rc.getLocation().distanceSquaredTo(closestSRPLocation) > 2) {
                        Navigator.moveTo(closestSRPLocation);
                    }
                }
            }
        }
    }

    public void move() throws GameActionException {
        if (aggressiveMode || aggressiveHold || rushSoldier || builder || !rc.isMovementReady()) {
            return;
        }

        RobotInfo[] adjacentAllies = rc.senseNearbyRobots(2, myTeam);
        if (adjacentAllies.length >= 3) {
            Movement.scatter();
        } else if (rc.getRoundNum() < 500 && !explored) {
            Movement.wander();
        } else {
            Movement.venture();
        }
    }

    public void paintLeftover() throws GameActionException {
        if (aggressiveMode || rushSoldier || builder || !rc.isActionReady()) {
            return;
        }
        if (aggressiveHold && rc.getPaint() < UnitType.SOLDIER.paintCapacity * 0.75) {
            return;
        }
        MapInfo locInfo = rc.senseMapInfo(rc.getLocation());
        if (!locInfo.getPaint().isAlly()) {
            MapLocation[] nearbyRuins = rc.senseNearbyRuins(8);
            if (nearbyRuins.length == 0) {
                rc.attack(rc.getLocation(), Util.useSecondary(rc.getLocation()));
            } else {
                MapLocation closestRuin = nearbyRuins[0];
                rc.attack(rc.getLocation(), Util.useSecondaryForTower(rc.getLocation(), closestRuin, towerType(rc.getLocation(), closestRuin)));
            }
        } else {
            MapInfo[] nearbyPaintLocations = rc.senseNearbyMapInfos();
            MapLocation[] nearbyRuins = rc.senseNearbyRuins(-1);
            for (MapInfo info : nearbyPaintLocations) {
                if (!info.getPaint().isAlly()) {
                    for (MapLocation ruin : nearbyRuins) {
                        if (info.getMapLocation().distanceSquaredTo(ruin) <= 8) {
                            rc.attack(info.getMapLocation(), Util.useSecondaryForTower(info.getMapLocation(), ruin, towerType(info.getMapLocation(), ruin)));
                            return;
                        }
                    }
                    rc.attack(info.getMapLocation(), Util.useSecondary(info.getMapLocation()));
                    return;
                }
            }
        }
    }

    public static UnitType towerType(MapLocation loc, MapLocation ruin) {
        double rand = randomFunction(loc.x, loc.y);
        if (rc.getRoundNum() < 150) {
            return UnitType.LEVEL_ONE_MONEY_TOWER;
        }
        if (rc.getRoundNum() < 400) {
            if (rand < 0.3) {
                return UnitType.LEVEL_ONE_PAINT_TOWER;
            } else {
                return UnitType.LEVEL_ONE_MONEY_TOWER;
            }
        } else if (rc.getRoundNum() < 1000) {
            if (rand < 0.5) {
                return UnitType.LEVEL_ONE_PAINT_TOWER;
            } else if (rand < 1) {
                return UnitType.LEVEL_ONE_MONEY_TOWER;
            } else {
                return UnitType.LEVEL_ONE_DEFENSE_TOWER;
            }
        } else {
            if (rand < 0.5) {
                return UnitType.LEVEL_ONE_PAINT_TOWER;
            } else if (rand < 1) {
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
