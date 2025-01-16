package sprint;

import battlecode.common.*;

public class Soldier extends Unit {

    // TODO: make an evade function that avoids tower range, especially defense towers
    // If we simply just move back, might get stuck in an infinite loop

    // Currently this might be false but unit still might be within tower range, i.e. low paint
    public static boolean aggressiveMode = false;
    public static boolean builder = false;

    public void act() throws GameActionException {
        super.act();
        attack();
        build();
        paintSRP();
        move();
    }

    public void attack() throws GameActionException {
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, opponentTeam);
        for (RobotInfo robot : enemyRobots) {
            if (robot.getType().isTowerType()) {
                if (rc.canAttack(robot.location)) {
                    rc.attack(robot.location);
                    aggressiveMode = true;
                    return;
                }
            }
        }
        aggressiveMode = false;
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
