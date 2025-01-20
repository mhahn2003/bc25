package sprint;

import battlecode.common.*;

public class Movement extends Globals {
    public static void scatter() throws GameActionException {
        Direction bestDir = null;
        int minAdjacentAllies = 999999;
        for (Direction dir : Globals.allDirections) {
            if (rc.canMove(dir)) {
                MapLocation loc = rc.getLocation().add(dir);
                RobotInfo[] nearbyAllies = rc.senseNearbyRobots(loc, 2, myTeam);
                if (nearbyAllies.length < minAdjacentAllies) {
                    minAdjacentAllies = nearbyAllies.length;
                    bestDir = dir;
                }
            }
        }
        if (bestDir != null) {
            rc.move(bestDir);
        }
    }

    public static Direction wanderDirection() throws GameActionException {
        if (wandering) {
            if (rc.getLocation().distanceSquaredTo(exploreLocations[wanderIndex]) < minDistToTarget) {
                wanderCount = 0;
            } else {
                wanderCount++;
            }

            if (wanderCount >= maxWanderingCounter) {
                wandering = false;
                exploreLocationsVisited[wanderIndex] = true;
            }

            if (rc.canSenseLocation(exploreLocations[wanderIndex])) {
                wandering = false;
                exploreLocationsVisited[wanderIndex] = true;
            }
        }

        if (!wandering) {
            wandering = true;
            int wanderIndex = -1;
            int maxDist = 0;
            for (int i = 0; i < 9; i++) {
                if (exploreLocationsVisited[i]) {
                    continue;
                }
                int dist = rc.getLocation().distanceSquaredTo(exploreLocations[i]);
                if (dist > maxDist) {
                    maxDist = dist;
                    wanderIndex = i;
                }
            }
            if (wanderIndex != -1) {
                Movement.wanderIndex = wanderIndex;
                minDistToTarget = maxDist;
                wanderCount = 0;
            } else {
                wandering = false;
                for (int i = 0; i < 9; i++) {
                    exploreLocationsVisited[i] = false;
                }
                return wanderDirection();
            }
        }

        MapLocation wanderLoc = exploreLocations[wanderIndex];
        Direction bestDir = null;
        int maxHeuristic = Integer.MIN_VALUE;
        for (Direction dir : Globals.adjacentDirections) {
            if (rc.canMove(dir)) {
                MapLocation loc = rc.getLocation().add(dir);
                PaintType paint = rc.senseMapInfo(loc).getPaint();
                int heuristic = -Util.paintPenalty(loc, paint) - loc.distanceSquaredTo(wanderLoc);
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
