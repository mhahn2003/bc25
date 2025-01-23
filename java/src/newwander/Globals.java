package newwander;

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

    static FastSet friendlyTowerLocations;
    static FastSet enemyTowerLocations;
    static FastSet noRefillTowerLocations;
    static FastSet ruinLocations;

    final static int enemyLocMinDist = 16;
    static MapLocation[] latestEnemyLocations = new MapLocation[25];
    static int latestEnemyLocationIndex = 0;

    static int exploreXCols = 0;
    static int exploreYRows = 0;
    static MapLocation[][] exploreLocations;
    static boolean[][] exploreLocationsVisited;
    static MapLocation centerLocation;
    static boolean wandering = false;
    static MapLocation wanderLocation;
    static int minWanderDistance = 100;
    static int minDistToTarget = 999999;
    static int wanderCount = 0;
    static int maxWanderingCounter = 5;
    static int noActionCounter = 0;
    static int noActionThreshold = 10;
    static MapLocation flipLocation = null;

    static FastSet taintedRuins;
    static MapLocation spawnLocation;
    // diag, vert, horz
    static MapLocation[] symmetryLocations = new MapLocation[3];
    static boolean[] symmetryLocationsVisited = new boolean[3];
    static int symmetry = -1;
    static boolean[] symmetryBroken = new boolean[3];
    static FastSet vertCheckedLocations;
    static FastSet horzCheckedLocations;
    static FastSet diagCheckedLocations;

    static UnitType buildTowerType = null;
    static int noPaintCounter = 0;
    static int noPaintTowerThreshold = 5;
    static int noPaintSRPThreshold = 5;
    static int noMopCounter = 0;
    static int noMopTowerThreshold = 5;
    static int noMopSRPThreshold = 3;
    static boolean continueBuild = false;

    static FastSet impossibleRuinLocations;
    static FastSet impossibleSRPLocations;
    static MapLocation buildRuinLocation;
    static MapLocation buildSRPLocation;

    static MapLocation refillPaintTowerLocation = null;
    static int refillTowerDistanceThreshold = 100;
    static MapLocation upgradeTowerLocation = null;

    static boolean[][] nearbyAllies = new boolean[3][3];

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
    public static Direction [] cardinalDirections = {
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST
    };



    public static void init(RobotController robotController) {
        rc = robotController;

        mapWidth = rc.getMapWidth();
        mapHeight = rc.getMapHeight();

        myId = rc.getID();
        myTeam = rc.getTeam();
        opponentTeam = myTeam.opponent();

        centerLocation = new MapLocation(mapWidth / 2, mapHeight / 2);
        exploreXCols = (int) Math.ceil((mapWidth-2) / 9.0);
        exploreYRows = (int) Math.ceil((mapHeight-2) / 9.0);
        exploreLocations = new MapLocation[exploreXCols][exploreYRows];
        exploreLocationsVisited = new boolean[exploreXCols][exploreYRows];
        for (int i = 0; i < exploreXCols; i++) {
            for (int j = 0; j < exploreYRows; j++) {
                exploreLocations[i][j] = new MapLocation(i * 9+2, j * 9+2);
            }
        }

        friendlyTowerLocations = new FastSet();
        enemyTowerLocations = new FastSet();
        noRefillTowerLocations = new FastSet();
        ruinLocations = new FastSet();
        taintedRuins = new FastSet();
        vertCheckedLocations = new FastSet();
        horzCheckedLocations = new FastSet();
        diagCheckedLocations = new FastSet();
        impossibleRuinLocations = new FastSet();
        impossibleSRPLocations = new FastSet();

        switch (rc.getType()) {
            case SOLDIER: r = new Soldier(); break;
            case SPLASHER: r = new Splasher(); break;
            case MOPPER: r = new Mopper(); break;
            default: r = new Tower();
        }
    }
}
