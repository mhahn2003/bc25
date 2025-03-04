package finals;

import battlecode.common.*;

/*
Adapted from camel_case's 2024 submission:
https://github.com/jmerle/battlecode-2024
 */
public class Globals {
    public static RobotController rc;

    public static int mapWidth;
    public static int mapHeight;

    public static int midX1;
    public static int midX2;
    public static int midY1;
    public static int midY2;

    public static int myId;
    public static Team myTeam;
    public static Team opponentTeam;
    public static Unit r;

    static FastSet friendlyTowerLocations;
    static FastSet enemyTowerLocations;
    static FastSet noRefillTowerLocations;
    static FastSet flickerTowerLocations;
    static FastSet ruinLocations;
    static int numMoneyTowers = 1;
    static int[] chips = new int[5];

    final static int enemyLocMinDist = 16;
    static MapLocation[] latestEnemyLocations = new MapLocation[25];
    static int latestEnemyLocationIndex = 0;

    static MapLocation[] exploreLocations = new MapLocation[9];
    static boolean[] exploreLocationsVisited = new boolean[9];
    static boolean wandering = false;
    static MapLocation wanderLocation;
    static int minWanderDistance = 100;
    static int minDistToTarget = 999999;
    static int wanderCount = 0;
    static int maxWanderingCounter = 5;
    static int noActionCounter = 0;
    static int noActionThreshold = 10;
    static int noFlickerCounter = 0;
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
    static int noPaintTowerThreshold = 3;
    static int noPaintSRPThreshold = 3;
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
    static MapLocation currentFlickerTowerLocation = null;

    static boolean[][] nearbyAllies = new boolean[3][3];
    static MapLocation closestDefenseTower = null;
    static MapLocation closestEnemyTower = null;

    static boolean noNearbyAllyPaint = false;

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
        if (mapWidth % 2 == 1) {
            midX1 = mapWidth / 2;
            midX2 = mapWidth / 2;
        } else {
            midX1 = mapWidth / 2 - 1;
            midX2 = mapWidth / 2;
        }
        if (mapHeight % 2 == 1) {
            midY1 = mapHeight / 2;
            midY2 = mapHeight / 2;
        } else {
            midY1 = mapHeight / 2 - 1;
            midY2 = mapHeight / 2;
        }

        myId = rc.getID();
        myTeam = rc.getTeam();
        opponentTeam = myTeam.opponent();

        int minX = 2;
        int midX = mapWidth / 2;
        int maxX = mapWidth - 3;
        int minY = 2;
        int midY = mapHeight / 2;
        int maxY = mapHeight - 3;
        exploreLocations[0] = new MapLocation(minX, minY);
        exploreLocations[1] = new MapLocation(midX, minY);
        exploreLocations[2] = new MapLocation(maxX, minY);
        exploreLocations[3] = new MapLocation(minX, midY);
        exploreLocations[4] = new MapLocation(midX, midY);
        exploreLocations[5] = new MapLocation(maxX, midY);
        exploreLocations[6] = new MapLocation(minX, maxY);
        exploreLocations[7] = new MapLocation(midX, maxY);
        exploreLocations[8] = new MapLocation(maxX, maxY);

        friendlyTowerLocations = new FastSet();
        enemyTowerLocations = new FastSet();
        noRefillTowerLocations = new FastSet();
        flickerTowerLocations = new FastSet();
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
