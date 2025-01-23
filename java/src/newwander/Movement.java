package newwander;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.PaintType;

public class Movement extends Globals {

    public static Direction wanderDirection() throws GameActionException {
        if (wandering) {
            if (rc.getLocation().distanceSquaredTo(wanderLocation) < minDistToTarget) {
                wanderCount = 0;
            } else {
                wanderCount++;
            }

            if (wanderCount >= maxWanderingCounter || rc.canSenseLocation(wanderLocation)) {
                wandering = false;
                exploreLocationsVisited[wanderLocation.x / 9][wanderLocation.y / 9] = true;
            }
        }

        if (!wandering) {
            wandering = true;
            FastSet adjacentWanderLocations = new FastSet();
            int exploreX = rc.getLocation().x / 9;
            int exploreY = rc.getLocation().y / 9;
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    if (exploreX + i >= 0 && exploreX + i < exploreXCols && exploreY + j >= 0 && exploreY + j < exploreYRows) {
                        if (!exploreLocationsVisited[exploreX + i][exploreY + j]) {
                            adjacentWanderLocations.add(exploreLocations[exploreX + i][exploreY + j]);
                        }
                    }
                }
            }

            MapLocation[] wanderLocs = adjacentWanderLocations.getLocations();
            if (wanderLocs.length > 0) {
                wanderLocation = wanderLocs[rc.getID() % wanderLocs.length];
                minDistToTarget = rc.getLocation().distanceSquaredTo(wanderLocation);
                wanderCount = 0;
            } else {
                FastSet farWanderLocations = new FastSet();
                for (int i = 0; i < exploreXCols; i++) {
                    for (int j = 0; j < exploreYRows; j++) {
                        if (!exploreLocationsVisited[i][j]) {
                            farWanderLocations.add(exploreLocations[i][j]);
                        }
                    }
                }
                MapLocation[] wanderLocs2 = farWanderLocations.getLocations();
                if (wanderLocs2.length > 0) {
                    wanderLocation = wanderLocs2[rc.getID() % wanderLocs2.length];
                    minDistToTarget = rc.getLocation().distanceSquaredTo(wanderLocation);
                    wanderCount = 0;
                } else {
                    wandering = false;
                    for (int i = 0; i < exploreXCols; i++) {
                        for (int j = 0; j < exploreYRows; j++) {
                            exploreLocationsVisited[i][j] = false;
                        }
                    }
                    return wanderDirection();
                }
            }
        }

        Direction bestDir = null;
        int maxHeuristic = Integer.MIN_VALUE;
        for (Direction dir : Globals.adjacentDirections) {
            if (rc.canMove(dir)) {
                MapLocation loc = rc.getLocation().add(dir);
                PaintType paint = rc.senseMapInfo(loc).getPaint();
                int heuristic = -Util.paintPenalty(loc, paint) - loc.distanceSquaredTo(wanderLocation);
                if (heuristic > maxHeuristic) {
                    maxHeuristic = heuristic;
                    bestDir = dir;
                }
            }
        }
        return bestDir;
    }

//    public static void attackDefenseTower() throws GameActionException {
//        RobotInfo[] allies = rc.senseNearbyRobots(targetEnemyTowerLocation, 36, myTeam);
//        if (allies.length >= 4) {
//            Navigator.moveTo(targetEnemyTowerLocation);
//            if (rc.canAttack(targetEnemyTowerLocation)) {
//                rc.attack(targetEnemyTowerLocation);
//            }
//        } else {
//            int curDist = rc.getLocation().distanceSquaredTo(targetEnemyTowerLocation);
//            if (curDist <= 16) {
//                Direction bestDir = null;
//                int maxDist = 0;
//                for (Direction dir : Globals.allDirections) {
//                    if (rc.canMove(dir)) {
//                        MapLocation loc = rc.getLocation().add(dir);
//                        int dist = loc.distanceSquaredTo(targetEnemyTowerLocation);
//                        if (dist > maxDist) {
//                            maxDist = dist;
//                            bestDir = dir;
//                        }
//                    }
//                }
//                if (bestDir != null && maxDist > curDist) {
//                    rc.move(bestDir);
//                }
//            } else if (curDist > 20) {
//                Direction bestDir = null;
//                int minDist = 999999;
//                for (Direction dir : Globals.allDirections) {
//                    if (rc.canMove(dir)) {
//                        MapLocation loc = rc.getLocation().add(dir);
//                        int dist = loc.distanceSquaredTo(targetEnemyTowerLocation);
//                        if (dist > 16 && dist < minDist) {
//                            minDist = dist;
//                            bestDir = dir;
//                        }
//                    }
//                }
//                if (bestDir != null) {
//                    rc.move(bestDir);
//                }
//            }
//        }
//    }
}
