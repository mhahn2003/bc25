package newwander;

import battlecode.common.*;

public class Tower extends Unit {
    static boolean init = false;
    static boolean startingTower = false;
    static int newTowerChipThreshold = 1000;
    static int roundsSinceLastAttack = 0;
    static boolean hasBeenAttacked = false;

    static int spawnedSoldiers = 0;
    static int spawnedMoppers = 0;
    static int spawnedSplashers = 0;
    static int spawnedDefenseMoppers = 0;

    static int midGameRoundStart = 100;
    static int endGameRoundStart = 300;

    public void act() throws GameActionException {
        if (!init) {
            init = true;
            if (rc.getRoundNum() == 1) {
                startingTower = true;
            }
            friendlyTowerLocations.add(rc.getLocation());
        }
        super.act();
        resetCounter();
        attackEnemyUnits();
        spawn();
        requestUpgrade();
        sendComms();
    }

    public void resetCounter() {
        if (rc.getRoundNum() == midGameRoundStart || rc.getRoundNum() == endGameRoundStart) {
            spawnedSoldiers = 0;
            spawnedMoppers = 0;
            spawnedSplashers = 0;
            spawnedDefenseMoppers = 0;
        }
    }

    public void attackEnemyUnits() throws GameActionException {
        RobotInfo[] totalEnemyUnits = rc.senseNearbyRobots(-1, opponentTeam);
        RobotInfo[] enemyUnits = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, opponentTeam);
        if (totalEnemyUnits.length == 0) {
            roundsSinceLastAttack += 1;
            return;
        }
        roundsSinceLastAttack = 0;
        hasBeenAttacked = true;
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
            Direction dir = loc.directionTo(centerLocation);
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
                        if (enemyUnit.getPaintAmount() >= 20 && enemyUnit.getType() != UnitType.MOPPER) {
                            int dist = rc.getLocation().distanceSquaredTo(enemyUnit.getLocation());
                            if (dist < minDist) {
                                minDist = dist;
                                closestEnemyLocation = enemyUnit.getLocation();
                            }
                        }
                    }
                    if (closestEnemyLocation != null) {
                        int mopperCount = 0;
                        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(closestEnemyLocation, 8, myTeam);
                        for (RobotInfo nearbyAlly : nearbyAllies) {
                            if (nearbyAlly.getType() == UnitType.MOPPER) mopperCount++;
                        }

                        if (rc.getChips() >= UnitType.MOPPER.moneyCost && rc.getPaint() >= UnitType.MOPPER.paintCost) {
                            if (mopperCount < 1 && rc.getLocation().distanceSquaredTo(closestEnemyLocation) <= 9 && spawnedDefenseMoppers < 1) {
                                Direction dir = rc.getLocation().directionTo(closestEnemyLocation);
                                if (tryBuildRobot(UnitType.MOPPER, dir)) {
                                    spawnedDefenseMoppers++;
                                    return;
                                }
                            }
                        }
                    }
                }

                UnitType type = getNextToSpawn();
                if (rc.getChips() >= type.moneyCost + newTowerChipThreshold && rc.getPaint() >= type.paintCost) {
                    Direction dir = rc.getLocation().directionTo(centerLocation);
                    tryBuildRobot(type, dir);
                }
            }
        }
    }

    public UnitType getNextToSpawn() {
        if (rc.getType().getBaseType() == UnitType.LEVEL_ONE_PAINT_TOWER) {
            if (rc.getRoundNum() < midGameRoundStart) {
                // early game
                // purely soldier
                return UnitType.SOLDIER;
            } else if (rc.getRoundNum() < endGameRoundStart) {
                // mid game
                // 6:1:3 ratio of soldiers to splashers to moppers
                double divSoldierCount = (double) spawnedSoldiers / 6.0;
                double divMopperCount = (double) spawnedMoppers / 3.0;
                double divSplasherCount = (double) spawnedSplashers / 1.0;
                if (divSplasherCount <= divSoldierCount && divSplasherCount <= divMopperCount && rc.getNumberTowers() > 6) {
                    return UnitType.SPLASHER;
                } else {
                    if (divMopperCount <= divSoldierCount - 0.5) {
                        return UnitType.MOPPER;
                    }
                    if (divSoldierCount <= divMopperCount - 0.5) {
                        return UnitType.SOLDIER;
                    }

                    if (rc.getChips() >= 2500) {
                        return UnitType.MOPPER;
                    } else {
                        return UnitType.SOLDIER;
                    }
                }
            } else {
                // end game
                // 1:2:1 ratio of soldiers to splashers to moppers
                double divSoldierCount = (double) spawnedSoldiers / 1.0;
                double divMopperCount = (double) spawnedMoppers / 1.0;
                double divSplasherCount = (double) spawnedSplashers / 2.0;
                if (divSplasherCount <= divSoldierCount && divSplasherCount <= divMopperCount) {
                    return UnitType.SPLASHER;
                } else {
                    if (divMopperCount <= divSoldierCount - 2) {
                        return UnitType.MOPPER;
                    }
                    if (divSoldierCount <= divMopperCount - 2) {
                        return UnitType.SOLDIER;
                    }

                    if (rc.getChips() >= 2500) {
                        return UnitType.MOPPER;
                    } else {
                        return UnitType.SOLDIER;
                    }
                }
            }
        } else {
            if (rc.getRoundNum() < midGameRoundStart) {
                return UnitType.SOLDIER;
            } else if (rc.getPaint() < 200) {
                return UnitType.MOPPER;
            } else {
                return UnitType.SOLDIER;
            }
        }
    }

    public void requestUpgrade() throws GameActionException {
        if (((rc.getType().getBaseType() != UnitType.LEVEL_ONE_DEFENSE_TOWER && roundsSinceLastAttack >= 40)
                || (rc.getType().getBaseType() == UnitType.LEVEL_ONE_DEFENSE_TOWER
                && hasBeenAttacked && roundsSinceLastAttack < 3)) && rc.getChips() >= Util.getUpgradeCost(rc.getType())) {
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
        MapLocation[] friendlyTowerLocations = Globals.friendlyTowerLocations.getLocations();
        for (MapLocation friendlyTowerLoc : friendlyTowerLocations) {
            Comms.addToMessageQueue(Comms.InfoCategory.FRIEND_TOWER, friendlyTowerLoc, true);
        }
        MapLocation[] enemyTowerLocations = Globals.enemyTowerLocations.getLocations();
        for (MapLocation enemyTowerLoc : enemyTowerLocations) {
            Comms.addToMessageQueue(Comms.InfoCategory.ENEMY_TOWER, enemyTowerLoc, true);
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
