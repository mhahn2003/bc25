package sprint;

import battlecode.common.*;

public class Movement extends Globals {
    public static void scatter() throws GameActionException {
        Direction bestDir = null;
        int minAdjacentAllies = 999999;
        for (Direction dir : Globals.allDirections) {
            if (rc.canMove(dir)) {
                MapLocation loc = rc.getLocation().add(dir);
                RobotInfo[] nearbyAllies = rc.senseNearbyRobots(loc, 2, myTeam);
                if (nearbyAllies.length < minAdjacentAllies) {
                    minAdjacentAllies = nearbyAllies.length;
                    bestDir = dir;
                }
            }
        }
        if (bestDir != null) {
            rc.move(bestDir);
        }
    }

    public static void wander() throws GameActionException {
        if (wandering) {
            if (wanderingCounter >= maxWanderingCounter) {
                wanderingCounter = 0;
                exploreLocationsVisited[wanderIndex] = true;
                wandering = false;
            } else {
                Navigator.moveTo(exploreLocations[wanderIndex]);
                wanderingCounter++;
            }
        }
        if (!wandering) {
            wandering = true;
            int wanderIndex = rc.getID() % 9;
            for (int i = 0; i < 9; i++) {
                if (exploreLocationsVisited[(wanderIndex + i) % 9]) {
                    continue;
                }
                MapLocation loc = Globals.exploreLocations[(wanderIndex + i) % 9];
                Globals.wanderIndex = (wanderIndex + i) % 9;
                Navigator.moveTo(loc);
                break;
            }
        }
    }

    public static void venture() throws GameActionException {
        boolean defenseTower = false;
        if (targetEnemyTowerLocation == null) {
            MapLocation closesetEnemyTowerLocation = null;
            int minDist = 999999;
            for (MapLocation loc : enemyNonDefenseTowerLocations) {
                if (loc == null) {
                    break;
                }
                if (!loc.equals(new MapLocation(-1, -1))) {
                    int dist = rc.getLocation().distanceSquaredTo(loc);
                    if (dist < minDist) {
                        minDist = dist;
                        closesetEnemyTowerLocation = loc;
                    }
                }
            }
            if (closesetEnemyTowerLocation == null) {
                for (MapLocation loc : enemyDefenseTowerLocations) {
                    if (loc == null) {
                        break;
                    }
                    if (!loc.equals(new MapLocation(-1, -1))) {
                        int dist = rc.getLocation().distanceSquaredTo(loc);
                        if (dist < minDist) {
                            minDist = dist;
                            closesetEnemyTowerLocation = loc;
                        }
                    }
                }
                if (closesetEnemyTowerLocation == null) {
                    adverse();
                }
                defenseTower = true;
            }
            targetEnemyTowerLocation = closesetEnemyTowerLocation;
        }

        if (targetEnemyTowerLocation != null) {
            if (defenseTower) {
                if (rc.getLocation().distanceSquaredTo(targetEnemyTowerLocation) >= 36) {
                    Navigator.moveTo(targetEnemyTowerLocation);
                } else {
                    aggressiveHold = true;
                    attackDefenseTower();
                }
                Navigator.moveTo(targetEnemyTowerLocation);
            } else {
                Navigator.moveTo(targetEnemyTowerLocation);
            }
        }
    }

    public static void adverse() throws GameActionException {
        RobotInfo[] robotInfos = rc.senseNearbyRobots(8);
        MapInfo[] mapInfos = rc.senseNearbyMapInfos(8);
        int[][] heuristic = new int[5][5];
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                heuristic[i][j] = 0;
            }
        }
        for (RobotInfo robotInfo : robotInfos) {
            MapLocation loc = robotInfo.location;
            int dx = loc.x - rc.getLocation().x;
            int dy = loc.y - rc.getLocation().y;
            heuristic[dx + 2][dy + 2] += heuristic(robotInfo.getTeam());
        }
        for (MapInfo mapInfo : mapInfos) {
            MapLocation loc = mapInfo.getMapLocation();
            int dx = loc.x - rc.getLocation().x;
            int dy = loc.y - rc.getLocation().y;
            heuristic[dx + 2][dy + 2] += heuristic(mapInfo.getPaint());
        }

        Direction bestDir = null;
        int maxHeuristic = -999999;
        for (Direction dir : Globals.allDirections) {
            if (rc.canMove(dir)) {
                MapLocation loc = rc.getLocation().add(dir);
                int dx = loc.x - rc.getLocation().x;
                int dy = loc.y - rc.getLocation().y;
                int heuristicSum = heuristic[dx + 1][dy + 1] + heuristic[dx + 2][dy + 1] + heuristic[dx + 3][dy + 1] + heuristic[dx + 1][dy + 2] + heuristic[dx + 2][dy + 2] + heuristic[dx + 3][dy + 2] + heuristic[dx + 1][dy + 3] + heuristic[dx + 2][dy + 3] + heuristic[dx + 3][dy + 3];
                if (heuristicSum > maxHeuristic) {
                    maxHeuristic = heuristicSum;
                    bestDir = dir;
                }
            }
        }
        if (bestDir != null) {
            rc.move(bestDir);
        }
    }

    public static int heuristic(Team team) {
        if (team == myTeam) {
            return -3;
        } else {
            return 5;
        }
    }

    public static int heuristic(PaintType paintType) {
        if (rc.getType() == UnitType.SOLDIER) {
            if (paintType.isAlly()) {
                return -3;
            } else if (paintType.isEnemy()) {
                return 1;
            } else {
                return 10;
            }
        } else if (rc.getType() == UnitType.MOPPER) {
            if (paintType.isAlly()) {
                return -3;
            } else if (paintType.isEnemy()) {
                return 10;
            } else {
                return -3;
            }
        } else if (rc.getType() == UnitType.SPLASHER) {
            if (paintType.isAlly()) {
                return -3;
            } else if (paintType.isEnemy()) {
                return 20;
            } else {
                return 5;
            }
        }
        return 0;
    }

    public static void attackDefenseTower() throws GameActionException {
        RobotInfo[] allies = rc.senseNearbyRobots(targetEnemyTowerLocation, 36, myTeam);
        if (allies.length >= 4) {
            Navigator.moveTo(targetEnemyTowerLocation);
            if (rc.canAttack(targetEnemyTowerLocation)) {
                rc.attack(targetEnemyTowerLocation);
            }
        } else {
            int curDist = rc.getLocation().distanceSquaredTo(targetEnemyTowerLocation);
            if (curDist <= 16) {
                Direction bestDir = null;
                int maxDist = 0;
                for (Direction dir : Globals.allDirections) {
                    if (rc.canMove(dir)) {
                        MapLocation loc = rc.getLocation().add(dir);
                        int dist = loc.distanceSquaredTo(targetEnemyTowerLocation);
                        if (dist > maxDist) {
                            maxDist = dist;
                            bestDir = dir;
                        }
                    }
                }
                if (bestDir != null && maxDist > curDist) {
                    rc.move(bestDir);
                }
            } else if (curDist > 20) {
                Direction bestDir = null;
                int minDist = 999999;
                for (Direction dir : Globals.allDirections) {
                    if (rc.canMove(dir)) {
                        MapLocation loc = rc.getLocation().add(dir);
                        int dist = loc.distanceSquaredTo(targetEnemyTowerLocation);
                        if (dist > 16 && dist < minDist) {
                            minDist = dist;
                            bestDir = dir;
                        }
                    }
                }
                if (bestDir != null) {
                    rc.move(bestDir);
                }
            }
        }
    }
}
