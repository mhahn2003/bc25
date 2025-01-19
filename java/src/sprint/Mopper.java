package sprint;

import battlecode.common.*;
import org.apache.commons.lang3.tuple.Pair;

import java.util.function.Function;

public class Mopper extends Unit {

    public enum MopperState {
        DEFAULT,
        REFILL,
        ATTACK,
        REFILL_OTHERS,
        BUILD_TOWER,
        BUILD_SRP,
    }

    static MopperState state = MopperState.DEFAULT;

    public void act() throws GameActionException {
        flush();
        super.act();
        upgrade();
        drain();
        evade();
        refill();
        attack();
        refillOthers();
        buildTower();
        buildSRP();
        move();
    }

    public void flush() {
        upgradeTowerLocation = null;
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
        MapLocation enemyTower = null;
        for (MapLocation ruin : nearbyRuins) {
            RobotInfo robot = rc.senseRobotAtLocation(ruin);
            if (robot != null && robot.getType().isTowerType() && robot.getTeam() == opponentTeam) {
                enemyTower = ruin;
                break;
            }
        }
        if (enemyTower == null) return;
        state = MopperState.DEFAULT;

        final MapLocation finalEnemyTower = enemyTower;
        computeBestAction(rc.getLocation(), newLoc -> {
            int distanceToTower = newLoc.distanceSquaredTo(finalEnemyTower);
            return distanceToTower > 9 ? 0 : 30 * (9 - distanceToTower);
        });
    }

