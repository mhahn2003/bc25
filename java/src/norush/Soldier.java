package norush;

import battlecode.common.*;

public class Soldier extends Unit {

    public enum SoldierState {
        DEFAULT,
        ATTACK,
        REFILL,
        RUSH,
        BUILD_TOWER,
        BUILD_SRP,
        INACTION,
    }

    static SoldierState state = SoldierState.DEFAULT;

    public void act() throws GameActionException {
        super.act();
//        System.out.println("init: " + Clock.getBytecodeNum());
        upgrade();
//        System.out.println("upgrade: " + Clock.getBytecodeNum());
        drain();
//        System.out.println("drain: " + Clock.getBytecodeNum());
        attack();
//        System.out.println("attack: " + Clock.getBytecodeNum());
        refill();
//        System.out.println("refill: " + Clock.getBytecodeNum());
        taint();
//        System.out.println("taint: " + Clock.getBytecodeNum());
        buildTower();
//        System.out.println("build: " + Clock.getBytecodeNum());
        buildSRP();
//        System.out.println("paint: " + Clock.getBytecodeNum());
        move();
//        System.out.println("move: " + Clock.getBytecodeNum());
        paintLeftover();
//        System.out.println("paintLeftover: " + Clock.getBytecodeNum());
        Comms.sendMessagesToTower();

        if (rc.isActionReady()) noActionCounter++;
        else noActionCounter = 0;
    }

