package dumbsrp;

import battlecode.common.*;

import java.util.function.Function;

public class Mopper extends Unit {

    private static boolean[][] computeEnemyLocs;

    public enum MopperState {
        DEFAULT,
        EVADE,
        REFILL,
        ATTACK,
        BUILD_TOWER,
        BUILD_SRP,
        DEFEND,
    }

    static MopperState state = MopperState.DEFAULT;

    public void act() throws GameActionException {
        super.act();
//        System.out.println("init: " + Clock.getBytecodeNum());
        if (rc.getPaint() == 0) rc.disintegrate();
        upgrade();
//        System.out.println("upgrade: " + Clock.getBytecodeNum());
        drain();
//        System.out.println("drain: " + Clock.getBytecodeNum());
        evade();
//        System.out.println("evade: " + Clock.getBytecodeNum());
        refill();
//        System.out.println("refill: " + Clock.getBytecodeNum());
        defend();
//        System.out.println("defend: " + Clock.getBytecodeNum());
        buildTower();
//        System.out.println("buildTower: " + Clock.getBytecodeNum());
        attack();
//        System.out.println("attack: " + Clock.getBytecodeNum());
        buildSRP();
//        System.out.println("buildSRP: " + Clock.getBytecodeNum());
        move();
//        System.out.println("move: " + Clock.getBytecodeNum());
        mopLeftover();
//        System.out.println("mopLeftover: " + Clock.getBytecodeNum());

        if (rc.isActionReady()) noActionCounter++;
        else noActionCounter = 0;
    }

    public void upgrade() throws GameActionException {
        if (state != MopperState.DEFAULT || upgradeTowerLocation == null) return;

        RobotInfo tower;
        if (rc.canSenseRobotAtLocation(upgradeTowerLocation)) {
            tower = rc.senseRobotAtLocation(upgradeTowerLocation);
            if (tower == null || tower.getTeam() == opponentTeam || tower.getType() == UnitType.LEVEL_THREE_PAINT_TOWER || tower.getType() == UnitType.LEVEL_THREE_MONEY_TOWER || tower.getType() == UnitType.LEVEL_THREE_DEFENSE_TOWER) {
                return;
            }
        } else {
            return;
        }

        if (rc.getChips() < Util.getUpgradeCost(tower.getType())) {
            return;
        }

        if (rc.isActionReady() && rc.canUpgradeTower(upgradeTowerLocation)) {
            rc.upgradeTower(upgradeTowerLocation);
        } else {
            Navigator.moveTo(upgradeTowerLocation);
        }
    }

    public void drain() throws GameActionException {
        MapLocation[] nearbyRuins = rc.senseNearbyRuins(2);
        for (MapLocation ruin : nearbyRuins) {
            RobotInfo robot = rc.senseRobotAtLocation(ruin);
            if (robot != null && robot.getTeam() == myTeam) {
                if (robot.getPaintAmount() >= 10 && rc.getPaint() <= 90) {
                    int transferAmount = Math.min(robot.getPaintAmount(), UnitType.MOPPER.paintCapacity - rc.getPaint());
                    if (rc.canTransferPaint(ruin, -transferAmount)) {
                        rc.transferPaint(ruin, -transferAmount);
                    }
                }
            }
        }

        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(2, myTeam);
        for (RobotInfo ally : nearbyAllies) {
            if (ally.getType() == UnitType.SPLASHER && ally.getPaintAmount() < 50 && rc.getPaint() <= 90) {
                int transferAmount = Math.min(ally.getPaintAmount(), UnitType.MOPPER.paintCapacity - rc.getPaint());
                if (rc.canTransferPaint(ally.getLocation(), -transferAmount)) {
                    rc.transferPaint(ally.getLocation(), -transferAmount);
                }
            }
        }
    }

