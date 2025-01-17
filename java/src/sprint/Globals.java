package sprint;

import battlecode.common.*;

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

    static FastSet friendlyNonPaintTowerLocations = new FastSet();
    static FastSet friendlyPaintTowerLocations = new FastSet();
    static FastSet enemyNonDefenseTowerLocations = new FastSet();
    static FastSet enemyDefenseTowerLocations = new FastSet();
    static FastSet ruinLocations = new FastSet();

    // TODO: make enemy locations expire after a while
    final static int enemyLocMinDist = 16;
    static MapLocation[] latestEnemyLocations = new MapLocation[25];
    static int latestEnemyLocationIndex = 0;

    static MapLocation[] exploreLocations = new MapLocation[9];
    static boolean[] exploreLocationsVisited = new boolean[9];
    static boolean explored = false;
    static boolean wandering = false;
    static int wanderIndex = 0;
    static int wanderingCounter = 0;
    final static int maxWanderingCounter = 50;

    static boolean rushSoldier = false;
    static MapLocation[] symmetryLocations = new MapLocation[3];
    static boolean[] symmetryLocationsVisited = new boolean[3];

    static MapLocation targetEnemyTowerLocation = null;
    static boolean aggressiveHold = false;

    static UnitType buildTowerType = null;
    static int noPaintCounter = 0;
    static int noPaintTowerThreshold = 5;
    static int noPaintSRPThreshold = 3;

    static FastSet impossibleRuins = new FastSet();
    static FastSet impossibleSRPLocations = new FastSet();
    static MapLocation buildRuinLocation;
    static MapLocation buildSRPLocation;

    static MapLocation refillPaintTowerLocation = null;

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

        int minX = 0;
        int midX = mapWidth / 2;
        int maxX = mapWidth - 1;
        int minY = 0;
        int midY = mapHeight / 2;
        int maxY = mapHeight - 1;
        exploreLocations[0] = new MapLocation(minX, minY);
        exploreLocations[1] = new MapLocation(midX, minY);
        exploreLocations[2] = new MapLocation(maxX, minY);
        exploreLocations[3] = new MapLocation(minX, midY);
        exploreLocations[4] = new MapLocation(midX, midY);
        exploreLocations[5] = new MapLocation(maxX, midY);
        exploreLocations[6] = new MapLocation(minX, maxY);
        exploreLocations[7] = new MapLocation(midX, maxY);
        exploreLocations[8] = new MapLocation(maxX, maxY);

        switch (rc.getType()) {
            case SOLDIER: r = new Soldier(); break;
            case SPLASHER: r = new Splasher(); break;
            case MOPPER: r = new Mopper(); break;
            default: r = new Tower();
        }
    }
}
