package quals;

import battlecode.common.*;

public class Splasher extends Unit {

    private static int[][] computeSplashHeuristic;

    public enum SplasherState {
        DEFAULT,
        REFILL,
        INACTION,
    }

    static SplasherState state = SplasherState.DEFAULT;

    public void act() throws GameActionException {
        super.act();
//        System.out.println("init: " + Clock.getBytecodeNum());
        preprocess();
//        System.out.println("preprocess: " + Clock.getBytecodeNum());
        upgrade();
//        System.out.println("upgrade: " + Clock.getBytecodeNum());
        drain();
//        System.out.println("drain: " + Clock.getBytecodeNum());
        refill();
//        System.out.println("refill: " + Clock.getBytecodeNum());
        splash();
//        System.out.println("splash: " + Clock.getBytecodeNum());
        attack();
//        System.out.println("attack: " + Clock.getBytecodeNum());
        move();
//        System.out.println("move: " + Clock.getBytecodeNum());

        if (rc.isActionReady()) noActionCounter++;
        else noActionCounter = 0;
    }

    public void preprocess() throws GameActionException {
        if (!rc.isActionReady() && !rc.isMovementReady()) return;
        computeSplashHeuristic = new int[7][7];
        MapInfo[] mapInfos = rc.senseNearbyMapInfos(18);
        for (MapInfo mapInfo : mapInfos) {
            int x = mapInfo.getMapLocation().x - rc.getLocation().x + 3;
            int y = mapInfo.getMapLocation().y - rc.getLocation().y + 3;
            if (x < 0 || x >= 7 || y < 0 || y >= 7) continue;
            PaintType p = mapInfo.getPaint();
            if (p.isAlly() || mapInfo.isWall()) computeSplashHeuristic[x][y] = -2;
            else if (p == PaintType.EMPTY) computeSplashHeuristic[x][y] = 5;
            else computeSplashHeuristic[x][y] = 13;
        }
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(10, opponentTeam);
        for (RobotInfo robot : nearbyRobots) {
            if (robot.getType().isTowerType()) {
                int x = robot.getLocation().x - rc.getLocation().x + 3;
                int y = robot.getLocation().y - rc.getLocation().y + 3;
                computeSplashHeuristic[x][y] += 15;
            }
        }
    }

