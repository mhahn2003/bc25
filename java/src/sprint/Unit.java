package sprint;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Message;

public class Unit extends Globals {

    public void act() throws GameActionException {
        Comms.senseInfo();
        System.out.println("senseInfo: " + Clock.getBytecodeNum());
        Comms.readMessages();
        System.out.println("readMessages: " + Clock.getBytecodeNum());
    }
}
