package sprint;

import battlecode.common.*;

public class Tower extends Unit {
    static boolean init = false;
    static boolean startingTower = false;

    public void act() throws GameActionException {
        if (!init) {
            init = true;
            if (rc.getRoundNum() == 1) {
                startingTower = true;
            }
            if (rc.getType().getBaseType() == UnitType.LEVEL_ONE_PAINT_TOWER) {
                friendlyPaintTowerLocations[0] = rc.getLocation();
            } else {
                friendlyNonPaintTowerLocations[0] = rc.getLocation();
            }
        }
        super.act();
        attackEnemyUnits();
        spawn();
        sendComms();
    }

    public void attackEnemyUnits() throws GameActionException {
        RobotInfo[] enemyUnits = rc.senseNearbyRobots(-1, opponentTeam);
        if (enemyUnits.length == 0) {
            return;
        }
        rc.attack(null);
        MapLocation enemyToAttack = enemyUnits[0].location;
        int minEnemyHealth = enemyUnits[0].health;
        for (RobotInfo enemyUnit : enemyUnits) {
            if (enemyUnit.health < minEnemyHealth) {
                minEnemyHealth = enemyUnit.health;
                enemyToAttack = enemyUnit.location;
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
                if (rc.canBuildRobot(UnitType.SOLDIER, newLoc)) {
                    rc.buildRobot(UnitType.SOLDIER, newLoc);
                    RobotInfo robot = rc.senseRobotAtLocation(newLoc);
                    initializeRobot(robot);
                    break;
                }
                MapLocation newLoc2 = loc.add(dir);
                if (rc.canBuildRobot(UnitType.SOLDIER, newLoc2)) {
                    rc.buildRobot(UnitType.SOLDIER, newLoc2);
                    RobotInfo robot = rc.senseRobotAtLocation(newLoc2);
                    initializeRobot(robot);
                    break;
                }
                dir = dir.rotateRight();
            }
        } else {
            if (rc.getChips() >= UnitType.SOLDIER.moneyCost + 200 && rc.getPaint() >= UnitType.SOLDIER.paintCost) {
                MapLocation loc = rc.getLocation();
                Direction dir = loc.directionTo(exploreLocations[4]);
                for (int i = 0; i < 8; i++) {
                    MapLocation newLoc = loc.add(dir).add(dir);
                    if (rc.canBuildRobot(UnitType.SOLDIER, newLoc)) {
                        rc.buildRobot(UnitType.SOLDIER, newLoc);
                        RobotInfo robot = rc.senseRobotAtLocation(newLoc);
                        initializeRobot(robot);
                        break;
                    }
                    MapLocation newLoc2 = loc.add(dir);
                    if (rc.canBuildRobot(UnitType.SOLDIER, newLoc2)) {
                        rc.buildRobot(UnitType.SOLDIER, newLoc2);
                        RobotInfo robot = rc.senseRobotAtLocation(newLoc2);
                        initializeRobot(robot);
                        break;
                    }
                    dir = dir.rotateRight();
                }
            }
        }
    }

    // TODO: make this compatible with spawning multiple robots in a row
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
        for (MapLocation enemyNonDefenseTowerLoc : enemyNonDefenseTowerLocations) {
            if (enemyNonDefenseTowerLoc != null) {
                Comms.addToMessageQueue(Comms.InfoCategory.ENEMY_NON_DEFENSE_TOWER, enemyNonDefenseTowerLoc, true);
            }
        }
        for (MapLocation enemyDefenseTowerLoc : enemyDefenseTowerLocations) {
            if (enemyDefenseTowerLoc != null) {
                Comms.addToMessageQueue(Comms.InfoCategory.ENEMY_DEFENSE_TOWER, enemyDefenseTowerLoc, true);
            }
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
