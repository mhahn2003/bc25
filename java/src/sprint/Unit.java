package sprint;

import battlecode.common.*;

public class Unit extends Globals {

    public void act() throws GameActionException {
        if (rc.getRoundNum() > 15) {
            rc.disintegrate();
        }
        flush();
        Comms.senseInfo();
//        if (rc.getType().isRobotType()) System.out.println("senseInfo: " + Clock.getBytecodeNum());
        Comms.readMessages();
//        if (rc.getType().isRobotType()) System.out.println("readMessages: " + Clock.getBytecodeNum());
    }

    public void flush() throws GameActionException{
        upgradeTowerLocation = null;
        nearbyAllies = new boolean[5][5];
        RobotInfo[] friendlyRobots = rc.senseNearbyRobots(8, myTeam);
        MapLocation tower = null;
        for (RobotInfo robot : friendlyRobots) {
            if (robot.getType().isTowerType()) {
                tower = robot.location;
            }
            nearbyAllies[robot.location.x - rc.getLocation().x + 2][robot.location.y - rc.getLocation().y + 2] = true;
        }
        if (spawnTowerLocation == null && rc.getType().isRobotType()) {
            spawnTowerLocation = tower;
            friendlyPaintTowerLocations.add(tower);
        }
    }
}
