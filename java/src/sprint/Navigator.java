package sprint;

import battlecode.common.*;

public class Navigator extends Globals {
    private static MapLocation currentTarget;

    private static int minDistanceToTarget;
    private static int roundsSinceMovingCloserToTarget;

    public static void moveTo(MapLocation target) throws GameActionException {
        MapLocation myLocation = rc.getLocation();
        rc.setIndicatorLine(rc.getLocation(), target, 255, 0, 0);

        if (myLocation.equals(target)) {
            return;
        }

        if (currentTarget == null || !currentTarget.equals(target)) {
            reset();
        }

        currentTarget = target;

        MapLocation nextLocation = myLocation.add(myLocation.directionTo(target));
        if (rc.canSenseLocation(nextLocation) && rc.senseMapInfo(nextLocation).isWall()) {
            return;
        }

        int distanceToTarget = myLocation.distanceSquaredTo(target);
        if (distanceToTarget < minDistanceToTarget) {
            minDistanceToTarget = distanceToTarget;
            roundsSinceMovingCloserToTarget = 0;
        } else {
            roundsSinceMovingCloserToTarget++;
        }

        if (roundsSinceMovingCloserToTarget < 3) {
            int startBytecodes = Clock.getBytecodeNum();
            Direction bellmanFordDirection = BellmanFordNavigator.getBestDirection(target);
            int endBytecodes = Clock.getBytecodeNum();
            System.out.println("BellmanFordNavigator: " + (endBytecodes - startBytecodes) + " bytecodes");
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

        if (!rc.isMovementReady()) {
            return;
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