    public void upgrade() throws GameActionException {
        if (state != SoldierState.DEFAULT || upgradeTowerLocation == null) return;

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
                if (robot.getPaintAmount() >= 10 && rc.getPaint() <= 150) {
                    int transferAmount = Math.min(robot.getPaintAmount(), UnitType.SOLDIER.paintCapacity - rc.getPaint());
                    if (rc.canTransferPaint(ruin, -transferAmount)) {
                        rc.transferPaint(ruin, -transferAmount);
                    }
                }
            }
        }
    }

    public void attack() throws GameActionException {
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, opponentTeam);
        MapLocation closestTower = null;
        int minDist = Integer.MAX_VALUE;
        for (RobotInfo robot : enemyRobots) {
            if (robot.getType().isTowerType()) {
                MapLocation robotLoc = robot.getLocation();
                int dist = rc.getLocation().distanceSquaredTo(robotLoc);
                if (dist < minDist) {
                    minDist = dist;
                    closestTower = robotLoc;
                }
            }
        }

        if (closestTower != null) {
            state = SoldierState.ATTACK;
            if (rc.getLocation().distanceSquaredTo(closestTower) <= 9) {
                if (rc.canAttack(closestTower)) {
                    rc.attack(closestTower);
                }
                Direction dir = rc.getLocation().directionTo(closestTower).opposite();
                if (rc.canMove(dir)) {
                    rc.move(dir);
                }
                Direction left = dir.rotateLeft();
                if (rc.canMove(left)) {
                    rc.move(left);
                }
                Direction right = dir.rotateRight();
                if (rc.canMove(right)) {
                    rc.move(right);
                }
            } else {
                for (Direction dir : Globals.adjacentDirections) {
                    MapLocation loc = rc.getLocation().add(dir);
                    if (rc.canMove(dir) && loc.distanceSquaredTo(closestTower) <= 9) {
                        rc.move(dir);
                        break;
                    }
                }
                if (rc.isMovementReady()) {
                    Navigator.moveTo(closestTower);
                }
                if (rc.canAttack(closestTower)) {
                    rc.attack(closestTower);
                }
            }
        } else {
            if (state == SoldierState.ATTACK) {
                state = SoldierState.DEFAULT;
            }
        }
    }

    public void refill() throws GameActionException {
        if (state == SoldierState.INACTION) return;
        if (state != SoldierState.REFILL && state != SoldierState.ATTACK && ((state == SoldierState.DEFAULT && rc.getPaint() < 80) || rc.getPaint() <= 25) && rc.getChips() < 2000) {
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
                if (state == SoldierState.BUILD_TOWER) {
                    continueBuild = true;
                }
                state = SoldierState.REFILL;
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
                    if (state == SoldierState.BUILD_TOWER) {
                        continueBuild = true;
                    }
                    state = SoldierState.REFILL;
                    refillPaintTowerLocation = closestRefillTower;
                } else {
                    state = SoldierState.DEFAULT;
                    return;
                }
            }
        }
        if (state == SoldierState.REFILL) {
            Logger.log("refill state");
            if (rc.getPaint() >= 80 || refillPaintTowerLocation == null) {
                if (continueBuild) state = SoldierState.BUILD_TOWER;
                else state = SoldierState.DEFAULT;
                return;
            }
            RobotInfo tower = null;
            if (rc.canSenseRobotAtLocation(refillPaintTowerLocation)) {
                tower = rc.senseRobotAtLocation(refillPaintTowerLocation);
                if (tower == null || tower.getTeam() == opponentTeam || (tower.getType().getBaseType() != UnitType.LEVEL_ONE_PAINT_TOWER && tower.getPaintAmount() < 15)) {
                    noRefillTowerLocations.add(refillPaintTowerLocation);
                    if (continueBuild) state = SoldierState.BUILD_TOWER;
                    else state = SoldierState.DEFAULT;
                    return;
                }

                if (rc.getLocation().distanceSquaredTo(refillPaintTowerLocation) > 4) {
                    RobotInfo[] nearbyRobots = rc.senseNearbyRobots(refillPaintTowerLocation, 4, myTeam);
                    if (nearbyRobots.length >= 8) {
                        noRefillTowerLocations.add(refillPaintTowerLocation);
                        if (continueBuild) state = SoldierState.BUILD_TOWER;
                        else state = SoldierState.DEFAULT;
                        return;
                    }
                }
            } else if (rc.getLocation().distanceSquaredTo(refillPaintTowerLocation) <= GameConstants.VISION_RADIUS_SQUARED) {
                if (continueBuild) state = SoldierState.BUILD_TOWER;
                else state = SoldierState.DEFAULT;
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
                int transferAmount = Math.min(UnitType.SOLDIER.paintCapacity - rc.getPaint(), tower.getPaintAmount());
                if (rc.canTransferPaint(refillPaintTowerLocation, -transferAmount)) {
                    rc.transferPaint(refillPaintTowerLocation, -transferAmount);
                    MapLocation opposite = new MapLocation(2 * refillPaintTowerLocation.x - rc.getLocation().x, 2 * refillPaintTowerLocation.y - rc.getLocation().y);
                    Navigator.moveTo(opposite);
                }
            } else if (rc.getLocation().distanceSquaredTo(refillPaintTowerLocation) <= 8) {
                if (rc.isActionReady()) {
                    Navigator.moveTo(refillPaintTowerLocation);
                    if (rc.getLocation().distanceSquaredTo(refillPaintTowerLocation) <= 2) {
                        int transferAmount = Math.min(UnitType.SOLDIER.paintCapacity - rc.getPaint(), tower.getPaintAmount());
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

    public void taint() throws GameActionException {
        if (!rc.isActionReady()) return;
        if (state == SoldierState.DEFAULT || state == SoldierState.RUSH) {
            MapLocation[] nearbyRuins = rc.senseNearbyRuins(-1);
            for (MapLocation ruin : nearbyRuins) {
                if (taintedRuins.contains(ruin)) {
                    continue;
                }
                RobotInfo robot = rc.senseRobotAtLocation(ruin);
                if (robot == null) {
                    MapInfo[] taintableTiles = rc.senseNearbyMapInfos(ruin, 8);
                    boolean tainted = false;
                    MapLocation bestPaintLocation = null;
                    int closestDist = Integer.MAX_VALUE;
                    for (MapInfo info : taintableTiles) {
                        if (info.getPaint().isAlly()) {
                            tainted = true;
                        }
                        if (info.getPaint() == PaintType.EMPTY) {
                            if (rc.getLocation().distanceSquaredTo(info.getMapLocation()) <= closestDist) {
                                closestDist = rc.getLocation().distanceSquaredTo(info.getMapLocation());
                                bestPaintLocation = info.getMapLocation();
                            }
                        }
                    }
                    if (tainted) {
                        taintedRuins.add(ruin);
                    }
                    if (!tainted && bestPaintLocation != null) {
                        if (rc.canAttack(bestPaintLocation)) {
                            rc.attack(bestPaintLocation, Util.useSecondaryForTower(bestPaintLocation, ruin, Util.towerType()));
                            taintedRuins.add(ruin);
                        }
                    }
                }
            }
        }
    }

    public void buildTower() throws GameActionException {
        if (state != SoldierState.BUILD_TOWER && state != SoldierState.DEFAULT) return;

        if (state == SoldierState.DEFAULT) {
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
            MapLocation markLoc = closestRuin.add(Direction.EAST);
            UnitType towerType = Util.towerType();
            if (rc.canSenseLocation(markLoc)) {
                if (rc.senseMapInfo(markLoc).getMark() == PaintType.EMPTY) {
                    if (towerType != UnitType.LEVEL_ONE_DEFENSE_TOWER) {
                        if (rc.getLocation().distanceSquaredTo(markLoc) <= 2) {
                            if (rc.canMark(markLoc)) {
                                // false = money, true = paint
                                rc.mark(markLoc, towerType == UnitType.LEVEL_ONE_PAINT_TOWER);
                            }
                        } else {
                            Navigator.moveTo(markLoc);
                            return;
                        }
                    }
                } else {
                    if (rc.senseMapInfo(markLoc).getMark().isSecondary()) {
                        towerType = UnitType.LEVEL_ONE_PAINT_TOWER;
                    } else {
                        towerType = UnitType.LEVEL_ONE_MONEY_TOWER;
                    }
                }
            } else {
                Navigator.moveTo(markLoc);
                return;
            }

            state = SoldierState.BUILD_TOWER;
            buildTowerType = towerType;
            buildRuinLocation = closestRuin;
            noPaintCounter = 0;
        }

        if (rc.getNumberTowers() == 25) {
            state = SoldierState.DEFAULT;
            return;
        }

        if (rc.canSenseRobotAtLocation(buildRuinLocation)) {
            RobotInfo robot = rc.senseRobotAtLocation(buildRuinLocation);
            if (robot != null) {
                state = SoldierState.DEFAULT;
                impossibleRuinLocations.add(buildRuinLocation);
                return;
            }
        }
        MapInfo[] nearbyLocations = rc.senseNearbyMapInfos(buildRuinLocation, 8);

        int numEnemyPaint = 0;
        int numSoldiersBuilding = 0;
        for (MapInfo info : nearbyLocations) {
            if (info.getPaint().isEnemy()) {
                numEnemyPaint += 1;
            }
        }

        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(buildRuinLocation, 8, myTeam);
        for (RobotInfo ally : nearbyAllies) {
            if (ally.getType() == UnitType.SOLDIER && ally.getLocation().distanceSquaredTo(buildRuinLocation) <= 1) {
                numSoldiersBuilding += 1;
            }
            if (ally.getType() == UnitType.MOPPER) {
                numEnemyPaint -= 3;
            }
        }

        if (numEnemyPaint > 0 || (numSoldiersBuilding >= 2 && rc.getLocation().distanceSquaredTo(buildRuinLocation) > 1)) {
            state = SoldierState.DEFAULT;
            impossibleRuinLocations.add(buildRuinLocation);
            return;
        }

        Logger.log("buildTower: " + buildRuinLocation + " " + buildTowerType);
        continueBuild = false;

        if (rc.getLocation().distanceSquaredTo(buildRuinLocation) <= 2 && rc.getPaint() < 5) {
            if (rc.getPaint() == 0) rc.disintegrate();
            else {
                state = SoldierState.REFILL;
                return;
            }
        }

        UnitType newTowerType = Util.towerType();
        if (buildTowerType != newTowerType) {
            if (!(buildTowerType == UnitType.LEVEL_ONE_PAINT_TOWER && rc.getNumberTowers() < 10)) {
                Logger.log("need overwrite");
                MapLocation markLoc = buildRuinLocation.add(Direction.EAST);
                if (rc.canSenseLocation(markLoc)) {
                    boolean overwrite = newTowerType == UnitType.LEVEL_ONE_PAINT_TOWER;
                    if (rc.senseMapInfo(markLoc).getMark().isSecondary() != overwrite) {
                        if (rc.getLocation().distanceSquaredTo(markLoc) <= 2) {
                            if (rc.canMark(markLoc)) {
                                rc.mark(markLoc, overwrite);
                                buildTowerType = newTowerType;
                            }
                        } else {
                            Navigator.moveTo(markLoc);
                            return;
                        }
                    }
                } else {
                    Navigator.moveTo(markLoc);
                    return;
                }
            }
        }

        if (rc.getLocation().distanceSquaredTo(buildRuinLocation) <= 2 && rc.isActionReady() && rc.canCompleteTowerPattern(buildTowerType, buildRuinLocation)) {
            rc.completeTowerPattern(buildTowerType, buildRuinLocation);
            MapLocation[] impossibleSRPLocs = impossibleSRPLocations.getLocations();
            for (MapLocation loc : impossibleSRPLocs) {
                if (Math.abs(loc.x - buildRuinLocation.x) <= 5 && Math.abs(loc.y - buildRuinLocation.y) <= 5) {
                    impossibleSRPLocations.remove(loc);
                }
            }
            state = SoldierState.DEFAULT;
            return;
        }
        rotateAroundTower(buildRuinLocation, buildTowerType, true);
        if (rc.isActionReady() && rc.getLocation().distanceSquaredTo(buildRuinLocation) <= 2 && rc.getChips() < 800) {
            noPaintCounter += 1;
            if (noPaintCounter >= noPaintTowerThreshold) {
                state = SoldierState.DEFAULT;
                impossibleRuinLocations.add(buildRuinLocation);
            }
        } else {
            noPaintCounter = 0;
        }
    }

    public void buildSRP() throws GameActionException {
        if (state != SoldierState.BUILD_SRP && state != SoldierState.DEFAULT) return;
        if (rc.getNumberTowers() < 6 && rc.getRoundNum() < 100) {
            state = SoldierState.DEFAULT;
            return;
        }

        MapLocation[] ruinLocs = ruinLocations.getLocations();
        FastSet rawRuins = new FastSet();
        for (MapLocation ruin : ruinLocs) {
            if (!friendlyTowerLocations.contains(ruin) && !enemyTowerLocations.contains(ruin)) {
                rawRuins.add(ruin);
            }
        }
        MapLocation[] rawRuinLocs = rawRuins.getLocations();
        if (rawRuinLocs.length != 0) {
            if (state == SoldierState.BUILD_SRP) {
                for (MapLocation rawRuin : rawRuinLocs) {
                    if (Math.abs(rawRuin.x - buildSRPLocation.x) <= 5 && Math.abs(rawRuin.y - buildSRPLocation.y) <= 5) {
                        impossibleSRPLocations.add(buildSRPLocation);
                        state = SoldierState.DEFAULT;
                        return;
                    }
                }
            }
        }

        if (state == SoldierState.DEFAULT) {
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
                state = SoldierState.BUILD_SRP;
                buildSRPLocation = closestSRPLocation;
                noPaintCounter = 0;
            } else {
                return;
            }
        }

        Logger.log("buildSRP: " + buildSRPLocation);

        if (rc.canSenseLocation(buildSRPLocation)) {
            if (rc.senseMapInfo(buildSRPLocation).isResourcePatternCenter()) {
                impossibleSRPLocations.add(buildSRPLocation);
                state = SoldierState.DEFAULT;
                return;
            }
        }

        if (rc.getLocation().distanceSquaredTo(buildSRPLocation) <= 2) {
            if (!rc.canMarkResourcePattern(buildSRPLocation)) {
                impossibleSRPLocations.add(buildSRPLocation);
                state = SoldierState.DEFAULT;
                return;
            }
        }

        MapInfo[] nearbyLocations = rc.senseNearbyMapInfos(buildSRPLocation, 8);

        int numEnemyPaint = 0;
        int numSoldiersBuilding = 0;
        for (MapInfo info : nearbyLocations) {
            if (info.getPaint().isEnemy()) {
                numEnemyPaint += 1;
            }
            if (info.isWall()) {
                impossibleSRPLocations.add(buildSRPLocation);
                state = SoldierState.DEFAULT;
                return;
            }
        }

        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(buildSRPLocation, 8, myTeam);
        for (RobotInfo ally : nearbyAllies) {
            if (ally.getType() == UnitType.SOLDIER && ally.getLocation().distanceSquaredTo(buildSRPLocation) <= 1) {
                numSoldiersBuilding += 1;
            }
            if (ally.getType() == UnitType.MOPPER && rc.getRoundNum() >= 100) {
                numEnemyPaint -= 3;
            }
        }

        if (numEnemyPaint > 0 || (numSoldiersBuilding >= 2 && rc.getLocation().distanceSquaredTo(buildSRPLocation) > 1)) {
            state = SoldierState.DEFAULT;
            impossibleSRPLocations.add(buildSRPLocation);
            return;
        }

        if (rc.getLocation().distanceSquaredTo(buildSRPLocation) <= 2 && rc.isActionReady() && rc.canCompleteResourcePattern(buildSRPLocation)) {
            rc.completeResourcePattern(buildSRPLocation);
            impossibleSRPLocations.add(buildSRPLocation);
            state = SoldierState.DEFAULT;
        } else {
            rotateAroundTower(buildSRPLocation, UnitType.LEVEL_ONE_MONEY_TOWER, false);
            if (rc.isActionReady() && rc.getLocation().distanceSquaredTo(buildSRPLocation) <= 2) {
                noPaintCounter += 1;
                if (noPaintCounter >= noPaintSRPThreshold) {
                    state = SoldierState.DEFAULT;
                    impossibleSRPLocations.add(buildSRPLocation);
                }
            } else {
                noPaintCounter = 0;
            }
        }
    }

    public void move() throws GameActionException {
        if ((state != SoldierState.INACTION && state != SoldierState.DEFAULT) || !rc.isMovementReady()) return;
        if (noActionCounter > noActionThreshold && state != SoldierState.INACTION) {
            state = SoldierState.INACTION;
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

        if (state == SoldierState.INACTION && noActionCounter != 0 && rc.getLocation().distanceSquaredTo(flipLocation) > 8) {
            Logger.log("flip: " + flipLocation);
            Navigator.moveTo(flipLocation);
        } else {
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

    public void paintLeftover() throws GameActionException {
        if (state != SoldierState.DEFAULT || !rc.isActionReady()) return;

        if (rc.getNumberTowers() < 4 && rc.getRoundNum() < 100) return;
        MapInfo locInfo = rc.senseMapInfo(rc.getLocation());
        if (locInfo.getPaint() == PaintType.EMPTY) {
            MapLocation[] nearbyRuins = rc.senseNearbyRuins(8);
            if (nearbyRuins.length == 0) {
                if (rc.canAttack(rc.getLocation())) {
                    rc.attack(rc.getLocation(), Util.useSecondary(rc.getLocation()));
                }
            } else {
                MapLocation closestRuin = nearbyRuins[0];
                if (rc.canAttack(rc.getLocation())) {
                    rc.attack(rc.getLocation(), Util.useSecondaryForTower(rc.getLocation(), closestRuin, Util.towerType()));
                }
            }
        } else {
            if (rc.getNumberTowers() < 6 && rc.getRoundNum() < 200) return;
            MapInfo[] nearbyPaintLocations = rc.senseNearbyMapInfos(9);
            MapLocation[] nearbyRuins = rc.senseNearbyRuins(-1);
            for (MapInfo info : nearbyPaintLocations) {
                if (info.getPaint() == PaintType.EMPTY) {
                    boolean nearRuin = false;
                    for (MapLocation ruin : nearbyRuins) {
                        if (info.getMapLocation().distanceSquaredTo(ruin) <= 8) {
                            nearRuin = true;
                            break;
                        }
                    }
                    if (!nearRuin) {
                        if (rc.canAttack(info.getMapLocation())) {
                            rc.attack(info.getMapLocation(), Util.useSecondary(info.getMapLocation()));
                            return;
                        }
                    }
                }
            }
            for (MapInfo info : nearbyPaintLocations) {
                if (info.getPaint() == PaintType.EMPTY) {
                    for (MapLocation ruin : nearbyRuins) {
                        if (info.getMapLocation().distanceSquaredTo(ruin) <= 8) {
                            MapLocation markLoc = ruin.add(Direction.EAST);
                            if (rc.canSenseLocation(markLoc)) {
                                if (rc.senseMapInfo(markLoc).getMark() == PaintType.EMPTY) {
                                    if (rc.canAttack(info.getMapLocation())) {
                                        rc.attack(info.getMapLocation(), Util.useSecondaryForTower(info.getMapLocation(), ruin, Util.towerType()));
                                        return;
                                    }
                                } else {
                                    UnitType type = rc.senseMapInfo(markLoc).getMark().isSecondary() ? UnitType.LEVEL_ONE_PAINT_TOWER : UnitType.LEVEL_ONE_MONEY_TOWER;
                                    if (rc.canAttack(info.getMapLocation())) {
                                        rc.attack(info.getMapLocation(), Util.useSecondaryForTower(info.getMapLocation(), ruin, type));
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void rotateAroundTower(MapLocation center, UnitType type, boolean forTower) throws GameActionException {
        Direction dir = rc.getLocation().directionTo(center);
        if (dir == Direction.NORTHEAST || dir == Direction.NORTHWEST || dir == Direction.SOUTHEAST || dir == Direction.SOUTHWEST) {
            dir = dir.rotateLeft();
        }
        MapLocation top = center.add(dir);
        MapLocation right = center.add(dir.rotateRight().rotateRight());
        MapLocation bottom = center.add(dir.opposite());
        MapLocation left = center.add(dir.rotateLeft().rotateLeft());
        Direction leftDir = dir.rotateLeft();
        Direction rightDir = dir.rotateRight();
        Direction leftOppositeDir = leftDir.opposite();
        Direction rightOppositeDir = rightDir.opposite();
        MapLocation topLeft = center.add(leftDir).add(leftDir);
        MapLocation topRight = center.add(rightDir).add(rightDir);
        MapLocation bottomLeft = center.add(rightOppositeDir).add(rightOppositeDir);
        MapLocation bottomRight = center.add(leftOppositeDir).add(leftOppositeDir);
        if (rc.getLocation().distanceSquaredTo(center) <= 1) {
            if (rc.getLocation().distanceSquaredTo(center) == 0) {
                // SRP case
                top = center.add(Direction.NORTH);
                right = center.add(Direction.EAST);
                bottom = center.add(Direction.SOUTH);
                left = center.add(Direction.WEST);
                if (rc.senseRobotAtLocation(top) != null && rc.senseRobotAtLocation(top).getTeam() == myTeam) {
                    if (rc.canMove(Direction.SOUTH)) {
                        rc.move(Direction.SOUTH);
                    }
                } else if (rc.senseRobotAtLocation(right) != null && rc.senseRobotAtLocation(right).getTeam() == myTeam) {
                    if (rc.canMove(Direction.WEST)) {
                        rc.move(Direction.WEST);
                    }
                } else if (rc.senseRobotAtLocation(bottom) != null && rc.senseRobotAtLocation(bottom).getTeam() == myTeam) {
                    if (rc.canMove(Direction.NORTH)) {
                        rc.move(Direction.NORTH);
                    }
                } else if (rc.senseRobotAtLocation(left) != null && rc.senseRobotAtLocation(left).getTeam() == myTeam) {
                    if (rc.canMove(Direction.EAST)) {
                        rc.move(Direction.EAST);
                    }
                } else {
                    if (rc.canMove(Direction.NORTH)) {
                        rc.move(Direction.NORTH);
                    } else if (rc.canMove(Direction.EAST)) {
                        rc.move(Direction.EAST);
                    } else if (rc.canMove(Direction.SOUTH)) {
                        rc.move(Direction.SOUTH);
                    } else if (rc.canMove(Direction.WEST)) {
                        rc.move(Direction.WEST);
                    } else if (rc.canMove(Direction.NORTHWEST)) {
                        rc.move(Direction.NORTHWEST);
                    } else if (rc.canMove(Direction.NORTHEAST)) {
                        rc.move(Direction.NORTHEAST);
                    } else if (rc.canMove(Direction.SOUTHEAST)) {
                        rc.move(Direction.SOUTHEAST);
                    } else if (rc.canMove(Direction.SOUTHWEST)) {
                        rc.move(Direction.SOUTHWEST);
                    }
                }
            }
            boolean oppositeBuilder = false;
            RobotInfo opposite = rc.senseRobotAtLocation(center.add(dir));
            if (opposite != null && opposite.getType() == UnitType.SOLDIER && opposite.getTeam() == myTeam) {
                oppositeBuilder = true;
            }
            boolean topEmpty = rc.senseMapInfo(top).getPaint() == PaintType.EMPTY;
            boolean rightEmpty = rc.senseMapInfo(right).getPaint() == PaintType.EMPTY;
            boolean bottomEmpty = rc.senseMapInfo(bottom).getPaint() == PaintType.EMPTY;
            boolean leftEmpty = rc.senseMapInfo(left).getPaint() == PaintType.EMPTY;
            boolean topLeftEmpty = rc.senseMapInfo(topLeft).getPaint() == PaintType.EMPTY;
            boolean topRightEmpty = rc.senseMapInfo(topRight).getPaint() == PaintType.EMPTY;
            boolean bottomLeftEmpty = rc.senseMapInfo(bottomLeft).getPaint() == PaintType.EMPTY;
            boolean bottomRightEmpty = rc.senseMapInfo(bottomRight).getPaint() == PaintType.EMPTY;
            // paint all 4 sides first, then all corners, then whatever is left
            // assume rc is at bottom of ruin
            if (bottomEmpty && rc.canAttack(bottom)) {
                rc.attack(bottom, Util.useSecondaryGeneral(bottom, center, type, forTower));
            } else if (rightEmpty && rc.canAttack(right)) {
                rc.attack(right, Util.useSecondaryGeneral(right, center, type, forTower));
            } else if (topEmpty && rc.canAttack(top)) {
                rc.attack(top, Util.useSecondaryGeneral(top, center, type, forTower));
            } else if (leftEmpty && rc.canAttack(left)) {
                rc.attack(left, Util.useSecondaryGeneral(left, center, type, forTower));
            } else {
                if (bottomLeftEmpty) {
                    if (rc.canAttack(bottomLeft)) {
                        rc.attack(bottomLeft, Util.useSecondaryGeneral(bottomLeft, center, type, forTower));
                    }
                } else if (bottomRightEmpty) {
                    if (rc.canAttack(bottomRight)) {
                        rc.attack(bottomRight, Util.useSecondaryGeneral(bottomRight, center, type, forTower));
                    }
                } else if (topLeftEmpty && rc.canMove(leftDir) && !oppositeBuilder) {
                    rc.move(leftDir);
                    if (rc.canAttack(topLeft)) {
                        rc.attack(topLeft, Util.useSecondaryGeneral(topLeft, center, type, forTower));
                    }
                } else if (topRightEmpty && rc.canMove(rightDir) && !oppositeBuilder) {
                    rc.move(rightDir);
                    if (rc.canAttack(topRight)) {
                        rc.attack(topRight, Util.useSecondaryGeneral(topRight, center, type, forTower));
                    }
                } else if (!oppositeBuilder) {
                    if (topLeftEmpty) {
                        Navigator.moveTo(topLeft);
                    } else if (topRightEmpty) {
                        Navigator.moveTo(topRight);
                    }
                }
            }

            if (rc.isMovementReady() && !oppositeBuilder) {
                if (!topEmpty && !rightEmpty && !bottomEmpty && !leftEmpty) {
                    if (topLeftEmpty && rc.canMove(leftDir)) {
                        rc.move(leftDir);
                    } else if (topRightEmpty && rc.canMove(rightDir)) {
                        rc.move(rightDir);
                    } else {
                        if (rc.senseRobotAtLocation(left) != null && rc.senseRobotAtLocation(left).getTeam() == myTeam) {
                            if (rc.canMove(rightDir)) {
                                rc.move(rightDir);
                            }
                        } else if (rc.senseRobotAtLocation(right) != null && rc.senseRobotAtLocation(right).getTeam() == myTeam) {
                            if (rc.canMove(leftDir)) {
                                rc.move(leftDir);
                            }
                        } else if (topLeftEmpty) {
                            Navigator.moveTo(topLeft);
                        } else if (topRightEmpty) {
                            Navigator.moveTo(topRight);
                        }
                    }
                    if (!topLeftEmpty && !topRightEmpty && !bottomLeftEmpty && !bottomRightEmpty) {
                        if (rc.senseMapInfo(left).getPaint().isAlly() && rc.canMove(leftDir)) {
                            rc.move(leftDir);
                        } else if (rc.senseMapInfo(right).getPaint().isAlly() && rc.canMove(rightDir)) {
                            rc.move(rightDir);
                        }
                    }
                } else if (!rc.senseMapInfo(bottom).getPaint().isAlly()) {
                    if (rc.senseMapInfo(left).getPaint().isAlly() && rc.canMove(leftDir)) {
                        rc.move(leftDir);
                    } else if (rc.senseMapInfo(right).getPaint().isAlly() && rc.canMove(rightDir)) {
                        rc.move(rightDir);
                    }
                }
            }
        } else if (rc.getLocation().distanceSquaredTo(center) <= 5 && rc.isMovementReady()) {
            Direction bestDir = null;
            int minDist = 5;
            for (Direction d : Globals.adjacentDirections) {
                MapLocation loc = rc.getLocation().add(d);
                if (rc.canMove(d) && loc.distanceSquaredTo(center) < minDist) {
                    minDist = loc.distanceSquaredTo(center);
                    bestDir = d;
                }
            }
            if (bestDir != null) {
                rc.move(bestDir);
            }
        } else if (rc.isMovementReady()) {
            Navigator.moveTo(center);
        }

        if (rc.isActionReady()) {
            if (rc.canSenseLocation(top) && rc.senseMapInfo(top).getPaint() == PaintType.EMPTY) {
                if (rc.canAttack(top)) {
                    rc.attack(top, Util.useSecondaryGeneral(top, center, type, forTower));
                }
            } else if (rc.canSenseLocation(right) && rc.senseMapInfo(right).getPaint() == PaintType.EMPTY) {
                if (rc.canAttack(right)) {
                    rc.attack(right, Util.useSecondaryGeneral(right, center, type, forTower));
                }
            } else if (rc.canSenseLocation(bottom) && rc.senseMapInfo(bottom).getPaint() == PaintType.EMPTY) {
                if (rc.canAttack(bottom)) {
                    rc.attack(bottom, Util.useSecondaryGeneral(bottom, center, type, forTower));
                }
            } else if (rc.canSenseLocation(left) && rc.senseMapInfo(left).getPaint() == PaintType.EMPTY) {
                if (rc.canAttack(left)) {
                    rc.attack(left, Util.useSecondaryGeneral(left, center, type, forTower));
                }
            } else if (rc.canSenseLocation(topLeft) && rc.senseMapInfo(topLeft).getPaint() == PaintType.EMPTY) {
                if (rc.canAttack(topLeft)) {
                    rc.attack(topLeft, Util.useSecondaryGeneral(topLeft, center, type, forTower));
                }
            } else if (rc.canSenseLocation(topRight) && rc.senseMapInfo(topRight).getPaint() == PaintType.EMPTY) {
                if (rc.canAttack(topRight)) {
                    rc.attack(topRight, Util.useSecondaryGeneral(topRight, center, type, forTower));
                }
            } else if (rc.canSenseLocation(bottomLeft) && rc.senseMapInfo(bottomLeft).getPaint() == PaintType.EMPTY) {
                if (rc.canAttack(bottomLeft)) {
                    rc.attack(bottomLeft, Util.useSecondaryGeneral(bottomLeft, center, type, forTower));
                }
            } else if (rc.canSenseLocation(bottomRight) && rc.senseMapInfo(bottomRight).getPaint() == PaintType.EMPTY) {
                if (rc.canAttack(bottomRight)) {
                    rc.attack(bottomRight, Util.useSecondaryGeneral(bottomRight, center, type, forTower));
                }
            } else {
                MapInfo[] nearbyLocations = rc.senseNearbyMapInfos(center, 8);

                MapLocation bestPaintLocation = null;
                int closestDist = 9;

                for (MapInfo info : nearbyLocations) {
                    if (!info.getPaint().isEnemy() && (!info.getMapLocation().equals(center) || !forTower) && !(info.getPaint().isAlly() && info.getPaint().isSecondary() == Util.useSecondaryGeneral(info.getMapLocation(), center, type, forTower))) {
                        if (rc.getLocation().distanceSquaredTo(info.getMapLocation()) <= closestDist) {
                            closestDist = rc.getLocation().distanceSquaredTo(info.getMapLocation());
                            bestPaintLocation = info.getMapLocation();
                        }
                    }
                }

                if (bestPaintLocation != null && rc.canAttack(bestPaintLocation)) {
                    rc.attack(bestPaintLocation, Util.useSecondaryGeneral(bestPaintLocation, center, type, forTower));
                }
            }
        }
    }
}
