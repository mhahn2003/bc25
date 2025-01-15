package sprint;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Message;

public class Unit extends Globals {
    static boolean init = false;

    public void act() throws GameActionException {
        if (!init) init();
        Comms.readMessages();

        sendComms();
    }

    public void init() throws GameActionException {
        init = true;

    }

    public void sendComms() throws GameActionException {

    }
}
