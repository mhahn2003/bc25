package finals;

import battlecode.common.*;

// location can be stored with 12 bits, 4 bits for classifications to store two locations per one message
public class Comms extends Globals {

    static int[] messageQueue = new int[40];
    static int[] initializeMessageQueue = new int[40];

    static boolean initializing = false;
    static int initializingUnitId = -1;

    public enum InfoCategory {
        EMPTY,
        FRIEND_TOWER,
        ENEMY_TOWER,
        RUIN,
        ENEMY_UNIT,
        EXPLORE_LOC_VISITED,
        UPGRADE,
        FLICKER,
    }

    public static int encodeMessage(InfoCategory info, MapLocation loc) {
        int infoNum = 0;
        switch (info) {
            case EMPTY -> infoNum = 0;
            case FRIEND_TOWER -> infoNum = 1;
            case ENEMY_TOWER -> infoNum = 2;
            case RUIN -> infoNum = 3;
            case ENEMY_UNIT -> infoNum = 4;
            case EXPLORE_LOC_VISITED -> infoNum = 5;
            case UPGRADE -> infoNum = 6;
            case FLICKER -> infoNum = 7;
        }
        return (infoNum * 4096) + encodeLoc(loc);
    }

    public static void addToMessageQueue(InfoCategory info, MapLocation loc, boolean initialize) {
        int message = encodeMessage(info, loc);
        if (initialize) {
            for (int i = 0; i < initializeMessageQueue.length; i++) {
                if (initializeMessageQueue[i] == 0) {
                    initializeMessageQueue[i] = message;
                    break;
                }
            }
        } else {
            for (int i = 0; i < messageQueue.length; i++) {
                if (messageQueue[i] == 0) {
                    messageQueue[i] = message;
                    break;
                }
            }
        }
    }

    public static void pushInitializeQueue() throws GameActionException {
        if (!initializing || initializingUnitId == -1) {
            return;
        }
        MapLocation sendTo;
        if (rc.canSenseRobot(initializingUnitId)) {
            RobotInfo robot = rc.senseRobot(initializingUnitId);
            sendTo = robot.getLocation();
        } else {
            initializing = false;
            return;
        }
        int messageIndex1 = -1;
        for (int i = 0; i < initializeMessageQueue.length; i++) {
            if (initializeMessageQueue[i] != 0) {
                messageIndex1 = i;
                break;
            }
        }
        int messageIndex2 = messageIndex1 - 1;

        while (messageIndex1 >= 0) {
            int message1 = initializeMessageQueue[messageIndex1];
            int message2 = messageIndex2 < 0 ? 0 : initializeMessageQueue[messageIndex2];
            int message = combineMessage(message1, message2);
            if (rc.canSendMessage(sendTo, message)) {
                rc.sendMessage(sendTo, message);
                initializeMessageQueue[messageIndex1] = 0;
                if (messageIndex2 >= 0) {
                    initializeMessageQueue[messageIndex2] = 0;
                }
                messageIndex1 -= 2;
                messageIndex2 -= 2;
            } else {
                break;
            }
        }
        if (messageIndex1 < 0) {
            initializing = false;
        }
    }

    public static boolean sendMessages(MapLocation sendTo, boolean broadcast) throws GameActionException {
        if (!broadcast && !rc.canSendMessage(sendTo)) {
            return false;
        }
        int message1 = 0;
        int message2 = 0;
        for (int i = messageQueue.length - 1; i >= 0; i--) {
            if (messageQueue[i] != 0) {
                if (message1 == 0) {
                    message1 = messageQueue[i];
                    messageQueue[i] = 0;
                } else if (message2 == 0) {
                    message2 = messageQueue[i];
                    messageQueue[i] = 0;
                } else {
                    break;
                }
            }
        }
        if (message1 != 0) {
            int message = combineMessage(message1, message2);
            if (broadcast) {
                if (rc.canBroadcastMessage()) {
                    rc.broadcastMessage(message);
                    return true;
                }
            } else if (rc.canSendMessage(sendTo, message)) {
                rc.sendMessage(sendTo, message);
                return true;
            }
        }
        return false;
    }

    public static int encodeLoc(MapLocation loc) {
        return (loc.x << 6) + loc.y;
    }

