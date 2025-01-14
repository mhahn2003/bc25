package sprint;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Message;

public class Unit extends Globals {
    static boolean init = false;
    static MapLocation[] friendlyTowerLocations = new MapLocation[25];
    static MapLocation[] friendlyPaintTowerLocations = new MapLocation[25];
    static MapLocation[] enemyTowerLocations = new MapLocation[25];
    static FastSet ruinLocations;

    public void act() throws GameActionException {
        if (!init) init();
        readComms();

        sendComms();
    }

    public void init() throws GameActionException {
        init = true;

    }

    public void readComms() throws GameActionException {
        Message[] messages = rc.readMessages(rc.getRoundNum());
        for (Message m : messages) {

        }
    }

    public void sendComms() throws GameActionException {

    }
}
