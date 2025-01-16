package sprint;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Message;

public class Unit extends Globals {

    public void act() throws GameActionException {
        Comms.readMessages();
        Comms.senseInfo();
    }
}
