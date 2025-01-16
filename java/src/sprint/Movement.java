package sprint;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;

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

    public static void wander() throws GameActionException {
        if (wandering) {
            if (wanderingCounter >= maxWanderingCounter) {
                wanderingCounter = 0;
                exploreLocationsVisited[wanderIndex] = true;
                wandering = false;
            } else {
                Navigator.moveTo(exploreLocations[wanderIndex]);
                wanderingCounter++;
            }
        }
        if (!wandering) {
            wandering = true;
            int wanderIndex = rc.getID() % 9;
            for (int i = 0; i < 9; i++) {
                if (exploreLocationsVisited[(wanderIndex + i) % 9]) {
                    continue;
                }
                MapLocation loc = Globals.exploreLocations[(wanderIndex + i) % 9];
                Globals.wanderIndex = (wanderIndex + i) % 9;
                Navigator.moveTo(loc);
                break;
            }
        }
    }

    public static void venture() throws GameActionException {
        if (targetEnemyTowerLocation == null) {
            MapLocation closesetEnemyTowerLocation = null;
            int minDist = 999999;
            for (MapLocation loc : enemyTowerLocations) {
                if (loc == null) {
                    break;
                }
                if (!loc.equals(new MapLocation(-1, -1))) {
                    int dist = rc.getLocation().distanceSquaredTo(loc);
                    if (dist < minDist) {
                        minDist = dist;
                        closesetEnemyTowerLocation = loc;
                    }
                }
            }

        }
    }
}
