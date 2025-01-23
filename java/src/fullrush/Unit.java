package fullrush;

import battlecode.common.GameActionException;
import battlecode.common.RobotInfo;

public class Unit extends Globals {

    public void act() throws GameActionException {
//        if (rc.getRoundNum() > 10) {
//            rc.disintegrate();
//        }
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
        for (RobotInfo robot : friendlyRobots) {
            nearbyAllies[robot.location.x - rc.getLocation().x + 2][robot.location.y - rc.getLocation().y + 2] = true;
        }
    }
}
