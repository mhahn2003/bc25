package sprint;

import battlecode.common.*;

import java.util.function.Function;

public class Mopper extends Unit {

    private static boolean[][] computeEnemyLocs;

    public enum MopperState {
        DEFAULT,
        EVADE,
        REFILL,
        ATTACK,
        REFILL_OTHERS,
        BUILD_TOWER,
        BUILD_SRP,
        INACTION,
    }

    static MopperState state = MopperState.DEFAULT;

    public void act() throws GameActionException {
        super.act();
//        System.out.println("init: " + Clock.getBytecodeNum());
        upgrade();
//        System.out.println("upgrade: " + Clock.getBytecodeNum());
        drain();
//        System.out.println("drain: " + Clock.getBytecodeNum());
        evade();
//        System.out.println("evade: " + Clock.getBytecodeNum());
        refill();
//        System.out.println("refill: " + Clock.getBytecodeNum());
        attack();
//        System.out.println("attack: " + Clock.getBytecodeNum());
        buildTower();
//        System.out.println("buildTower: " + Clock.getBytecodeNum());
        refillOthers();
//        System.out.println("refillOthers: " + Clock.getBytecodeNum());
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
            state = MopperState.DEFAULT;
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

    public void attack() throws GameActionException {
        if (state == MopperState.REFILL || state == MopperState.EVADE) return;
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

        if (closestEnemy == null) return;
        state = MopperState.ATTACK;

        Logger.log("attack");

        final MapLocation finalClosestEnemy = closestEnemy.getLocation();
        computeBestAction(rc.getLocation(), newLoc -> newLoc.distanceSquaredTo(finalClosestEnemy));
    }

    public void refillOthers() throws GameActionException {
        if (rc.getPaint() <= 50 || state == MopperState.REFILL || state == MopperState.ATTACK || state == MopperState.BUILD_TOWER) return;
        RobotInfo[] friendlyRobots = rc.senseNearbyRobots(-1, myTeam);
        RobotInfo closestRobot = null;
        int minDist = Integer.MAX_VALUE;
        int transferAmount = 0;
        for (RobotInfo robot : friendlyRobots) {
            if (robot.getType().isRobotType()) {
                if ((robot.getType() != UnitType.MOPPER && robot.getPaintAmount() < robot.getType().paintCapacity * 0.4) || (robot.getType() == UnitType.MOPPER && robot.getPaintAmount() <= 10)) {
                    int dist = rc.getLocation().distanceSquaredTo(robot.getLocation());
                    if (dist < minDist) {
                        minDist = dist;
                        closestRobot = robot;
                        transferAmount = Math.min(rc.getPaint() - 40, robot.getType().paintCapacity - robot.getPaintAmount());
                    }
                }
            }
        }

        if (closestRobot == null) return;
        state = MopperState.REFILL_OTHERS;

        Logger.log("refillOthers: " + closestRobot + " " + transferAmount);

        if (rc.getLocation().distanceSquaredTo(closestRobot.getLocation()) <= 2) {
            if (rc.canTransferPaint(closestRobot.getLocation(), transferAmount)) {
                rc.transferPaint(closestRobot.getLocation(), transferAmount);
            }
        } else {
            if (rc.isMovementReady()) {
                for (Direction dir : Globals.adjacentDirections) {
                    MapLocation newLoc = rc.getLocation().add(dir);
                    if (rc.canMove(dir) && newLoc.distanceSquaredTo(closestRobot.getLocation()) <= 2) {
                        rc.move(dir);
                        if (rc.canTransferPaint(closestRobot.getLocation(), transferAmount)) {
                            rc.transferPaint(closestRobot.getLocation(), transferAmount);
                        }
                        return;
                    }
                }
                final MapLocation finalClosestRobot = closestRobot.getLocation();
                computeBestAction(rc.getLocation(), newLoc -> newLoc.distanceSquaredTo(finalClosestRobot)/3);
            }
        }
    }

    public void buildTower() throws GameActionException {
        if (state != MopperState.DEFAULT && state != MopperState.BUILD_TOWER) return;

        if (state == MopperState.DEFAULT) {
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
            final MapLocation paintLocation = bestEnemyPaintLocation;
            final MapLocation ruinLocation = buildRuinLocation;
            Logger.log("no mop counter: " + noMopCounter);
            if (noMopCounter >= Globals.noMopTowerThreshold) {
                Logger.log("no mop");
                Navigator.moveTo(paintLocation);
            } else {
                computeBestAction(rc.getLocation(), newLoc -> newLoc.distanceSquaredTo(paintLocation) <= 5 ? 0 : newLoc.distanceSquaredTo(paintLocation) + newLoc.distanceSquaredTo(ruinLocation) / 3);
            }
            if (rc.isActionReady()) noMopCounter++;
        } else {
            if (rc.getLocation().distanceSquaredTo(buildRuinLocation) <= 2 && rc.isActionReady()) {
                MapLocation markLoc = buildRuinLocation.add(Direction.EAST);
                UnitType towerType;
                PaintType mark = rc.senseMapInfo(markLoc).getPaint();
                if (mark == PaintType.EMPTY) towerType = UnitType.LEVEL_ONE_DEFENSE_TOWER;
                else if (mark.isSecondary()) towerType = UnitType.LEVEL_ONE_PAINT_TOWER;
                else towerType = UnitType.LEVEL_ONE_MONEY_TOWER;
                if (rc.canCompleteTowerPattern(towerType, buildRuinLocation)) {
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

    public void buildSRP() throws GameActionException {
        if (state != MopperState.BUILD_SRP && state != MopperState.DEFAULT || rc.getRoundNum() < 100) return;

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
            MapLocation loc = rc.getLocation();
            int x = (loc.x + 2) % 4;
            int y = (loc.y + 2) % 4;
            MapLocation[] possibleSRPLocations = new MapLocation[8];

            MapLocation loc1 = new MapLocation(loc.x - x, loc.y - y);
            if (rc.canSenseLocation(loc1) && !impossibleSRPLocations.contains(loc1) && !rc.senseMapInfo(loc1).isResourcePatternCenter()) {
                possibleSRPLocations[0] = loc1;
            }
            MapLocation loc2 = new MapLocation(loc.x - x, loc.y + 4 - y);
            if (rc.canSenseLocation(loc2) && !impossibleSRPLocations.contains(loc2) && !rc.senseMapInfo(loc2).isResourcePatternCenter()) {
                possibleSRPLocations[1] = loc2;
            }
            MapLocation loc3 = new MapLocation(loc.x + 4 - x, loc.y - y);
            if (rc.canSenseLocation(loc3) && !impossibleSRPLocations.contains(loc3) && !rc.senseMapInfo(loc3).isResourcePatternCenter()) {
                possibleSRPLocations[2] = loc3;
            }
            MapLocation loc4 = new MapLocation(loc.x + 4 - x, loc.y + 4 - y);
            if (rc.canSenseLocation(loc4) && !impossibleSRPLocations.contains(loc4) && !rc.senseMapInfo(loc4).isResourcePatternCenter()) {
                possibleSRPLocations[3] = loc4;
            }
            MapLocation loc5 = new MapLocation(loc.x - 4 - x, loc.y - y);
            if (rc.canSenseLocation(loc5) && !impossibleSRPLocations.contains(loc5) && !rc.senseMapInfo(loc5).isResourcePatternCenter()) {
                possibleSRPLocations[4] = loc5;
            }
            MapLocation loc6 = new MapLocation(loc.x - x, loc.y - 4 - y);
            if (rc.canSenseLocation(loc6) && !impossibleSRPLocations.contains(loc6) && !rc.senseMapInfo(loc6).isResourcePatternCenter()) {
                possibleSRPLocations[5] = loc6;
            }
            MapLocation loc7 = new MapLocation(loc.x + 4 - x, loc.y - 4 - y);
            if (rc.canSenseLocation(loc7) && !impossibleSRPLocations.contains(loc7) && !rc.senseMapInfo(loc7).isResourcePatternCenter()) {
                possibleSRPLocations[6] = loc7;
            }
            MapLocation loc8 = new MapLocation(loc.x - 4 - x, loc.y + 4 - y);
            if (rc.canSenseLocation(loc8) && !impossibleSRPLocations.contains(loc8) && !rc.senseMapInfo(loc8).isResourcePatternCenter()) {
                possibleSRPLocations[7] = loc8;
            }

            for (MapLocation ruin : rawRuinLocs) {
                for (int i = 0; i < possibleSRPLocations.length; i++) {
                    if (possibleSRPLocations[i] != null && Math.abs(ruin.x - possibleSRPLocations[i].x) <= 5 && Math.abs(ruin.y - possibleSRPLocations[i].y) <= 5) {
                        impossibleSRPLocations.add(possibleSRPLocations[i]);
                        possibleSRPLocations[i] = null;
                        break;
                    }
                }
            }

            MapLocation closestSRPLocation = null;
            int minDist = Integer.MAX_VALUE;
            for (MapLocation possibleSRPLocation : possibleSRPLocations) {
                if (possibleSRPLocation != null) {
                    int dist = loc.distanceSquaredTo(possibleSRPLocation);
                    if (dist < minDist) {
                        minDist = dist;
                        closestSRPLocation = possibleSRPLocation;
                    }
                }
            }

            if (closestSRPLocation != null) {
                state = MopperState.BUILD_SRP;
                buildSRPLocation = closestSRPLocation;
                noMopCounter = 0;
            } else {
                return;
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
    }

    public void move() throws GameActionException {
        Logger.log("state: " + state);
        if (state != MopperState.DEFAULT && state != MopperState.INACTION) return;

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
        } else {
            if (noActionCounter > noActionThreshold && state != MopperState.INACTION) {
                state = MopperState.INACTION;
                int totalDiagLength = mapWidth * mapWidth + mapHeight * mapHeight;
                flipLocation = null;
                if (rc.getLocation().distanceSquaredTo(exploreLocations[4]) < totalDiagLength/36) {
                    MapLocation furtherOpposite = new MapLocation(3 * exploreLocations[4].x - 2 * rc.getLocation().x, 3 * exploreLocations[4].y - 2 * rc.getLocation().y);
                    if (rc.onTheMap(furtherOpposite)) {
                        flipLocation = furtherOpposite;
                    }
                }
                if (flipLocation == null) {
                    flipLocation = new MapLocation(mapWidth - rc.getLocation().x - 1, mapHeight - rc.getLocation().y - 1);
                }
            }

            if (flipLocation != null) {
                if (rc.getLocation().distanceSquaredTo(flipLocation) <= 8) {
                    flipLocation = null;
                } else {
                    Logger.log("flip: " + flipLocation);
                    Navigator.moveTo(flipLocation);
                    return;
                }
            }
            Movement.wanderDirection();
            Logger.log("wander: " + wanderLocation);
            computeBestAction(rc.getLocation(), newLoc -> newLoc.distanceSquaredTo(wanderLocation) / 3);
            if (rc.isMovementReady()) {
                Navigator.moveTo(wanderLocation);
            }
        }
    }

    public void mopLeftover() throws GameActionException {
        if (!rc.isActionReady()) return;
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
        else return 5 * numEnemies;
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
            return new LocationHeuristic(maxPaintBenefit, bestLoc);
        } else {
            if (rc.isActionReady()) {
                if (rc.canSenseLocation(loc) && rc.senseMapInfo(loc).getPaint().isEnemy()) {
                    return new LocationHeuristic(5, loc);
                }
                for (Direction dir : Globals.adjacentDirections) {
                    MapLocation newLoc = loc.add(dir);
                    if (rc.canSenseLocation(newLoc) && rc.senseMapInfo(newLoc).getPaint().isEnemy()) {
                        return new LocationHeuristic(5, newLoc);
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