    public void evade() throws GameActionException {
        MapLocation[] nearbyRuins = rc.senseNearbyRuins(-1);
        MapLocation closestEnemyTower = null;
        int minDist = Integer.MAX_VALUE;
        for (MapLocation ruin : nearbyRuins) {
            RobotInfo robot = rc.senseRobotAtLocation(ruin);
            if (robot != null && robot.getType().isTowerType() && robot.getTeam() == opponentTeam) {
                int dist = rc.getLocation().distanceSquaredTo(ruin);
                if (dist < minDist) {
                    minDist = dist;
                    closestEnemyTower = ruin;
                }
            }
        }
        if (closestEnemyTower == null) {
            if (state == MopperState.EVADE) {
                state = MopperState.DEFAULT;
            }
            return;
        }
        state = MopperState.EVADE;

        Logger.log("evade: " + closestEnemyTower);
        final MapLocation finalEnemyTower = closestEnemyTower;
        if (flipLocation != null) {
            Logger.log("towards: " + flipLocation);
            computeBestAction(rc.getLocation(), newLoc -> {
                int distanceToTower = newLoc.distanceSquaredTo(finalEnemyTower);
                return (distanceToTower > 9 ? 0 : 50 * (11 - distanceToTower)) + newLoc.distanceSquaredTo(flipLocation);
            });
        } else {
            Movement.wanderDirection();
            Logger.log("towards: " + wanderLocation);
            computeBestAction(rc.getLocation(), newLoc -> {
                int distanceToTower = newLoc.distanceSquaredTo(finalEnemyTower);
                return (distanceToTower > 9 ? 0 : 50 * (11 - distanceToTower)) + newLoc.distanceSquaredTo(wanderLocation);
            });
        }
    }

    public void refill() throws GameActionException {
        if (state != MopperState.REFILL && ((state == MopperState.DEFAULT && rc.getPaint() < 50) || rc.getPaint() <= 25) && rc.getChips() < 2000) {
            Logger.log("need refill");
            MapLocation closestFriendPaintTower = null;
            int minDist = Integer.MAX_VALUE;
            MapLocation[] friendlyPaintTowerLocations = Globals.friendlyTowerLocations.getLocations();
            for (MapLocation loc : friendlyPaintTowerLocations) {
                int dist = rc.getLocation().distanceSquaredTo(loc);
                if (dist < minDist && !noRefillTowerLocations.contains(loc)) {
                    minDist = dist;
                    closestFriendPaintTower = loc;
                }
            }
            if (closestFriendPaintTower != null && rc.getLocation().distanceSquaredTo(closestFriendPaintTower) <= refillTowerDistanceThreshold) {
                Logger.log("paint tower: " + closestFriendPaintTower);
                state = MopperState.REFILL;
                refillPaintTowerLocation = closestFriendPaintTower;
            } else {
                MapLocation closestRefillTower = null;
                minDist = Integer.MAX_VALUE;
                MapLocation[] nearbyRuins = rc.senseNearbyRuins(-1);
                for (MapLocation ruin : nearbyRuins) {
                    RobotInfo robot = rc.senseRobotAtLocation(ruin);
                    if (robot != null && robot.getTeam() == myTeam && robot.getPaintAmount() >= 15) {
                        int dist = rc.getLocation().distanceSquaredTo(ruin);
                        if (dist < minDist) {
                            minDist = dist;
                            closestRefillTower = ruin;
                        }
                    }
                }

                if (closestRefillTower != null) {
                    Logger.log("paint tower: " + closestRefillTower);
                    state = MopperState.REFILL;
                    refillPaintTowerLocation = closestRefillTower;
                } else {
                    state = MopperState.DEFAULT;
                    return;
                }
            }
        }
        if (state == MopperState.REFILL) {
            if (rc.getPaint() >= 50) {
                state = MopperState.DEFAULT;
                return;
            }
            RobotInfo tower = null;
            if (rc.canSenseRobotAtLocation(refillPaintTowerLocation)) {
                tower = rc.senseRobotAtLocation(refillPaintTowerLocation);
                if (tower == null || tower.getTeam() == opponentTeam || (tower.getType().getBaseType() != UnitType.LEVEL_ONE_PAINT_TOWER && tower.getPaintAmount() < 15)) {
                    noRefillTowerLocations.add(refillPaintTowerLocation);
                    state = MopperState.DEFAULT;
                    return;
                }

                if (rc.getLocation().distanceSquaredTo(refillPaintTowerLocation) > 4) {
                    RobotInfo[] nearbyRobots = rc.senseNearbyRobots(refillPaintTowerLocation, 4, myTeam);
                    if (nearbyRobots.length >= 8) {
                        noRefillTowerLocations.add(refillPaintTowerLocation);
                        state = MopperState.DEFAULT;
                        return;
                    }
                }
            } else if (rc.getLocation().distanceSquaredTo(refillPaintTowerLocation) <= GameConstants.VISION_RADIUS_SQUARED) {
                state = MopperState.DEFAULT;
                return;
            }

            MapLocation[] nearbyRuins = rc.senseNearbyRuins(-1);
            for (MapLocation ruin : nearbyRuins) {
                RobotInfo robot = rc.senseRobotAtLocation(ruin);
                if (robot != null && robot.getTeam() == myTeam && rc.getLocation().distanceSquaredTo(ruin) < rc.getLocation().distanceSquaredTo(refillPaintTowerLocation) && robot.getPaintAmount() >= 15) {
                    refillPaintTowerLocation = ruin;
                    tower = robot;
                    break;
                }
            }

            if (rc.getLocation().distanceSquaredTo(refillPaintTowerLocation) <= 2) {
                int transferAmount = Math.min(UnitType.MOPPER.paintCapacity - rc.getPaint(), tower.getPaintAmount());
                if (rc.canTransferPaint(refillPaintTowerLocation, -transferAmount)) {
                    rc.transferPaint(refillPaintTowerLocation, -transferAmount);
                    MapLocation opposite = new MapLocation(2 * refillPaintTowerLocation.x - rc.getLocation().x, 2 * refillPaintTowerLocation.y - rc.getLocation().y);
                    Navigator.moveTo(opposite);
                }
            } else if (rc.getLocation().distanceSquaredTo(refillPaintTowerLocation) <= 8) {
                if (rc.isActionReady()) {
                    Navigator.moveTo(refillPaintTowerLocation);
                    if (rc.getLocation().distanceSquaredTo(refillPaintTowerLocation) <= 2) {
                        int transferAmount = Math.min(UnitType.MOPPER.paintCapacity - rc.getPaint(), tower.getPaintAmount());
                        if (rc.canTransferPaint(refillPaintTowerLocation, -transferAmount)) {
                            rc.transferPaint(refillPaintTowerLocation, -transferAmount);
                        }
                    }
                }
            } else {
                if (rc.canSenseLocation(refillPaintTowerLocation)) {
                    computeBestAction(rc.getLocation(), newLoc -> newLoc.distanceSquaredTo(refillPaintTowerLocation));
                } else {
                    Navigator.moveTo(refillPaintTowerLocation);
                }
            }
        }
    }