    public static MapLocation decodeLoc(int loc) {
        return new MapLocation(loc / 64, loc % 64);
    }

    public static int combineMessage(int m1, int m2){
        return (m1 << 16) + m2;
    }

    public static void splitMessage(Message m) throws GameActionException {
        long messageBytes = m.getBytes();
        if (messageBytes < 0) {
            messageBytes += 4294967296L;
        }
        int message1 = (int) (messageBytes / 65536);
        int message2 = (int) (messageBytes % 65536);
        decipherMessage(message1);
        decipherMessage(message2);
    }

    public static void readMessages() throws GameActionException {
        Message[] messages = rc.readMessages(rc.getRoundNum()-1);
        for (Message m : messages) {
            splitMessage(m);
        }
        messages = rc.readMessages(rc.getRoundNum());
        for (Message m : messages) {
            splitMessage(m);
        }
    }

    public static void decipherMessage(int m) {
        int infoNum = m / 4096;
        InfoCategory info = InfoCategory.values()[infoNum];
        MapLocation loc = decodeLoc(m % 4096);
        switch (info) {
            case FRIEND_TOWER -> {
                friendlyTowerLocations.add(loc);
                ruinLocations.add(loc);
            }
            case ENEMY_TOWER -> {
                enemyTowerLocations.add(loc);
                ruinLocations.add(loc);
            }
            case RUIN -> ruinLocations.add(loc);
            case ENEMY_UNIT -> {
                int emptyIndex = -1;
                boolean locationTooClose = false;
                for (int i = 0; i < latestEnemyLocations.length; i++) {
                    if (emptyIndex == -1 && (latestEnemyLocations[i] == null || latestEnemyLocations[i].equals(new MapLocation(-1, -1)))) {
                        emptyIndex = i;
                    }
                    if (latestEnemyLocations[i] != null && latestEnemyLocations[i].distanceSquaredTo(loc) < enemyLocMinDist) {
                        locationTooClose = true;
                        break;
                    }
                }
                if (!locationTooClose) {
                    if (emptyIndex != -1) {
                        latestEnemyLocations[emptyIndex] = loc;
                    } else {
                        latestEnemyLocations[latestEnemyLocationIndex] = loc;
                        latestEnemyLocationIndex = (latestEnemyLocationIndex + 1) % latestEnemyLocations.length;
                    }
                }
            }
            case EXPLORE_LOC_VISITED -> {
                for (int i = 0; i < exploreLocations.length; i++) {
                    if (exploreLocations[i].equals(loc)) {
                        exploreLocationsVisited[i] = true;
                        break;
                    }
                }
            }
            case UPGRADE -> upgradeTowerLocation = loc;
            case FLICKER -> {
                Logger.log("receive flicker: " + loc);
                if (currentFlickerTowerLocation == null) currentFlickerTowerLocation = loc;
                noFlickerCounter = 0;
            }
        }
    }