    public void refill() throws GameActionException {
        if (state != MopperState.REFILL && ((state == MopperState.DEFAULT && rc.getPaint() <= 40) || rc.getPaint() <= 20)) {
            Logger.log("need refill");
            MapLocation closestFriendPaintTower = null;
            int minDist = Integer.MAX_VALUE;
            MapLocation[] friendlyPaintTowerLocations = Globals.friendlyPaintTowerLocations.getLocations();
            for (MapLocation loc : friendlyPaintTowerLocations) {
                int dist = rc.getLocation().distanceSquaredTo(loc);
                if (dist < minDist) {
                    minDist = dist;
                    closestFriendPaintTower = loc;
                }
            }
            if (closestFriendPaintTower != null) {
                Logger.log("paint tower: " + closestFriendPaintTower);
                state = MopperState.REFILL;
                refillPaintTowerLocation = closestFriendPaintTower;
            } else {
                state = MopperState.DEFAULT;
                return;
            }
        }
        if (state == MopperState.REFILL) {
            Logger.log("refill state");
            if (rc.getPaint() >= 50) {
                state = MopperState.DEFAULT;
                return;
            }
            RobotInfo tower = null;
            if (rc.canSenseRobotAtLocation(refillPaintTowerLocation)) {
                tower = rc.senseRobotAtLocation(refillPaintTowerLocation);
                if (tower == null || tower.getTeam() == opponentTeam || (tower.getType().getBaseType() != UnitType.LEVEL_ONE_PAINT_TOWER && tower.getPaintAmount() < 15)) {
                    state = MopperState.DEFAULT;
                    return;
                }
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

            if (tower == null) {
                state = MopperState.DEFAULT;
                return;
            }

            if (rc.getLocation().distanceSquaredTo(refillPaintTowerLocation) <= 2) {
                if (rc.getPaint() <= 5 || tower.getPaintAmount() >= 30) {
                    int transferAmount = Math.min(UnitType.MOPPER.paintCapacity - rc.getPaint(), tower.getPaintAmount());
                    if (rc.canTransferPaint(refillPaintTowerLocation, -transferAmount)) {
                        rc.transferPaint(refillPaintTowerLocation, -transferAmount);
                        computeBestAction(rc.getLocation(), newLoc -> 0);
                    }
                }
            } else if (rc.getLocation().distanceSquaredTo(refillPaintTowerLocation) <= 8) {
                if ((rc.getPaint() <= 5 || tower.getPaintAmount() >= 30) && rc.isActionReady()) {
                    Navigator.moveTo(refillPaintTowerLocation);
                    if (rc.getLocation().distanceSquaredTo(refillPaintTowerLocation) <= 2) {
                        int transferAmount = Math.min(UnitType.MOPPER.paintCapacity - rc.getPaint(), tower.getPaintAmount());
                        if (rc.canTransferPaint(refillPaintTowerLocation, -transferAmount)) {
                            rc.transferPaint(refillPaintTowerLocation, -transferAmount);
                        }
                    }
                }
                computeBestAction(rc.getLocation(), newLoc -> 0);
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
        if (state == MopperState.REFILL) return;
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, opponentTeam);
        RobotInfo closestEnemy = null;
        int minDist = Integer.MAX_VALUE;
        for (RobotInfo enemy : enemyRobots) {
            if (enemy.getType().isRobotType()) {
                int dist = rc.getLocation().distanceSquaredTo(enemy.getLocation());
                if (dist < minDist) {
                    minDist = dist;
                    closestEnemy = enemy;
                }
            }
        }

        if (closestEnemy == null) return;
        state = MopperState.ATTACK;

        final MapLocation finalClosestEnemy = closestEnemy.getLocation();
        computeBestAction(rc.getLocation(), newLoc -> newLoc.distanceSquaredTo(finalClosestEnemy));
    }

    public void refillOthers() throws GameActionException {
        if (rc.getPaint() <= 50 || state == MopperState.REFILL || state == MopperState.ATTACK) return;
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
                computeBestAction(rc.getLocation(), newLoc -> newLoc.distanceSquaredTo(finalClosestRobot));
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
            noMopCounter = 0;
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
            final MapLocation paintLocation = bestEnemyPaintLocation;
            final MapLocation ruinLocation = buildRuinLocation;
            noMopCounter = 0;
            computeBestAction(rc.getLocation(), newLoc -> newLoc.distanceSquaredTo(paintLocation) + newLoc.distanceSquaredTo(ruinLocation));
        } else {
            if (rc.getLocation().distanceSquaredTo(buildRuinLocation) <= 8) noMopCounter++;
            if (noMopCounter >= Globals.noMopTowerThreshold) {
                impossibleRuinLocations.add(buildRuinLocation);
                state = MopperState.DEFAULT;
            } else {
                final MapLocation opposite = buildRuinLocation.add(rc.getLocation().directionTo(buildRuinLocation));
                computeBestAction(rc.getLocation(), newLoc -> newLoc.distanceSquaredTo(opposite));
            }
        }
    }

    public void buildSRP() throws GameActionException {
        if (state != MopperState.BUILD_SRP && state != MopperState.DEFAULT) return;

        MapLocation[] ruinLocs = ruinLocations.getLocations();
        FastSet rawRuins = new FastSet();
        for (MapLocation ruin : ruinLocs) {
            if (!friendlyPaintTowerLocations.contains(ruin) && !friendlyNonPaintTowerLocations.contains(ruin) && !enemyNonDefenseTowerLocations.contains(ruin) && !enemyDefenseTowerLocations.contains(ruin)) {
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
            MapLocation[] possibleSRPLocations = new MapLocation[5];

            if (x == 0 && y == 0 && !impossibleSRPLocations.contains(loc) && !rc.senseMapInfo(loc).isResourcePatternCenter()) {
                possibleSRPLocations[4] = loc;
            }
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

            for (MapLocation ruin : rawRuinLocs) {
                for (int i = 0; i < 5; i++) {
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
                computeBestAction(rc.getLocation(), newLoc -> newLoc.distanceSquaredTo(paintLocation) + newLoc.distanceSquaredTo(ruinLocation));
            } else {
                if (rc.getLocation().distanceSquaredTo(buildSRPLocation) <= 2 && rc.isActionReady() && rc.canCompleteResourcePattern(buildSRPLocation)) {
                    rc.completeResourcePattern(buildSRPLocation);
                    impossibleSRPLocations.add(buildSRPLocation);
                    state = MopperState.DEFAULT;
                } else if (rc.getLocation().distanceSquaredTo(buildSRPLocation) <= 8) {
                    noMopCounter++;
                } else if (noMopCounter >= Globals.noMopTowerThreshold || rc.getLocation().equals(buildSRPLocation)) {
                    impossibleSRPLocations.add(buildSRPLocation);
                    state = MopperState.DEFAULT;
                } else {
                    final MapLocation opposite = buildSRPLocation.add(rc.getLocation().directionTo(buildSRPLocation));
                    computeBestAction(rc.getLocation(), newLoc -> newLoc.distanceSquaredTo(opposite));
                }
            }
        }
    }

    public void move() throws GameActionException {
        if (state != MopperState.DEFAULT) return;

        Movement.wanderDirection();

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
            final MapLocation paintLocation = closestEnemyPaintLocation;
            computeBestAction(rc.getLocation(), newLoc -> newLoc.distanceSquaredTo(paintLocation) + newLoc.distanceSquaredTo(exploreLocations[wanderIndex]));
        } else {
            computeBestAction(rc.getLocation(), newLoc -> newLoc.distanceSquaredTo(exploreLocations[wanderIndex]));
        }
    }

    public static int sweepHeuristic(MapLocation loc, Direction dir) throws GameActionException {
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(loc, 5, opponentTeam);
        FastSet enemyLocs = new FastSet();
        for (RobotInfo enemy : enemyRobots) {
            enemyLocs.add(enemy.getLocation());
        }
        int numEnemies = 0;
        MapLocation loc1 = loc.add(dir);
        MapLocation loc2 = loc.add(dir.rotateLeft());
        MapLocation loc3 = loc.add(dir.rotateRight());
        MapLocation loc4 = loc.add(dir).add(dir);
        MapLocation loc5 = loc.add(dir).add(dir.rotateLeft());
        MapLocation loc6 = loc.add(dir).add(dir.rotateRight());
        if (enemyLocs.contains(loc1)) numEnemies++;
        if (enemyLocs.contains(loc2)) numEnemies++;
        if (enemyLocs.contains(loc3)) numEnemies++;
        if (enemyLocs.contains(loc4)) numEnemies++;
        if (enemyLocs.contains(loc5)) numEnemies++;
        if (enemyLocs.contains(loc6)) numEnemies++;
        return 5 * numEnemies - Util.paintPenalty(loc, rc.senseMapInfo(loc).getPaint());
    }

    public static Pair<Integer, MapLocation> succHeuristic(MapLocation loc) throws GameActionException {
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(loc, 8, opponentTeam);
        int maxPaintBenefit = 0;
        MapLocation bestLoc = null;
        if (rc.isActionReady()) {
            for (RobotInfo enemy : enemyRobots) {
                if (enemy.getType().isRobotType() && rc.senseMapInfo(enemy.getLocation()).getPaint().isEnemy()) {
                    int paintBenefit = Math.max(10, enemy.getPaintAmount()) + 5;
                    if (paintBenefit > maxPaintBenefit) {
                        maxPaintBenefit = paintBenefit;
                        bestLoc = enemy.getLocation();
                    }
                }
            }
        }

        PaintType paint = rc.senseMapInfo(loc).getPaint();
        if (maxPaintBenefit != 0) {
            return Pair.of(maxPaintBenefit - Util.paintPenalty(loc, paint), bestLoc);
        } else {
            if (rc.isActionReady()) {
                if (paint.isEnemy()) {
                    paint = PaintType.EMPTY;
                    return Pair.of(5-Util.paintPenalty(loc, paint), loc);
                }
                for (Direction dir : Globals.adjacentDirections) {
                    MapLocation newLoc = loc.add(dir);
                    if (rc.canSenseLocation(newLoc) && rc.senseMapInfo(newLoc).getPaint().isEnemy()) {
                        return Pair.of(5-Util.paintPenalty(loc, paint), newLoc);
                    }
                }
            }
            return Pair.of(-Util.paintPenalty(loc, paint), null);
        }
    }

    private void computeBestAction(MapLocation loc, Function<MapLocation, Integer> penaltyFunc) throws GameActionException {
        Direction bestDir = null;
        Direction bestSweepDir = null;
        MapLocation succLocation = null;
        int bestHeuristic = Integer.MIN_VALUE;

        // Check adjacent directions
        for (Direction dir : adjacentDirections) {
            MapLocation newLoc = loc.add(dir);
            int penalty = penaltyFunc.apply(newLoc);
            if (rc.canMove(dir)) {
                if (rc.isActionReady()) {
                    for (Direction sweepDir : Globals.cardinalDirections) {
                        int heuristic = sweepHeuristic(newLoc, sweepDir) - penalty;
                        if (heuristic > bestHeuristic) {
                            bestHeuristic = heuristic;
                            bestDir = dir;
                            bestSweepDir = sweepDir;
                            succLocation = null;
                        }
                    }
                }
                Pair<Integer, MapLocation> heuristicPair = succHeuristic(newLoc);
                int heuristic = heuristicPair.getLeft() - penalty;
                if (heuristic > bestHeuristic) {
                    bestHeuristic = heuristic;
                    bestDir = dir;
                    succLocation = heuristicPair.getRight();
                    bestSweepDir = null;
                }
            }
        }

        // Check current location
        int penalty = penaltyFunc.apply(loc);
        for (Direction dir : Globals.cardinalDirections) {
            int heuristic = sweepHeuristic(loc, dir) - penalty;
            if (heuristic > bestHeuristic) {
                bestHeuristic = heuristic;
                bestDir = Direction.CENTER;
                bestSweepDir = dir;
                succLocation = null;
            }
        }
        Pair<Integer, MapLocation> heuristicPair = succHeuristic(loc);
        int heuristic = heuristicPair.getLeft() - penalty;
        if (heuristic > bestHeuristic) {
            bestHeuristic = heuristic;
            bestDir = Direction.CENTER;
            succLocation = heuristicPair.getRight();
            bestSweepDir = null;
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