    public void defend() throws GameActionException {
        MapLocation[] nearbyRuins = rc.senseNearbyRuins(-1);
        MapLocation closestFriendlyTower = null;
        int minDist = Integer.MAX_VALUE;
        for (MapLocation ruin : nearbyRuins) {
            RobotInfo robot = rc.senseRobotAtLocation(ruin);
            if (robot != null && robot.getTeam() == myTeam && robot.getType().isTowerType()) {
                int dist = rc.getLocation().distanceSquaredTo(ruin);
                if (dist < minDist) {
                    minDist = dist;
                    closestFriendlyTower = ruin;
                }
            }
        }
        if (closestFriendlyTower == null) {
            if (state == MopperState.DEFEND) {
                state = MopperState.DEFAULT;
            }
            return;
        }
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(closestFriendlyTower, -1, opponentTeam);
        RobotInfo closestEnemy = null;
        minDist = Integer.MAX_VALUE;
        for (RobotInfo enemy : enemyRobots) {
            if (enemy.getType().isRobotType() && enemy.getPaintAmount() > 0) {
                int dist = rc.getLocation().distanceSquaredTo(enemy.getLocation());
                if (dist < minDist) {
                    minDist = dist;
                    closestEnemy = enemy;
                }
            }
        }

        if (closestEnemy == null) {
            if (state == MopperState.DEFEND) {
                state = MopperState.DEFAULT;
            }
            return;
        }
        state = MopperState.DEFEND;
        final MapLocation finalClosestEnemy = closestEnemy.getLocation();
        computeBestAction(rc.getLocation(), newLoc -> newLoc.distanceSquaredTo(finalClosestEnemy));
    }

