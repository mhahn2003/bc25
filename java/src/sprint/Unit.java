package sprint;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Message;

public class Unit extends Globals {

    public void act() throws GameActionException {
//        if (rc.getRoundNum() > 50) {
//            rc.disintegrate();
//        }
        Comms.senseInfo();
//        if (rc.getType().isRobotType()) System.out.println("senseInfo: " + Clock.getBytecodeNum());
        Comms.readMessages();
//        if (rc.getType().isRobotType()) System.out.println("readMessages: " + Clock.getBytecodeNum());
    }
}
