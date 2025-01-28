package finals;

import battlecode.common.*;

public class Splasher extends Unit {

    private static int[][] computeSplashHeuristic;

    public enum SplasherState {
        DEFAULT,
        REFILL,
        ATTACK,
    }

    static SplasherState state = SplasherState.DEFAULT;

    public void act() throws GameActionException {
        super.act();
//        System.out.println("init: " + Clock.getBytecodeNum());
        if (rc.getPaint() == 0) rc.disintegrate();
        preprocess();
//        System.out.println("preprocess: " + Clock.getBytecodeNum());
        upgrade();
//        System.out.println("upgrade: " + Clock.getBytecodeNum());
        drain();
//        System.out.println("drain: " + Clock.getBytecodeNum());
        attack();
//        System.out.println("attack: " + Clock.getBytecodeNum());
        refill();
//        System.out.println("refill: " + Clock.getBytecodeNum());
        splash();
//        System.out.println("splash: " + Clock.getBytecodeNum());
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

        MapLocation[] nearbyRuins = rc.senseNearbyRuins(-1);
        for (MapLocation ruin : nearbyRuins) {
            RobotInfo robot = rc.senseRobotAtLocation(ruin);
            if (robot == null) {
                int x = ruin.x - rc.getLocation().x + 3;
                int y = ruin.y - rc.getLocation().y + 3;
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dy = -2; dy <= 2; dy++) {
                        if (x + dx >= 0 && x + dx < 7 && y + dy >= 0 && y + dy < 7 && rc.canSenseLocation(ruin.translate(dx, dy)) && rc.senseMapInfo(ruin.translate(dx, dy)).getPaint().isEnemy()) {
                            computeSplashHeuristic[x + dx][y + dy] += 10;
                        }
                    }
                }
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

    public void attack() throws GameActionException {
        if (rc.getPaint() < 50) {
            state = SplasherState.DEFAULT;
            return;
        }
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
            if (target.getType().getBaseType() == UnitType.LEVEL_ONE_DEFENSE_TOWER) {
                if (closestDefenseTower == null) {
                    closestDefenseTower = target.getLocation();
                } else {
                    int dist = rc.getLocation().distanceSquaredTo(target.getLocation());
                    int closestDist = rc.getLocation().distanceSquaredTo(closestDefenseTower);
                    if (dist < closestDist) {
                        closestDefenseTower = target.getLocation();
                    }
                }
                MapLocation opposite = new MapLocation(2 * rc.getLocation().x - target.getLocation().x, 2 * rc.getLocation().y - target.getLocation().y);
                Logger.log("run away: " + target.getLocation() + " -> " + opposite);
                Navigator.moveTo(opposite);
            } else {
                state = SplasherState.ATTACK;
                if (rc.getLocation().distanceSquaredTo(target.getLocation()) != 10) {
                    for (Direction dir : Globals.adjacentDirections) {
                        MapLocation loc = rc.getLocation().add(dir);
                        if (rc.canMove(dir) && loc.distanceSquaredTo(target.getLocation()) == 10) {
                            rc.move(dir);
                            break;
                        }
                    }
                }

                if (rc.getLocation().distanceSquaredTo(target.getLocation()) == 16) {
                    Direction dir = rc.getLocation().directionTo(target.getLocation());
                    MapLocation attackLoc = rc.getLocation().add(dir).add(dir);
                    if (rc.canAttack(attackLoc)) {
                        rc.attack(attackLoc);
                    }
                } else if (rc.getLocation().distanceSquaredTo(target.getLocation()) == 10) {
                    Direction dir = rc.getLocation().directionTo(target.getLocation()).opposite();
                    MapLocation farAttackLoc = target.getLocation().add(dir).add(dir);
                    MapLocation closeAttackLoc = rc.getLocation().add(dir.opposite()).add(dir.opposite());
                    if (getHeuristic(farAttackLoc) > getHeuristic(closeAttackLoc)) {
                        if (rc.canAttack(farAttackLoc)) {
                            rc.attack(farAttackLoc);
                        }
                    } else {
                        if (rc.canAttack(closeAttackLoc)) {
                            rc.attack(closeAttackLoc);
                        }
                    }
                } else {
                    MapLocation[] attackLocs = new MapLocation[12];
                    attackLocs[0] = new MapLocation(target.getLocation().x, target.getLocation().y + 4);
                    attackLocs[1] = new MapLocation(target.getLocation().x, target.getLocation().y - 4);
                    attackLocs[2] = new MapLocation(target.getLocation().x + 4, target.getLocation().y);
                    attackLocs[3] = new MapLocation(target.getLocation().x - 4, target.getLocation().y);
                    attackLocs[4] = new MapLocation(target.getLocation().x + 3, target.getLocation().y + 1);
                    attackLocs[5] = new MapLocation(target.getLocation().x + 3, target.getLocation().y - 1);
                    attackLocs[6] = new MapLocation(target.getLocation().x - 3, target.getLocation().y + 1);
                    attackLocs[7] = new MapLocation(target.getLocation().x - 3, target.getLocation().y - 1);
                    attackLocs[8] = new MapLocation(target.getLocation().x + 1, target.getLocation().y + 3);
                    attackLocs[9] = new MapLocation(target.getLocation().x - 1, target.getLocation().y + 3);
                    attackLocs[10] = new MapLocation(target.getLocation().x + 1, target.getLocation().y - 3);
                    attackLocs[11] = new MapLocation(target.getLocation().x - 1, target.getLocation().y - 3);
                    minDist = Integer.MAX_VALUE;
                    MapLocation bestLoc = null;
                    for (MapLocation loc : attackLocs) {
                        if (rc.onTheMap(loc)) {
                            if (rc.canSenseLocation(loc)) {
                                RobotInfo robot = rc.senseRobotAtLocation(loc);
                                if (robot != null || rc.senseMapInfo(loc).isWall()) continue;
                            }
                            int dist = rc.getLocation().distanceSquaredTo(loc);
                            if (dist < minDist) {
                                minDist = dist;
                                bestLoc = loc;
                            }
                        }
                    }
                    if (bestLoc != null) {
                        Navigator.moveTo(bestLoc);
                    } else {
                        state = SplasherState.DEFAULT;
                    }
                }
            }
        } else {
            state = SplasherState.DEFAULT;
        }
    }

