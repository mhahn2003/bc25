package dumbsrp;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

public class Navigator extends Globals {
    private static MapLocation currentTarget;

    private static int minDistanceToTarget;
    private static int roundsSinceMovingCloserToTarget;

    public static void moveTo(MapLocation target) throws GameActionException {
        MapLocation myLocation = rc.getLocation();
        if (myLocation.equals(target)) {
            return;
        }

        if (currentTarget == null || !currentTarget.equals(target)) {
            reset();
        }

        currentTarget = target;

        if (!rc.isMovementReady()) {
            return;
        }

        int distanceToTarget = myLocation.distanceSquaredTo(target);
        if (distanceToTarget < minDistanceToTarget) {
            minDistanceToTarget = distanceToTarget;
            roundsSinceMovingCloserToTarget = 0;
        } else {
            roundsSinceMovingCloserToTarget++;
        }

        if (roundsSinceMovingCloserToTarget < 3 && Clock.getBytecodesLeft() > 6000) {
            Direction bellmanFordDirection = BellmanFordNavigator.getBestDirection(target);
            if (bellmanFordDirection != null) {
                if (rc.canMove(bellmanFordDirection)) {
                    rc.move(bellmanFordDirection);
                }
                Logger.log("bf " + bellmanFordDirection);
                return;
            } else {
                Logger.log("bf null");
            }
        } else {
            Logger.log("bf n/a");
        }

        BugNavigator.moveTo(target);
    }

    public static void reset() {
        currentTarget = null;

        minDistanceToTarget = Integer.MAX_VALUE;
        roundsSinceMovingCloserToTarget = 0;

        BugNavigator.reset();
    }
}