    public void buildTower() throws GameActionException {
        if (state == MopperState.EVADE || state == MopperState.DEFEND || state == MopperState.REFILL) return;

        if (state != MopperState.BUILD_TOWER) {
            MapLocation[] ruins = rc.senseNearbyRuins(-1);

            if (ruins.length == 0) {
                return;
            }

            MapLocation closestRuin = null;
            int minDist = Integer.MAX_VALUE;
            for (MapLocation ruin : ruins) {
                if (impossibleRuinLocations.contains(ruin)) {
                    continue;
                }
                RobotInfo robot = rc.senseRobotAtLocation(ruin);
                if (robot == null) {
                    int dist = rc.getLocation().distanceSquaredTo(ruin);
                    if (dist <= minDist) {
                        minDist = dist;
                        closestRuin = ruin;
                    }
                }
            }

            if (closestRuin == null) return;

            state = MopperState.BUILD_TOWER;
            buildRuinLocation = closestRuin;
            Logger.log("reset no mop");
            noMopCounter = 0;
        }

        if (state == MopperState.BUILD_TOWER) {
            if (rc.getNumberTowers() == 25) {
                state = MopperState.DEFAULT;
                return;
            }

            if (rc.canSenseRobotAtLocation(buildRuinLocation)) {
                RobotInfo robot = rc.senseRobotAtLocation(buildRuinLocation);
                if (robot != null) {
                    impossibleRuinLocations.add(buildRuinLocation);
                    state = MopperState.DEFAULT;
                    return;
                }
            }

            Logger.log("buildTower: " + buildRuinLocation);

            MapInfo[] nearbyLocations = rc.senseNearbyMapInfos(buildRuinLocation, 8);

            MapLocation bestEnemyPaintLocation = null;
            int minDist = Integer.MAX_VALUE;
            for (MapInfo mapInfo : nearbyLocations) {
                if (mapInfo.getPaint().isEnemy()) {
                    int dist = rc.getLocation().distanceSquaredTo(mapInfo.getMapLocation());
                    if (dist < minDist) {
                        minDist = dist;
                        bestEnemyPaintLocation = mapInfo.getMapLocation();
                    }
                }
            }

            if (bestEnemyPaintLocation != null) {
                Logger.log("paint loc: " + bestEnemyPaintLocation);
                Logger.log("no mop counter: " + noMopCounter);
                if (rc.isActionReady()) {
                    if (rc.isMovementReady()) {
                        Direction bestDir = null;
                        int minHeuristic = Integer.MAX_VALUE;
                        for (Direction dir : Direction.allDirections()) {
                            MapLocation newLoc = rc.getLocation().add(dir);
                            if (rc.canMove(dir) && newLoc.distanceSquaredTo(bestEnemyPaintLocation) <= 2) {
                                int heuristic = 0;
                                PaintType paint = rc.senseMapInfo(newLoc).getPaint();
                                if (paint.isEnemy()) heuristic += 2;
                                else if (paint == PaintType.EMPTY) heuristic += 1;
                                if (heuristic < minHeuristic) {
                                    minHeuristic = heuristic;
                                    bestDir = dir;
                                }
                            }
                        }
                        if (bestDir != null) {
                            rc.move(bestDir);
                        }
                    }
                    if (rc.canAttack(bestEnemyPaintLocation)) {
                        rc.attack(bestEnemyPaintLocation);
                    }
                    if (rc.isMovementReady()) {
                        Navigator.moveTo(bestEnemyPaintLocation);
                    }
                } else if (rc.isMovementReady()) {
                    if (rc.getLocation().distanceSquaredTo(bestEnemyPaintLocation) > 8) {
                        Navigator.moveTo(bestEnemyPaintLocation);
                    } else if (rc.getActionCooldownTurns() < 20) {
                        Direction bestDir = null;
                        int minHeuristic = Integer.MAX_VALUE;
                        for (Direction dir : Direction.allDirections()) {
                            MapLocation newLoc = rc.getLocation().add(dir);
                            if (rc.canMove(dir) && newLoc.distanceSquaredTo(bestEnemyPaintLocation) <= 2) {
                                int heuristic = 0;
                                PaintType paint = rc.senseMapInfo(newLoc).getPaint();
                                if (paint.isEnemy()) heuristic += 2;
                                else if (paint == PaintType.EMPTY) heuristic += 1;
                                if (heuristic < minHeuristic) {
                                    minHeuristic = heuristic;
                                    bestDir = dir;
                                }
                            }
                        }
                        if (bestDir != null) {
                            rc.move(bestDir);
                        }
                    } else {
                        Direction bestDir = null;
                        int minHeuristic = Integer.MAX_VALUE;
                        for (Direction dir : Direction.allDirections()) {
                            MapLocation newLoc = rc.getLocation().add(dir);
                            if (rc.canMove(dir) && newLoc.distanceSquaredTo(bestEnemyPaintLocation) <= 8) {
                                int heuristic = 0;
                                PaintType paint = rc.senseMapInfo(newLoc).getPaint();
                                if (paint.isEnemy()) heuristic += 2;
                                else if (paint == PaintType.EMPTY) heuristic += 1;
                                if (heuristic < minHeuristic) {
                                    minHeuristic = heuristic;
                                    bestDir = dir;
                                }
                            }
                        }
                        if (bestDir != null) {
                            rc.move(bestDir);
                        }
                    }
                }
            } else {
                if (rc.getLocation().distanceSquaredTo(buildRuinLocation) <= 2 && rc.isActionReady()) {
                    UnitType towerType = Util.getTowerType(buildRuinLocation);
                    if (towerType != null && rc.canCompleteTowerPattern(towerType, buildRuinLocation)) {
                        rc.completeTowerPattern(towerType, buildRuinLocation);
                        state = MopperState.DEFAULT;
                        return;
                    }
                }
                if (rc.getLocation().distanceSquaredTo(buildRuinLocation) <= 4) {
                    impossibleRuinLocations.add(buildRuinLocation);
                    state = MopperState.DEFAULT;
                    return;
                }
                if (rc.getLocation().distanceSquaredTo(buildRuinLocation) <= 8) noMopCounter++;
                if (noMopCounter >= Globals.noMopTowerThreshold) {
                    impossibleRuinLocations.add(buildRuinLocation);
                    state = MopperState.DEFAULT;
                } else {
                    if (rc.getLocation().distanceSquaredTo(buildRuinLocation) <= 8) {
                        final MapLocation opposite = buildRuinLocation.add(rc.getLocation().directionTo(buildRuinLocation));
                        computeBestAction(rc.getLocation(), newLoc -> newLoc.distanceSquaredTo(opposite) / 3);
                    } else {
                        Navigator.moveTo(buildRuinLocation);
                    }
                }
            }
        }
    }

