package finals;

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
    static int endGameRoundStart = 250;

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
        flicker();
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

    public void flicker() throws GameActionException {
        if (rc.getChips() < 2000 || rc.getPaint() >= 100) return;
        if (rc.getType() == UnitType.LEVEL_ONE_MONEY_TOWER && rc.getChips() >= 3000) {
            if (Util.isDefenseTowerLocation(rc.getLocation())) return;
            MapInfo[] paintInfo = rc.senseNearbyMapInfos(8);
            int numIncorrectPaint = 0;
            for (MapInfo info : paintInfo) {
                if (info.getMapLocation().equals(rc.getLocation())) continue;
                if (info.getPaint().isEnemy()) return;
                if (info.getPaint() == PaintType.EMPTY) {
                    numIncorrectPaint++;
                } else if (info.getPaint().isSecondary() != Util.useSecondaryForTower(info.getMapLocation(), rc.getLocation(), UnitType.LEVEL_ONE_MONEY_TOWER)) {
                    numIncorrectPaint++;
                }
            }
            if (numIncorrectPaint < 3 || rc.getChips() > 5000) {
                RobotInfo[] nearbyAllies = rc.senseNearbyRobots(-1, myTeam);
                int minDist = Integer.MAX_VALUE;
                Logger.log("need flicker");
                RobotInfo closestAlly = null;
                for (RobotInfo ally : nearbyAllies) {
                    if (ally.getType() == UnitType.SOLDIER && rc.canSendMessage(ally.getLocation()) && ally.getPaintAmount() > numIncorrectPaint * 5) {
                        int dist = rc.getLocation().distanceSquaredTo(ally.getLocation());
                        if (dist < minDist) {
                            minDist = dist;
                            closestAlly = ally;
                        }
                    }
                }
                if (closestAlly != null) {
                    RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(-1, opponentTeam);
                    for (RobotInfo enemy : nearbyEnemies) {
                        if (enemy.getType() == UnitType.MOPPER || enemy.getType() == UnitType.SPLASHER) {
                            return;
                        }
                    }

                    int message = Comms.encodeMessage(Comms.InfoCategory.FLICKER, rc.getLocation());
                    message = Comms.combineMessage(message, 0);
                    if (rc.canSendMessage(closestAlly.getLocation(), message)) {
                        Logger.log("sending flicker: " + closestAlly.getLocation());
                        rc.sendMessage(closestAlly.getLocation(), message);
                    }
                    if (closestAlly.getLocation().distanceSquaredTo(rc.getLocation()) <= 2 && numIncorrectPaint == 0) {
                        rc.disintegrate();
                    }
                }
            }
        } else if (rc.getType().getBaseType() == UnitType.LEVEL_ONE_DEFENSE_TOWER) {
            switch (rc.getType()) {
                case LEVEL_ONE_DEFENSE_TOWER -> {
                    if (roundsSinceLastAttack < 100) return;
                }
                case LEVEL_TWO_DEFENSE_TOWER -> {
                    if (roundsSinceLastAttack < 150) return;
                }
                case LEVEL_THREE_DEFENSE_TOWER -> {
                    if (roundsSinceLastAttack < 200) return;
                }
            }

            MapInfo[] paintInfo = rc.senseNearbyMapInfos(8);
            int numIncorrectPaint = 0;
            for (MapInfo info : paintInfo) {
                if (info.getMapLocation().equals(rc.getLocation())) continue;
                if (info.getPaint().isEnemy()) return;
                if (info.getPaint() == PaintType.EMPTY) {
                    numIncorrectPaint++;
                } else if (info.getPaint().isSecondary() != Util.useSecondaryForTower(info.getMapLocation(), rc.getLocation(), UnitType.LEVEL_ONE_MONEY_TOWER)) {
                    numIncorrectPaint++;
                }
            }

            RobotInfo[] nearbyAllies = rc.senseNearbyRobots(-1, myTeam);
            int minDist = Integer.MAX_VALUE;
            Logger.log("need flicker");
            RobotInfo closestAlly = null;
            for (RobotInfo ally : nearbyAllies) {
                if (ally.getType() == UnitType.SOLDIER && rc.canSendMessage(ally.getLocation()) && ally.getPaintAmount() > numIncorrectPaint * 5) {
                    int dist = rc.getLocation().distanceSquaredTo(ally.getLocation());
                    if (dist < minDist) {
                        minDist = dist;
                        closestAlly = ally;
                    }
                }
            }
            if (closestAlly != null) {
                int message = Comms.encodeMessage(Comms.InfoCategory.FLICKER, rc.getLocation());
                message = Comms.combineMessage(message, 0);
                if (rc.canSendMessage(closestAlly.getLocation(), message)) {
                    Logger.log("sending flicker: " + closestAlly.getLocation());
                    rc.sendMessage(closestAlly.getLocation(), message);
                }
                if (closestAlly.getLocation().distanceSquaredTo(rc.getLocation()) <= 2 && numIncorrectPaint == 0) {
                    rc.disintegrate();
                }
            }
        }
    }

    public void spawn() throws GameActionException {
        if (startingTower && rc.getRoundNum() <= 3) {
            if (tryBuildRobot(UnitType.SOLDIER, exploreLocations[4])) {
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
                            if (mopperCount < 2 && rc.getLocation().distanceSquaredTo(closestEnemyLocation) <= 9 && spawnedDefenseMoppers < 1) {
                                if (tryBuildRobot(UnitType.MOPPER, closestEnemyLocation)) {
                                    spawnedDefenseMoppers++;
                                    return;
                                }
                            }
                        }

                        if (rc.getHealth() <= 100) {
                            if (rc.getPaint() >= 300 && rc.getChips() >= 400) {
                                if (tryBuildRobot(UnitType.SPLASHER, closestEnemyLocation)) {
                                    spawnedSplashers++;
                                    return;
                                }
                            } else if (rc.getPaint() >= 200 & rc.getChips() >= 250) {
                                if (tryBuildRobot(UnitType.SOLDIER, closestEnemyLocation)) {
                                    spawnedSoldiers++;
                                    return;
                                }
                            }
                        }
                    }
                }

                UnitType type = getNextToSpawn();
                if (rc.getChips() >= type.moneyCost + newTowerChipThreshold && rc.getPaint() >= type.paintCost) {
                    tryBuildRobot(type, exploreLocations[4]);
                }
            }
        }
    }

    public UnitType getNextToSpawn() {
        if (rc.getType().getBaseType() == UnitType.LEVEL_ONE_PAINT_TOWER) {
            if (rc.getRoundNum() < midGameRoundStart) {
                // early game
                // purely soldier
                if (Math.min(mapHeight, mapWidth) <= 30 && spawnedSplashers == 0) return UnitType.SPLASHER;
                else return UnitType.SOLDIER;
            } else if (rc.getRoundNum() < endGameRoundStart) {
                // mid game
                // 3:1:2 ratio of soldiers to splashers to moppers
                double divSoldierCount = (double) spawnedSoldiers / 3.0;
                double divSplasherCount = (double) spawnedSplashers / 1.0;
                double divMopperCount = (double) spawnedMoppers / 2.0;
                if (divSplasherCount <= divSoldierCount && divSplasherCount <= divMopperCount && rc.getNumberTowers() > 6) {
                    return UnitType.SPLASHER;
                } else {
                    if (divMopperCount <= divSoldierCount - 0.5) {
                        return UnitType.MOPPER;
                    }
                    if (divSoldierCount <= divMopperCount - 0.5) {
                        return UnitType.SOLDIER;
                    }

                    if (rc.getChips() >= 2000) {
                        return UnitType.MOPPER;
                    } else {
                        return UnitType.SOLDIER;
                    }
                }
            } else {
                // end game
                // 1:4:2 ratio of soldiers to splashers to moppers
                double divSoldierCount = (double) spawnedSoldiers / 1.0;
                double divSplasherCount = (double) spawnedSplashers / 4.0;
                double divMopperCount = (double) spawnedMoppers / 2.0;
                if (divSplasherCount <= divSoldierCount && divSplasherCount <= divMopperCount) {
                    return UnitType.SPLASHER;
                } else {
                    if (divMopperCount <= divSoldierCount - 2) {
                        return UnitType.MOPPER;
                    }
                    if (divSoldierCount <= divMopperCount - 2) {
                        return UnitType.SOLDIER;
                    }

                    if (rc.getChips() >= 2000) {
                        return UnitType.MOPPER;
                    } else {
                        return UnitType.SOLDIER;
                    }
                }
            }
        } else {
            if (rc.getRoundNum() < midGameRoundStart) {
                if (Math.min(mapHeight, mapWidth) <= 30 && spawnedSplashers == 0 && rc.getPaint() >= 300) return UnitType.SPLASHER;
                return UnitType.SOLDIER;
            } else if (rc.getRoundNum() < endGameRoundStart) {
                if (rc.getPaint() < 200) {
                    return UnitType.MOPPER;
                } else {
                    if (rc.getChips() > 1500 && rc.getPaint() >= 300) {
                        return UnitType.SPLASHER;
                    } else {
                        return UnitType.SOLDIER;
                    }
                }
            } else if (rc.getPaint() < 200) {
                return UnitType.MOPPER;
            } else if (rc.getPaint() < 300) {
                return UnitType.SOLDIER;
            } else if (rc.getChips() > 1500 && rc.getRoundNum() % 10 != 0) {
                return UnitType.SPLASHER;
            } else {
                return UnitType.SOLDIER;
            }
        }
    }

    public void requestUpgrade() throws GameActionException {
        if (((rc.getType().getBaseType() != UnitType.LEVEL_ONE_DEFENSE_TOWER && roundsSinceLastAttack >= 20)
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

    public boolean tryBuildRobot(UnitType unitType, MapLocation towards) throws GameActionException {
        if (rc.isActionReady()) {
            MapLocation[] spawnLocs = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4);
            MapLocation bestSpawnLoc = null;
            int minDist = Integer.MAX_VALUE;
            for (MapLocation spawnLoc : spawnLocs) {
                if (rc.canBuildRobot(unitType, spawnLoc)) {
                    int dist = towards.distanceSquaredTo(spawnLoc);
                    if (dist < minDist) {
                        minDist = dist;
                        bestSpawnLoc = spawnLoc;
                    }
                }
            }
            if (bestSpawnLoc != null) {
                buildRobot(unitType, bestSpawnLoc);
                return true;
            }
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
