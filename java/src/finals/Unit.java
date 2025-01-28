package finals;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;

public class Unit extends Globals {

    public void act() throws GameActionException {
//        if (rc.getRoundNum() > 10) {
//            rc.disintegrate();
//        }
        flush();
        calculateTowers();
        Comms.senseInfo();
//        if (rc.getType().isRobotType()) System.out.println("senseInfo: " + Clock.getBytecodeNum());
        Comms.readMessages();
//        if (rc.getType().isRobotType()) System.out.println("readMessages: " + Clock.getBytecodeNum());
    }

    public void flush() throws GameActionException{
        upgradeTowerLocation = null;
        nearbyAllies = new boolean[5][5];
        RobotInfo[] friendlyRobots = rc.senseNearbyRobots(8, myTeam);
        for (RobotInfo robot : friendlyRobots) {
            nearbyAllies[robot.location.x - rc.getLocation().x + 2][robot.location.y - rc.getLocation().y + 2] = true;
        }
        if (spawnLocation == null) {
            MapLocation[] ruins = rc.senseNearbyRuins(4);
            if (ruins.length == 0) {
                spawnLocation = rc.getLocation();
            } else {
                spawnLocation = ruins[0];
            }
            Logger.log("spawn from: " + spawnLocation);
            symmetryLocations[0] = new MapLocation(mapWidth - spawnLocation.x - 1, mapHeight - spawnLocation.y - 1);
            symmetryLocations[1] = new MapLocation(mapWidth - spawnLocation.x - 1, spawnLocation.y);
            symmetryLocations[2] = new MapLocation(spawnLocation.x, mapHeight - spawnLocation.y - 1);
        }
    }

    public void calculateTowers() throws GameActionException {
        chips[0] = chips[1];
        chips[1] = chips[2];
        chips[2] = chips[3];
        chips[3] = chips[4];
        chips[4] = rc.getChips();
        if (rc.getRoundNum() > 5) {
            int maxDiff = Math.max(Math.max(Math.max(Math.max(chips[4] - chips[3], chips[3] - chips[2]), chips[2] - chips[1]), chips[1] - chips[0]), 0);
            numMoneyTowers = maxDiff / 20;
        }
    }
}