    public void upgrade() throws GameActionException {
        if (state != SplasherState.DEFAULT || upgradeTowerLocation == null) return;

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
                if (robot.getPaintAmount() >= 10 && rc.getPaint() <= 270) {
                    int transferAmount = Math.min(robot.getPaintAmount(), UnitType.SPLASHER.paintCapacity - rc.getPaint());
                    if (rc.canTransferPaint(ruin, -transferAmount)) {
                        rc.transferPaint(ruin, -transferAmount);
                    }
                }
            }
        }
    }

    public void refill() throws GameActionException {
        if (state != SplasherState.REFILL && ((state == SplasherState.DEFAULT && rc.getPaint() < 150) || rc.getPaint() <= 49) && rc.getChips() < 2000) {
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
                state = SplasherState.REFILL;
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
                    state = SplasherState.REFILL;
                    refillPaintTowerLocation = closestRefillTower;
                } else {
                    state = SplasherState.DEFAULT;
                    return;
                }
            }
        }
        if (state == SplasherState.REFILL) {
            if (rc.getPaint() >= 150) {
                state = SplasherState.DEFAULT;
                return;
            }
            RobotInfo tower = null;
            if (rc.canSenseRobotAtLocation(refillPaintTowerLocation)) {
                tower = rc.senseRobotAtLocation(refillPaintTowerLocation);
                if (tower == null || tower.getTeam() == opponentTeam || (tower.getType().getBaseType() != UnitType.LEVEL_ONE_PAINT_TOWER && tower.getPaintAmount() < 15)) {
                    noRefillTowerLocations.add(refillPaintTowerLocation);
                    state = SplasherState.DEFAULT;
                    return;
                }

                if (rc.getLocation().distanceSquaredTo(refillPaintTowerLocation) > 4) {
                    RobotInfo[] nearbyRobots = rc.senseNearbyRobots(refillPaintTowerLocation, 4, myTeam);
                    if (nearbyRobots.length >= 8) {
                        noRefillTowerLocations.add(refillPaintTowerLocation);
                        state = SplasherState.DEFAULT;
                        return;
                    }
                }
            } else if (rc.getLocation().distanceSquaredTo(refillPaintTowerLocation) <= GameConstants.VISION_RADIUS_SQUARED) {
                state = SplasherState.DEFAULT;
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
                int transferAmount = Math.min(UnitType.SPLASHER.paintCapacity - rc.getPaint(), tower.getPaintAmount());
                if (rc.canTransferPaint(refillPaintTowerLocation, -transferAmount)) {
                    rc.transferPaint(refillPaintTowerLocation, -transferAmount);
                    MapLocation opposite = new MapLocation(2 * refillPaintTowerLocation.x - rc.getLocation().x, 2 * refillPaintTowerLocation.y - rc.getLocation().y);
                    Navigator.moveTo(opposite);
                }
            } else if (rc.getLocation().distanceSquaredTo(refillPaintTowerLocation) <= 8) {
                if (rc.isActionReady()) {
                    Navigator.moveTo(refillPaintTowerLocation);
                    if (rc.getLocation().distanceSquaredTo(refillPaintTowerLocation) <= 2) {
                        int transferAmount = Math.min(UnitType.SPLASHER.paintCapacity - rc.getPaint(), tower.getPaintAmount());
                        if (rc.canTransferPaint(refillPaintTowerLocation, -transferAmount)) {
                            rc.transferPaint(refillPaintTowerLocation, -transferAmount);
                        }
                    }
                }
            } else {
                Navigator.moveTo(refillPaintTowerLocation);
            }
        }
    }

    public void attack() throws GameActionException {
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(-1, opponentTeam);
        if (nearbyEnemies.length == 0) return;

        RobotInfo target = null;
        int minDist = Integer.MAX_VALUE;
        for (RobotInfo enemy : nearbyEnemies) {
            if (enemy.getType().isTowerType()) {
                int dist = rc.getLocation().distanceSquaredTo(enemy.getLocation());
                if (dist < minDist) {
                    minDist = dist;
                    target = enemy;
                }
            }
        }
        if (target != null) {
            if (target.getType().getBaseType() == UnitType.LEVEL_ONE_DEFENSE_TOWER || !rc.isActionReady()) {
                MapLocation opposite = new MapLocation(2 * rc.getLocation().x - target.getLocation().x, 2 * rc.getLocation().y - target.getLocation().y);
                Logger.log("run away: " + target.getLocation() + " -> " + opposite);
                Navigator.moveTo(opposite);
            } else {
                RobotInfo [] alliedRobots = rc.senseNearbyRobots(target.getLocation(), 20, myTeam);
                int numSoldiers = 0;
                for (RobotInfo ally : alliedRobots) {
                    if (ally.getType() == UnitType.SOLDIER || ally.getType() == UnitType.SPLASHER) {
                        numSoldiers++;
                    }
                }
                if (numSoldiers < 2) {
                    MapLocation opposite = new MapLocation(2 * rc.getLocation().x - target.getLocation().x, 2 * rc.getLocation().y - target.getLocation().y);
                    Logger.log("run away: " + target.getLocation() + " -> " + opposite);
                    Navigator.moveTo(opposite);
                } else if (rc.getLocation().distanceSquaredTo(target.getLocation()) > 10) {
                    Navigator.moveTo(target.getLocation());
                }
            }
        }
    }

    public void splash() throws GameActionException {
        if (!rc.isActionReady()) return;

        int bestSplash = 0;
        MapLocation bestSplashLocation = null;
        MapLocation[] attackLocs = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 4);
        for (MapLocation loc : attackLocs) {
            int heuristic = getHeuristic(loc);
            if (heuristic > bestSplash) {
                bestSplash = heuristic;
                bestSplashLocation = loc;
            }
        }

        if (bestSplashLocation != null) {
            if (rc.canAttack(bestSplashLocation)) {
                rc.attack(bestSplashLocation);
            }
        }
    }

    private static int getHeuristic(MapLocation loc) {
        int x = loc.x - rc.getLocation().x + 3;
        int y = loc.y - rc.getLocation().y + 3;
        return computeSplashHeuristic[x-1][y-1] + computeSplashHeuristic[x-1][y] + computeSplashHeuristic[x-1][y+1] +
                computeSplashHeuristic[x][y-1] + computeSplashHeuristic[x][y] + computeSplashHeuristic[x][y+1] +
                computeSplashHeuristic[x+1][y-1] + computeSplashHeuristic[x+1][y] + computeSplashHeuristic[x+1][y+1] - 50;
    }

    public void move() throws GameActionException {
        if (!rc.isMovementReady()) return;

        Direction dir = null;
        int bestSplash = 0;

        // stay still
        int heuristic = getHeuristic(rc.getLocation());
        if (heuristic > bestSplash) {
            bestSplash = heuristic;
            dir = Direction.CENTER;
        }
        Direction centerDir = rc.getLocation().directionTo(exploreLocations[4]);
        for (int i = 0; i < 8; i++) {
            if (rc.canMove(centerDir)) {
                MapLocation loc = rc.getLocation().add(centerDir);
                heuristic = getHeuristic(loc);
                if (heuristic > bestSplash) {
                    bestSplash = heuristic;
                    dir = centerDir;
                }
            }
            centerDir = centerDir.rotateRight();
        }
        if (dir != null) {
            if (dir != Direction.CENTER) {
                rc.move(dir);
            }
        } else {
            if (state == SplasherState.REFILL) return;

            if (noActionCounter > noActionThreshold && state != SplasherState.INACTION) {
                state = SplasherState.INACTION;
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
            if (rc.isMovementReady()) {
                Navigator.moveTo(wanderLocation);
            }
        }
    }
}
