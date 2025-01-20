package sprint;

import battlecode.common.*;

public class Tower extends Unit {
    static boolean init = false;
    static boolean startingTower = false;
    static int newTowerChipThreshold = 1000;
    static int roundsSinceLastAttack = 0;

    public void act() throws GameActionException {
        if (!init) {
            init = true;
            if (rc.getRoundNum() == 1) {
                startingTower = true;
            }
            if (rc.getType().getBaseType() == UnitType.LEVEL_ONE_PAINT_TOWER) {
                friendlyPaintTowerLocations.add(rc.getLocation());
            } else {
                friendlyNonPaintTowerLocations.add(rc.getLocation());
            }
        }
        super.act();
        attackEnemyUnits();
        spawn();
        requestUpgrade();
        sendComms();
    }

    public void attackEnemyUnits() throws GameActionException {
        RobotInfo[] totalEnemyUnits = rc.senseNearbyRobots(-1, opponentTeam);
        RobotInfo[] enemyUnits = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, opponentTeam);
        if (totalEnemyUnits.length == 0) {
            roundsSinceLastAttack += 1;
            return;
        }
        roundsSinceLastAttack = 0;
        if (enemyUnits.length == 0) {
            return;
        }
        rc.attack(null);
        MapLocation enemyToAttack = enemyUnits[0].getLocation();
        int minEnemyHealth = enemyUnits[0].health;
        for (RobotInfo enemyUnit : enemyUnits) {
            if (enemyUnit.health < minEnemyHealth) {
                minEnemyHealth = enemyUnit.health;
                enemyToAttack = enemyUnit.getLocation();
            }
        }
        if (rc.canAttack(enemyToAttack)) {
            rc.attack(enemyToAttack);
        }
    }

    public void spawn() throws GameActionException {
        if (startingTower && rc.getRoundNum() <= 3) {
            MapLocation loc = rc.getLocation();
            Direction dir = loc.directionTo(exploreLocations[4]);
            for (int i = 0; i < 8; i++) {
                MapLocation newLoc = loc.add(dir).add(dir);
                if (buildRobot(UnitType.SOLDIER, newLoc)) {
                    break;
                }
                MapLocation newLoc2 = loc.add(dir);
                if (buildRobot(UnitType.SOLDIER, newLoc2)) {
                    break;
                }
                dir = dir.rotateRight();
            }
        } else if (rc.getType().getBaseType() != UnitType.LEVEL_ONE_PAINT_TOWER) {
            // TODO : spawn logic for new money towers newly minted
            if (rc.getChips() >= UnitType.MOPPER.moneyCost + newTowerChipThreshold && rc.getPaint() == UnitType.MOPPER.paintCost) {
                MapLocation loc = rc.getLocation();
                Direction dir = loc.directionTo(exploreLocations[4]);
                for (int i = 0; i < 8; i++) {
                    MapLocation newLoc = loc.add(dir).add(dir);
                    if (buildRobot(UnitType.MOPPER, newLoc)) {
                        break;
                    }
                    MapLocation newLoc2 = loc.add(dir);
                    if (buildRobot(UnitType.MOPPER, newLoc2)) {
                        break;
                    }
                    dir = dir.rotateRight();
                }
            }
        } else {
            if (rc.getChips() >= UnitType.SOLDIER.moneyCost + newTowerChipThreshold && rc.getPaint() >= UnitType.SOLDIER.paintCost + 100) {
                MapLocation loc = rc.getLocation();
                Direction dir = loc.directionTo(exploreLocations[4]);
                for (int i = 0; i < 8; i++) {
                    MapLocation newLoc = loc.add(dir).add(dir);
                    if (buildRobot(UnitType.SOLDIER, newLoc)) {
                        break;
                    }
                    MapLocation newLoc2 = loc.add(dir);
                    if (buildRobot(UnitType.SOLDIER, newLoc2)) {
                        break;
                    }
                    dir = dir.rotateRight();
                }
            }
        }
    }

    public void requestUpgrade() throws GameActionException {
        // TODO : different criteria for defense towers
        if (roundsSinceLastAttack >= 40 && rc.getChips() >= Util.getUpgradeCost(rc.getType())) {
            RobotInfo[] friendlyRobots = rc.senseNearbyRobots(-1, myTeam);
            RobotInfo closestFriendlyRobot = null;
            int minDist = Integer.MAX_VALUE;
            for (RobotInfo friendlyRobot : friendlyRobots) {
                if (friendlyRobot.getType().isRobotType() && friendlyRobot.getPaintAmount() >= 10 && rc.canSendMessage(friendlyRobot.getLocation())) {
                    int dist = rc.getLocation().distanceSquaredTo(friendlyRobot.getLocation());
                    if (dist < minDist) {
                        minDist = dist;
                        closestFriendlyRobot = friendlyRobot;
                    }
                }
            }
            if (closestFriendlyRobot != null) {
                int message = Comms.encodeMessage(Comms.InfoCategory.UPGRADE, rc.getLocation());
                message = Comms.combineMessage(message, 0);
                if (rc.canSendMessage(closestFriendlyRobot.getLocation(), message)) {
                    rc.sendMessage(closestFriendlyRobot.getLocation(), message);
                }
            }
        }
    }

    public void initializeRobot(RobotInfo robot) throws GameActionException {
        if (Comms.initializing) {
            return;
        }
        Comms.initializing = true;
        Comms.initializingUnitId = robot.getID();
        Comms.initializeMessageQueue = new int[40];
        MapLocation[] ruins = ruinLocations.getLocations();
        for (MapLocation ruin : ruins) {
            Comms.addToMessageQueue(Comms.InfoCategory.RUIN, ruin, true);
        }
        MapLocation[] friendlyPaintTowerLocations = Globals.friendlyPaintTowerLocations.getLocations();
        for (MapLocation friendlyPaintTowerLoc : friendlyPaintTowerLocations) {
            Comms.addToMessageQueue(Comms.InfoCategory.FRIEND_PAINT_TOWER, friendlyPaintTowerLoc, true);
        }
        MapLocation[] enemyNonDefenseTowerLocations = Globals.enemyNonDefenseTowerLocations.getLocations();
        for (MapLocation enemyNonDefenseTowerLoc : enemyNonDefenseTowerLocations) {
            Comms.addToMessageQueue(Comms.InfoCategory.ENEMY_NON_DEFENSE_TOWER, enemyNonDefenseTowerLoc, true);
        }
        MapLocation[] enemyDefenseTowerLocations = Globals.enemyDefenseTowerLocations.getLocations();
        for (MapLocation enemyDefenseTowerLoc : enemyDefenseTowerLocations) {
            Comms.addToMessageQueue(Comms.InfoCategory.ENEMY_DEFENSE_TOWER, enemyDefenseTowerLoc, true);
        }
        for (int i = 0; i < exploreLocations.length; i++) {
            if (exploreLocationsVisited[i]) {
                Comms.addToMessageQueue(Comms.InfoCategory.EXPLORE_LOC_VISITED, exploreLocations[i], true);
            }
        }
    }

    public boolean buildRobot(UnitType unitType, MapLocation loc) throws GameActionException {
        if (rc.canBuildRobot(unitType, loc)) {
            rc.buildRobot(unitType, loc);
            RobotInfo robot = rc.senseRobotAtLocation(loc);
            initializeRobot(robot);
            return true;
        }

        return false;
    }

    public void sendComms() throws GameActionException {
        Comms.pushInitializeQueue();
        // broadcast every 20 rounds
        if (rc.getID() % 20 == rc.getRoundNum()) {
            while (true) {
                if (!Comms.sendMessages(new MapLocation(-1, -1), true)) {
                    break;
                }
            }
        }
    }
}
