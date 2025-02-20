package testsprint;

import battlecode.common.*;

public class Tower extends Unit {
    static boolean init = false;
    static boolean startingTower = false;
    static int newTowerChipThreshold = 1000;
    static int roundsSinceLastAttack = 0;

    static int spawnedSoldiers = 0;
    static int spawnedMoppers = 0;
    static int spawnedSplashers = 0;

    static int spawnedDefenseMoppers = 0;

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
            if (tryBuildRobot(UnitType.SOLDIER, dir)) {
                return;
            }
        } else {
            if (rc.getRoundNum() > 4) {
                // defend
                RobotInfo[] enemyUnits = rc.senseNearbyRobots(-1, opponentTeam);
                if (enemyUnits.length > 0) {
                    MapLocation closestEnemyLocation = enemyUnits[0].getLocation();
                    int minDist = rc.getLocation().distanceSquaredTo(closestEnemyLocation);
                    for (RobotInfo enemyUnit : enemyUnits) {
                        int dist = rc.getLocation().distanceSquaredTo(enemyUnit.getLocation());
                        if (dist < minDist) {
                            minDist = dist;
                            closestEnemyLocation = enemyUnit.getLocation();
                        }
                    }
                    if (closestEnemyLocation != null) {
                        int mopperCount = 0;
                        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(closestEnemyLocation, 8, myTeam);
                        for (RobotInfo nearbyAlly : nearbyAllies) {
                            if (nearbyAlly.getType() == UnitType.MOPPER) mopperCount++;
                        }

                        if ((mopperCount < 1 || rc.getHealth() <= 200) && rc.getChips() >= UnitType.MOPPER.moneyCost && rc.getPaint() >= UnitType.MOPPER.paintCost) {
                            if (rc.getLocation().distanceSquaredTo(closestEnemyLocation) <= 9 || spawnedDefenseMoppers < 1 || rc.getHealth() <= 200) {
                                Direction dir = rc.getLocation().directionTo(closestEnemyLocation);
                                if (tryBuildRobot(UnitType.MOPPER, dir)) {
                                    spawnedDefenseMoppers++;
                                    return;
                                }
                            }
                        }
                    }
                }

                // early game
                if (rc.getRoundNum() < 100) {
                    if (rc.getChips() >= UnitType.SOLDIER.moneyCost + newTowerChipThreshold && rc.getPaint() >= UnitType.SOLDIER.paintCost) {
                        Direction dir = rc.getLocation().directionTo(exploreLocations[4]);
                        if (tryBuildRobot(UnitType.SOLDIER, dir)) {
                            return;
                        }
                    }
                } else {
                    UnitType type = getNextToSpawn();
                    if (rc.getChips() >= type.moneyCost + newTowerChipThreshold && rc.getPaint() >= type.paintCost) {
                        Direction dir = rc.getLocation().directionTo(exploreLocations[4]);
                        if (tryBuildRobot(type, dir)) {
                            return;
                        }
                    }
                }
            }
        }
    }

    public UnitType getNextToSpawn() {
        if (rc.getType().getBaseType() == UnitType.LEVEL_ONE_PAINT_TOWER) {
            if (spawnedSoldiers - spawnedMoppers >= 2) {
                return UnitType.MOPPER;
            }
            if (spawnedMoppers - spawnedSoldiers >= 2) {
                return UnitType.SOLDIER;
            }

            if (rc.getChips() >= 3000) {
                return UnitType.MOPPER;
            } else {
                return UnitType.SOLDIER;
            }
        } else {
            if (rc.getPaint() < 200) {
                return UnitType.MOPPER;
            } else {
                return UnitType.SOLDIER;
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
    }

    public boolean tryBuildRobot(UnitType unitType, Direction initialDir) throws GameActionException {
        Direction dir = initialDir;
        for (int i = 0; i < 8; i++) {
            MapLocation newLoc = rc.getLocation().add(dir).add(dir);
            if (buildRobot(unitType, newLoc)) {
                return true;
            }
            MapLocation newLoc2 = rc.getLocation().add(dir);
            if (buildRobot(unitType, newLoc2)) {
                return true;
            }
            dir = dir.rotateRight();
        }
        return false;
    }

    public boolean buildRobot(UnitType unitType, MapLocation loc) throws GameActionException {
        if (rc.canBuildRobot(unitType, loc)) {
            rc.buildRobot(unitType, loc);
            switch (unitType) {
                case SOLDIER:
                    spawnedSoldiers += 1;
                    break;
                case MOPPER:
                    spawnedMoppers += 1;
                    break;
                case SPLASHER:
                    spawnedSplashers += 1;
                    break;
            }
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
