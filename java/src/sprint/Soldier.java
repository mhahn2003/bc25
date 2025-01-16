package sprint;

import battlecode.common.*;

import java.util.Map;

public class Soldier extends Unit {

    // TODO: make an evade function that avoids tower range, especially defense towers
    // If we simply just move back, might get stuck in an infinite loop

    // Currently this might be false but unit still might be within tower range, i.e. low paint
    public static boolean aggressiveMode = false;
    public static boolean builder = false;

    public static final int attackDistanceThreshold = 5;

    public void act() throws GameActionException {
        super.act();
        attack();
        rush();
        build();
        paintSRP();
        move();
    }

    public void attack() throws GameActionException {
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, opponentTeam);
        for (RobotInfo robot : enemyRobots) {
            if (robot.getType().isTowerType()) {
                MapLocation robotLoc = robot.location;
                if (rc.canAttack(robotLoc)) {
                    rc.attack(robotLoc);
                    aggressiveMode = true;
                    if (rc.getLocation().distanceSquaredTo(robotLoc) > attackDistanceThreshold) {
                        Navigator.moveTo(robotLoc);
                    }
                    return;
                }
            }
        }
        aggressiveMode = false;
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
        } else if (rushSoldier) {
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
                }
            }
        }
        if (rc.getLocation().distanceSquaredTo(closestRuin) <= GameConstants.BUILD_TOWER_RADIUS_SQUARED) {
            if (rc.canCompleteTowerPattern(type, closestRuin)) {
                rc.completeTowerPattern(type, closestRuin);
                builder = false;
            }
        } else {
            Navigator.moveTo(closestRuin);
        }
    }

    public void paintSRP() throws GameActionException {

    }

    public void move() throws GameActionException {
        if (aggressiveMode) {
            return;
        }
        if (builder) {
            return;
        }
        if (rc.getRoundNum() < 200) {

        } else if (rc.getRoundNum() < 500 && !explored) {
            if (wandering) {
                if (wanderingCounter >= maxWanderingCounter) {
                    wandering = false;
                    wanderingCounter = 0;
                    exploreLocationsVisited[wanderIndex] = true;
                } else {
                    Navigator.moveTo(exploreLocations[wanderIndex]);
                    wanderingCounter++;
                }
            }
            wandering = true;
            // wander around
            int wanderIndex = rc.getID() % 9;
            for (int i = 0; i < 9; i++) {
                if (exploreLocationsVisited[(wanderIndex + i) % 9]) {
                    continue;
                }
                MapLocation loc = Globals.exploreLocations[(wanderIndex + i) % 9];
                Globals.wanderIndex = i;
                Navigator.moveTo(loc);
                break;
            }
        } else {
            // spread out to enemy territory
        }
    }

    public static UnitType towerType(MapLocation loc, MapLocation ruin) {
        double rand = randomFunction(loc.x, loc.y);
        if (rc.getRoundNum() < 200) {
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