    public void attack() throws GameActionException {
        if (state == MopperState.REFILL || state == MopperState.EVADE || state == MopperState.BUILD_TOWER) return;
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, opponentTeam);
        RobotInfo closestEnemy = null;
        int minDist = Integer.MAX_VALUE;
        for (RobotInfo enemy : enemyRobots) {
            if (enemy.getType().isRobotType() && enemy.getPaintAmount() > 0) {
                int dist = rc.getLocation().distanceSquaredTo(enemy.getLocation());
                if (dist < minDist) {
                    minDist = dist;
                    closestEnemy = enemy;
                }
            }
        }

        if (closestEnemy == null) {
            if (state == MopperState.ATTACK) {
                state = MopperState.DEFAULT;
            }
            return;
        }
        state = MopperState.ATTACK;

        Logger.log("attack");

        final MapLocation finalClosestEnemy = closestEnemy.getLocation();
        computeBestAction(rc.getLocation(), newLoc -> newLoc.distanceSquaredTo(finalClosestEnemy));
    }

    public void buildSRP() throws GameActionException {
        if ((state != MopperState.BUILD_SRP && state != MopperState.DEFAULT) || rc.getRoundNum() < 100 || rc.getNumberTowers() < 6) return;

        MapLocation[] ruinLocs = ruinLocations.getLocations();
        FastSet rawRuins = new FastSet();
        for (MapLocation ruin : ruinLocs) {
            if (!friendlyTowerLocations.contains(ruin) && !enemyTowerLocations.contains(ruin)) {
                rawRuins.add(ruin);
            }
        }
        MapLocation[] rawRuinLocs = rawRuins.getLocations();
        if (rawRuinLocs.length != 0) {
            if (state == MopperState.BUILD_SRP) {
                for (MapLocation rawRuin : rawRuinLocs) {
                    if (Math.abs(rawRuin.x - buildSRPLocation.x) <= 5 && Math.abs(rawRuin.y - buildSRPLocation.y) <= 5) {
                        impossibleSRPLocations.add(buildSRPLocation);
                        state = MopperState.DEFAULT;
                        return;
                    }
                }
            }
        }

        if (state == MopperState.DEFAULT) {
            MapLocation closestSRPLocation = Util.getClosestSRPLocation(rawRuinLocs);
            if (closestSRPLocation != null) {
                state = MopperState.BUILD_SRP;
                buildSRPLocation = closestSRPLocation;
                noMopCounter = 0;
            } else {
                return;
            }
        }

        Logger.log("buildSRP: " + buildSRPLocation);

        if (rc.canSenseLocation(buildSRPLocation)) {
            if (rc.senseMapInfo(buildSRPLocation).isResourcePatternCenter()) {
                impossibleSRPLocations.add(buildSRPLocation);
                state = MopperState.DEFAULT;
                return;
            }
        }

        if (rc.getLocation().distanceSquaredTo(buildSRPLocation) <= 2) {
            if (!rc.canMarkResourcePattern(buildSRPLocation)) {
                impossibleSRPLocations.add(buildSRPLocation);
                state = MopperState.DEFAULT;
                return;
            }
        }

        MapInfo[] nearbyLocations = rc.senseNearbyMapInfos(buildSRPLocation, 8);

        MapLocation closestEnemyPaintLocation = null;
        int minDist2 = Integer.MAX_VALUE;
        for (MapInfo info : nearbyLocations) {
            if (info.getPaint().isEnemy()) {
                int dist = rc.getLocation().distanceSquaredTo(info.getMapLocation());
                if (dist < minDist2) {
                    minDist2 = dist;
                    closestEnemyPaintLocation = info.getMapLocation();
                }
            }
            if (info.isWall()) {
                impossibleSRPLocations.add(buildSRPLocation);
                state = MopperState.DEFAULT;
                return;
            }
        }

        if (closestEnemyPaintLocation != null) {
            final MapLocation paintLocation = closestEnemyPaintLocation;
            final MapLocation ruinLocation = buildSRPLocation;
            noMopCounter = 0;
            Logger.log("paint loc: " + paintLocation);
            computeBestAction(rc.getLocation(), newLoc -> newLoc.distanceSquaredTo(paintLocation) <= 5 ? 0 : newLoc.distanceSquaredTo(paintLocation) + newLoc.distanceSquaredTo(ruinLocation)/3);
        } else {
            if (rc.getLocation().distanceSquaredTo(buildSRPLocation) <= 4) {
                impossibleSRPLocations.add(buildSRPLocation);
                state = MopperState.DEFAULT;
                return;
            } else if (rc.getLocation().distanceSquaredTo(buildSRPLocation) <= 8) {
                noMopCounter++;
            }
            if (noMopCounter >= Globals.noMopSRPThreshold) {
                impossibleSRPLocations.add(buildSRPLocation);
                state = MopperState.DEFAULT;
            } else if (rc.canSenseLocation(buildSRPLocation)) {
                final MapLocation opposite = buildSRPLocation.add(rc.getLocation().directionTo(buildSRPLocation));
                computeBestAction(rc.getLocation(), newLoc -> newLoc.distanceSquaredTo(opposite)/3);
            } else {
                Navigator.moveTo(buildSRPLocation);
            }
        }
    }

    public void move() throws GameActionException {
        Logger.log("state: " + state);
        if (state != MopperState.DEFAULT) return;

        if (!(rc.getNumberTowers() < 6 && rc.getRoundNum() < 200)) {
            RobotInfo[] nearbyAllies = rc.senseNearbyRobots(8, myTeam);
            if (nearbyAllies.length >= 4) {
                MapInfo[] nearbyLocations = rc.senseNearbyMapInfos();
                MapLocation closestEnemyPaintLocation = null;
                int minDist = Integer.MAX_VALUE;
                for (MapInfo info : nearbyLocations) {
                    if (info.getPaint().isEnemy()) {
                        int dist = rc.getLocation().distanceSquaredTo(info.getMapLocation());
                        if (dist < minDist) {
                            minDist = dist;
                            closestEnemyPaintLocation = info.getMapLocation();
                        }
                    }
                }
                if (closestEnemyPaintLocation != null) {
                    Logger.log("paint: " + closestEnemyPaintLocation);
                    final MapLocation paintLocation = closestEnemyPaintLocation;
                    computeBestAction(rc.getLocation(), newLoc -> newLoc.distanceSquaredTo(paintLocation));
                    if (rc.isMovementReady() && rc.getLocation().distanceSquaredTo(paintLocation) > 5) {
                        Navigator.moveTo(paintLocation);
                    }
                }
            }
        }

        if (rc.isMovementReady()) {
            if (noActionCounter > noActionThreshold && flipLocation == null) {
                int totalDiagLength = mapWidth * mapWidth + mapHeight * mapHeight;
                flipLocation = exploreLocations[4];
                if (rc.getLocation().equals(flipLocation)) flipLocation = null;
                else {
                    while (flipLocation.distanceSquaredTo(exploreLocations[4]) < totalDiagLength / 16) {
                        MapLocation flipped = flipLocation.translate(exploreLocations[4].x - rc.getLocation().x, exploreLocations[4].y - rc.getLocation().y);
                        if (rc.onTheMap(flipped)) flipLocation = flipped;
                        else break;
                    }
                }
            }

            if (flipLocation != null) {
                Util.checkSymmetry();
                MapLocation base = Util.getBaseToVisit();
                if (base != null) {
                    Logger.log("base rush: " + base);
                    Navigator.moveTo(base);
                } else if (flipLocation != null) {
                    if (rc.getLocation().distanceSquaredTo(flipLocation) <= 9) {
                        flipLocation = null;
                    } else {
                        Logger.log("flip: " + flipLocation);
                        Navigator.moveTo(flipLocation);
                    }
                }
            }

            if (rc.isMovementReady()) {
                Movement.wanderDirection();
                Logger.log("wander: " + wanderLocation);
                computeBestAction(rc.getLocation(), newLoc -> newLoc.distanceSquaredTo(wanderLocation) / 3);
                if (rc.isMovementReady()) {
                    Navigator.moveTo(wanderLocation);
                }
            }
        }
    }

    public void mopLeftover() throws GameActionException {
        if (!rc.isActionReady() || (state != MopperState.DEFAULT && state != MopperState.REFILL)) return;
        if (rc.senseMapInfo(rc.getLocation()).getPaint().isEnemy()) {
            rc.attack(rc.getLocation());
        } else {
            for (Direction dir : Globals.adjacentDirections) {
                MapLocation newLoc = rc.getLocation().add(dir);
                if (rc.canSenseLocation(newLoc) && rc.senseMapInfo(newLoc).getPaint().isEnemy()) {
                    rc.attack(newLoc);
                    return;
                }
            }
        }
    }

    public static int sweepHeuristic(MapLocation loc, Direction dir) {
        int numEnemies = 0;
        MapLocation loc1 = loc.add(dir);
        MapLocation loc2 = loc.add(dir.rotateLeft());
        MapLocation loc3 = loc.add(dir.rotateRight());
        MapLocation loc4 = loc.add(dir).add(dir);
        MapLocation loc5 = loc.add(dir).add(dir.rotateLeft());
        MapLocation loc6 = loc.add(dir).add(dir.rotateRight());
        if (computeEnemyLocs[loc1.x-rc.getLocation().x+3][loc1.y-rc.getLocation().y+3]) numEnemies++;
        if (computeEnemyLocs[loc2.x-rc.getLocation().x+3][loc2.y-rc.getLocation().y+3]) numEnemies++;
        if (computeEnemyLocs[loc3.x-rc.getLocation().x+3][loc3.y-rc.getLocation().y+3]) numEnemies++;
        if (computeEnemyLocs[loc4.x-rc.getLocation().x+3][loc4.y-rc.getLocation().y+3]) numEnemies++;
        if (computeEnemyLocs[loc5.x-rc.getLocation().x+3][loc5.y-rc.getLocation().y+3]) numEnemies++;
        if (computeEnemyLocs[loc6.x-rc.getLocation().x+3][loc6.y-rc.getLocation().y+3]) numEnemies++;
        if (numEnemies == 0) return -9999;
        else return 3 * 5 * numEnemies;
    }

    public record LocationHeuristic(Integer value, MapLocation location) {}

    public LocationHeuristic succHeuristic(MapLocation loc) throws GameActionException {
        int maxPaintBenefit = 0;
        MapLocation bestLoc = null;
        if (rc.isActionReady()) {
            for (Direction dir : Globals.adjacentDirections) {
                MapLocation newLoc = loc.add(dir);
                if (computeEnemyLocs[newLoc.x-rc.getLocation().x+3][newLoc.y-rc.getLocation().y+3] && rc.senseMapInfo(newLoc).getPaint().isEnemy()) {
                    int paintBenefit = Math.max(10, rc.senseRobotAtLocation(newLoc).getPaintAmount()) + 5;
                    if (paintBenefit > maxPaintBenefit) {
                        maxPaintBenefit = paintBenefit;
                        bestLoc = newLoc;
                    }
                    if (maxPaintBenefit == 15) break;
                }
            }
        }

        if (maxPaintBenefit != 0) {
            return new LocationHeuristic(2 * maxPaintBenefit, bestLoc);
        } else {
            if (rc.isActionReady()) {
                if (rc.canSenseLocation(loc) && rc.senseMapInfo(loc).getPaint().isEnemy()) {
                    return new LocationHeuristic(2 * 5, loc);
                }
                for (Direction dir : Globals.adjacentDirections) {
                    MapLocation newLoc = loc.add(dir);
                    if (rc.canSenseLocation(newLoc) && rc.senseMapInfo(newLoc).getPaint().isEnemy()) {
                        return new LocationHeuristic(2 * 5, newLoc);
                    }
                }
            }
            return new LocationHeuristic(0, null);
        }
    }

    private void computeBestAction(MapLocation loc, Function<MapLocation, Integer> penaltyFunc) throws GameActionException {
        int startBytecodes = Clock.getBytecodeNum();
        Logger.log("computeBestAction: " + startBytecodes);

        Direction bestDir = null;
        Direction bestSweepDir = null;
        MapLocation succLocation = null;
        int bestHeuristic = Integer.MIN_VALUE;
        computeEnemyLocs = new boolean[7][7];
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(loc, 13, opponentTeam);
        for (RobotInfo enemy : enemyRobots) {
            if (enemy.getPaintAmount() > 0) computeEnemyLocs[enemy.location.x-rc.getLocation().x+3][enemy.location.y-rc.getLocation().y+3] = true;
        }

        if (!rc.isActionReady()) {
            if (rc.isMovementReady()) {
                for (Direction dir : Globals.adjacentDirections) {
                    if (rc.canMove(dir)) {
                        MapLocation newLoc = loc.add(dir);
                        int penalty = penaltyFunc.apply(newLoc) + Util.paintPenalty(newLoc, rc.senseMapInfo(newLoc).getPaint());
                        int heuristic = -penalty;
                        if (heuristic > bestHeuristic) {
                            bestHeuristic = heuristic;
                            bestDir = dir;
                        }
                    }
                }
            }
            Logger.log("movement no action: " + (Clock.getBytecodeNum() - startBytecodes));
        } else {
            if (rc.isMovementReady()) {
                // move then succ
                for (Direction dir : Globals.adjacentDirections) {
                    if (Clock.getBytecodesLeft() < 2000) break;
                    if (rc.canMove(dir)) {
                        MapLocation newLoc = loc.add(dir);
                        LocationHeuristic heuristicPair = succHeuristic(newLoc);
                        int penalty = penaltyFunc.apply(newLoc) + Util.paintPenalty(newLoc, rc.senseMapInfo(newLoc).getPaint());
                        int heuristic = heuristicPair.value() - penalty;
                        if (heuristic > bestHeuristic) {
                            bestHeuristic = heuristic;
                            bestDir = dir;
                            succLocation = heuristicPair.location();
                        }
                    }
                }
                Logger.log("movement succ: " + (Clock.getBytecodeNum() - startBytecodes));
            }
            // no move succ & sweep
            int penalty = penaltyFunc.apply(loc) + Util.paintPenalty(loc, rc.senseMapInfo(loc).getPaint());
            LocationHeuristic heuristicPair = succHeuristic(loc);
            int heuristic = heuristicPair.value() - penalty;
            if (heuristic > bestHeuristic) {
                bestHeuristic = heuristic;
                bestDir = Direction.CENTER;
                succLocation = heuristicPair.location();
            }
            for (Direction dir : Globals.cardinalDirections) {
                heuristic = sweepHeuristic(loc, dir) - penalty;
                if (heuristic > bestHeuristic) {
                    bestHeuristic = heuristic;
                    bestDir = Direction.CENTER;
                    bestSweepDir = dir;
                    succLocation = null;
                }
            }
            Logger.log("no movement succ & sweep: " + (Clock.getBytecodeNum() - startBytecodes));

            if (Clock.getBytecodesLeft() >= 2000 && rc.isMovementReady()) {
                for (Direction dir : adjacentDirections) {
                    if (Clock.getBytecodesLeft() < 2000) break;
                    if (rc.canMove(dir)) {
                        for (Direction sweepDir : Globals.cardinalDirections) {
                            heuristic = sweepHeuristic(loc.add(dir), sweepDir) - penalty;
                            if (heuristic > bestHeuristic) {
                                bestHeuristic = heuristic;
                                bestDir = dir;
                                bestSweepDir = sweepDir;
                            }
                        }
                    }
                }
            }
        }

        // Execute the best action
        if (bestDir != null) {
            if (bestDir != Direction.CENTER && rc.canMove(bestDir)) {
                rc.move(bestDir);
            }

            if (bestSweepDir != null) {
                if (rc.isActionReady() && rc.canMopSwing(bestSweepDir)) {
                    rc.mopSwing(bestSweepDir);
                }
            } else if (succLocation != null) {
                if (rc.isActionReady() && rc.canAttack(succLocation)) {
                    rc.attack(succLocation);
                }
            }
        }
    }

}
