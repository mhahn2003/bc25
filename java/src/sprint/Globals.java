package sprint;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;
import battlecode.common.Direction;

/*
Adapted from camel_case's 2024 submission:
https://github.com/jmerle/battlecode-2024
 */
public class Globals {
    public static RobotController rc;

    public static int mapWidth;
    public static int mapHeight;

    public static int myId;
    public static Team myTeam;
    public static Team opponentTeam;
    public static Unit r;

    static MapLocation[] friendlyNonPaintTowerLocations = new MapLocation[25];
    static MapLocation[] friendlyPaintTowerLocations = new MapLocation[25];
    static MapLocation[] enemyTowerLocations = new MapLocation[25];
    static FastSet ruinLocations;

    public static Direction[] allDirections = Direction.values();
    public static Direction[] adjacentDirections = {
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST,
            Direction.NORTHEAST,
            Direction.SOUTHEAST,
            Direction.SOUTHWEST,
            Direction.NORTHWEST
    };

    public static void init(RobotController robotController) {
        rc = robotController;

        mapWidth = rc.getMapWidth();
        mapHeight = rc.getMapHeight();

        myId = rc.getID();
        myTeam = rc.getTeam();
        opponentTeam = myTeam.opponent();

        switch (rc.getType()) {
            case SOLDIER: r = new Soldier(); break;
            case SPLASHER: r = new Splasher(); break;
            case MOPPER: r = new Mopper(); break;
            default: r = new Tower();
        }
    }
}
