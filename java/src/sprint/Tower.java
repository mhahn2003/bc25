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
            for (Direction dir : Globals.adjacentDirections) {
                MapLocation newLoc = loc.add(dir).add(dir);
                if (rc.canBuildRobot(UnitType.SOLDIER, newLoc)) {
                    rc.buildRobot(UnitType.SOLDIER, newLoc);
                    RobotInfo robot = rc.senseRobotAtLocation(newLoc);
                    initializeRobot(robot);
                    break;
                }
            }
        } else {
            if (rc.getChips() >= UnitType.SOLDIER.moneyCost + 200 && rc.getPaint() >= UnitType.SOLDIER.paintCost) {
                MapLocation loc = rc.getLocation();
                for (Direction dir : Globals.adjacentDirections) {
                    MapLocation newLoc = loc.add(dir).add(dir);
                    if (rc.canBuildRobot(UnitType.SOLDIER, newLoc)) {
                        rc.buildRobot(UnitType.SOLDIER, newLoc);
                        RobotInfo robot = rc.senseRobotAtLocation(newLoc);
                        initializeRobot(robot);
                        break;
                    }
                }
                for (Direction dir : Globals.adjacentDirections) {
                    MapLocation newLoc = loc.add(dir);
                    if (rc.canBuildRobot(UnitType.SOLDIER, newLoc)) {
                        rc.buildRobot(UnitType.SOLDIER, newLoc);
                        RobotInfo robot = rc.senseRobotAtLocation(newLoc);
                        initializeRobot(robot);
                        break;
                    }
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
        for (MapLocation enemyLoc : latestEnemyLocations) {
            if (enemyLoc != null) {
                Comms.addToMessageQueue(Comms.InfoCategory.ENEMY_UNIT, enemyLoc, true);
            }
        }
        for (MapLocation enemyTowerLoc : enemyTowerLocations) {
            if (enemyTowerLoc != null) {
                Comms.addToMessageQueue(Comms.InfoCategory.ENEMY_TOWER, enemyTowerLoc, true);
            }
        }
        for (MapLocation friendlyPaintTowerLoc : friendlyPaintTowerLocations) {
            if (friendlyPaintTowerLoc != null) {
                Comms.addToMessageQueue(Comms.InfoCategory.FRIEND_PAINT_TOWER, friendlyPaintTowerLoc, true);
            }
        }
        for (MapLocation friendlyNonPaintTowerLoc : friendlyNonPaintTowerLocations) {
            if (friendlyNonPaintTowerLoc != null) {
                Comms.addToMessageQueue(Comms.InfoCategory.FRIEND_NON_PAINT_TOWER, friendlyNonPaintTowerLoc, true);
            }
        }
    }

    @Override
    public void sendComms() throws GameActionException {
        Comms.initialize();
        while (true) {
            if (!Comms.sendMessages(new MapLocation(-1, -1), true)) {
                break;
            }
        }

    }
}