    public void refill() throws GameActionException {
        if (state != SplasherState.REFILL && rc.getPaint() < 50) {
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
                    refillPaintTowerLocation = null;
                }
            }
        }
        if (state == SplasherState.REFILL) {
            if (rc.getPaint() > 100) {
                state = SplasherState.DEFAULT;
                return;
            }
            RobotInfo tower = null;
            if (refillPaintTowerLocation != null) {
                if (rc.canSenseRobotAtLocation(refillPaintTowerLocation)) {
                    tower = rc.senseRobotAtLocation(refillPaintTowerLocation);
                    if (tower == null || tower.getTeam() == opponentTeam || (tower.getType().getBaseType() != UnitType.LEVEL_ONE_PAINT_TOWER && tower.getPaintAmount() < 15)) {
                        noRefillTowerLocations.add(refillPaintTowerLocation);
                        refillPaintTowerLocation = null;
                    } else if (rc.getLocation().distanceSquaredTo(refillPaintTowerLocation) > 4) {
                        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(refillPaintTowerLocation, 4, myTeam);
                        if (nearbyRobots.length >= 8) {
                            noRefillTowerLocations.add(refillPaintTowerLocation);
                            refillPaintTowerLocation = null;
                        }
                    }
                }
            }

            RobotInfo[] nearbyMoppers = rc.senseNearbyRobots(-1, myTeam);
            MapLocation closestMopper = null;
            int minDist = Integer.MAX_VALUE;
            for (RobotInfo mopper : nearbyMoppers) {
                if (mopper.getType() == UnitType.MOPPER && mopper.getPaintAmount() <= UnitType.MOPPER.paintCapacity - rc.getPaint()) {
                    int dist = rc.getLocation().distanceSquaredTo(mopper.getLocation());
                    if (dist < minDist) {
                        minDist = dist;
                        closestMopper = mopper.getLocation();
                    }
                }
            }
            if (closestMopper != null) {
                if (rc.getLocation().distanceSquaredTo(closestMopper) > 2) {
                    Navigator.moveTo(closestMopper);
                }
            } else {
                if (tower != null && refillPaintTowerLocation != null && rc.getLocation().distanceSquaredTo(refillPaintTowerLocation) <= 2) {
                    int transferAmount = Math.min(UnitType.SOLDIER.paintCapacity - rc.getPaint(), tower.getPaintAmount());
                    if (rc.canTransferPaint(refillPaintTowerLocation, -transferAmount)) {
                        rc.transferPaint(refillPaintTowerLocation, -transferAmount);
                    }
                } else {
                    if (refillPaintTowerLocation != null) {
                        Navigator.moveTo(refillPaintTowerLocation);
                    } else {
                        state = SplasherState.DEFAULT;
                    }
                }
            }
        }
    }

    public void splash() throws GameActionException {
        if (!rc.isActionReady() || rc.getPaint() < 50) return;

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
        if (!rc.isMovementReady() || state == SplasherState.ATTACK || state == SplasherState.REFILL) return;

        Direction dir = null;
        int bestSplash = 0;

        // stay still
        int heuristic = getHeuristic(rc.getLocation());
        if (heuristic > bestSplash) {
            bestSplash = heuristic;
            dir = Direction.CENTER;
        }
        Direction centerDir = rc.getLocation().directionTo(exploreLocations[4]);
        if (centerDir == Direction.CENTER) centerDir = Direction.NORTH;
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
            Util.checkSymmetry();
            MapLocation base = Util.getBaseToVisit();
            if (base != null) {
                Logger.log("base rush: " + base);
                Navigator.moveTo(base);
            }

            if (rc.isMovementReady()) {
                Direction bestDir = Movement.wanderDirection();
                Logger.log("wander: " + wanderLocation);
                RobotInfo[] nearbyAllies = rc.senseNearbyRobots(8, myTeam);
                if (nearbyAllies.length < 3) {
                    Navigator.moveTo(wanderLocation);
                } else if (bestDir != null && rc.canMove(bestDir)) {
                    rc.move(bestDir);
                }
            }
        }
    }
}