    public static void senseInfo() throws GameActionException {
        RobotInfo[] robots = rc.senseNearbyRobots();
        for (RobotInfo robot : robots) {
            if (robot.getTeam() == myTeam) {
                if (robot.getType().isTowerType()) {
                    MapLocation loc = robot.getLocation();
                    if (!friendlyTowerLocations.contains(loc)) {
                        friendlyTowerLocations.add(loc);
                        addToMessageQueue(InfoCategory.FRIEND_TOWER, loc, false);
                    }
                    if (!ruinLocations.contains(loc)) {
                        ruinLocations.add(loc);
                        addToMessageQueue(InfoCategory.RUIN, loc, false);
                    }
                    if (robot.getType() == UnitType.LEVEL_ONE_MONEY_TOWER && robot.getID() % 3 == 0) {
                        flickerTowerLocations.add(loc);
                    }
                    if (robot.getType().getBaseType() == UnitType.LEVEL_ONE_DEFENSE_TOWER) {
                        flickerTowerLocations.add(loc);
                    }
                }
            } else {
                if (robot.getType().isTowerType()) {
                    if (!enemyTowerLocations.contains(robot.getLocation())) {
                        enemyTowerLocations.add(robot.getLocation());
                        addToMessageQueue(InfoCategory.ENEMY_TOWER, robot.getLocation(), false);
                    }
                    if (!ruinLocations.contains(robot.getLocation())) {
                        ruinLocations.add(robot.getLocation());
                        addToMessageQueue(InfoCategory.RUIN, robot.getLocation(), false);
                    }
                }
                int emptyIndex = -1;
                boolean locationTooClose = false;
                for (int i = 0; i < latestEnemyLocations.length; i++) {
                    if (emptyIndex == -1 && (latestEnemyLocations[i] == null || latestEnemyLocations[i].equals(new MapLocation(-1, -1)))) {
                        emptyIndex = i;
                    }
                    if (latestEnemyLocations[i] != null && latestEnemyLocations[i].distanceSquaredTo(robot.getLocation()) < enemyLocMinDist) {
                        locationTooClose = true;
                        break;
                    }
                }
                if (!locationTooClose) {
                    if (emptyIndex != -1) {
                        latestEnemyLocations[emptyIndex] = robot.getLocation();
                        addToMessageQueue(InfoCategory.ENEMY_UNIT, robot.getLocation(), false);
                    } else {
                        latestEnemyLocations[latestEnemyLocationIndex] = robot.getLocation();
                        addToMessageQueue(InfoCategory.ENEMY_UNIT, robot.getLocation(), false);
                        latestEnemyLocationIndex = (latestEnemyLocationIndex + 1) % latestEnemyLocations.length;
                    }
                }
            }
        }

        MapLocation[] ruins = rc.senseNearbyRuins(-1);
        for (MapLocation loc : ruins) {
            if (!ruinLocations.contains(loc)) {
                ruinLocations.add(loc);
                addToMessageQueue(InfoCategory.RUIN, loc, false);
            }
            if (rc.canSenseRobotAtLocation(loc) && rc.senseRobotAtLocation(loc) == null) {
                if (friendlyTowerLocations.contains(loc)) {
                    friendlyTowerLocations.remove(loc);
                }
                if (enemyTowerLocations.contains(loc)) {
                    enemyTowerLocations.remove(loc);
                }
                if (flickerTowerLocations.contains(loc)) {
                    flickerTowerLocations.remove(loc);
                }
            }
        }

        for (int i = 0; i < exploreLocations.length; i++) {
            if (!exploreLocationsVisited[i] && rc.canSenseLocation(exploreLocations[i])) {
                exploreLocationsVisited[i] = true;
                addToMessageQueue(InfoCategory.EXPLORE_LOC_VISITED, exploreLocations[i], false);
                break;
            }
        }
        for (int i = 0; i < symmetryLocations.length; i++) {
            if (symmetryLocationsVisited[i]) {
                continue;
            }
            if (rc.canSenseLocation(symmetryLocations[i])) {
                symmetryLocationsVisited[i] = true;
            }
        }

        if (closestDefenseTower != null) {
            if (rc.canSenseLocation(closestDefenseTower)) {
                RobotInfo robot = rc.senseRobotAtLocation(closestDefenseTower);
                if (robot == null || robot.getTeam() == myTeam) {
                    closestDefenseTower = null;
                }
            }
        }

        if (closestEnemyTower != null) {
            if (rc.canSenseLocation(closestEnemyTower)) {
                RobotInfo robot = rc.senseRobotAtLocation(closestEnemyTower);
                if (robot == null || robot.getTeam() != myTeam) {
                    closestEnemyTower = null;
                }
            }
        }

        if (rc.getType() == UnitType.MOPPER) {
            noNearbyAllyPaint = true;
            for (Direction dir : Direction.allDirections()) {
                MapLocation loc = rc.getLocation().add(dir);
                if (rc.canSenseLocation(loc) && rc.senseMapInfo(loc).getPaint().isAlly()) {
                    noNearbyAllyPaint = false;
                    break;
                }
            }
        }
    }

    public static void sendMessagesToTower() throws GameActionException {
        MapLocation[] ruins = rc.senseNearbyRuins(-1);
        for (MapLocation ruin : ruins) {
            if (rc.canSenseRobotAtLocation(ruin)) {
                RobotInfo robot = rc.senseRobotAtLocation(ruin);
                if (robot.getTeam() == myTeam && sendMessages(robot.getLocation(), false)) {
                    break;
                }
            }
        }
    }
}
