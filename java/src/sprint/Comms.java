package sprint;

import battlecode.common.*;

// location can be stored with 12 bits, 4 bits for classifications to store two locations per one message
public class Comms extends Globals {

    static int[] messageQueue = new int[40];
    static int[] initializeMessageQueue = new int[40];

    static boolean initializing = false;
    static int initializingUnitId = -1;

    public enum InfoCategory {
        EMPTY,
        FRIEND_NON_PAINT_TOWER,
        FRIEND_PAINT_TOWER,
        ENEMY_NON_DEFENSE_TOWER,
        ENEMY_DEFENSE_TOWER,
        RUIN,
        ENEMY_UNIT,
        EXPLORE_LOC_VISITED,
        UPGRADE,
    }

    public static int encodeMessage(InfoCategory info, MapLocation loc) {
        int infoNum = 0;
        switch (info) {
            case EMPTY -> infoNum = 0;
            case FRIEND_NON_PAINT_TOWER -> infoNum = 1;
            case FRIEND_PAINT_TOWER -> infoNum = 2;
            case ENEMY_NON_DEFENSE_TOWER -> infoNum = 3;
            case ENEMY_DEFENSE_TOWER -> infoNum = 4;
            case RUIN -> infoNum = 5;
            case ENEMY_UNIT -> infoNum = 6;
            case EXPLORE_LOC_VISITED -> infoNum = 7;
            case UPGRADE -> infoNum = 8;
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
        Logger.log("Message: " + messageBytes);
        int message1 = (int) (messageBytes / 65536);
        int message2 = (int) (messageBytes % 65536);
        decipherMessage(message1);
        decipherMessage(message2);
    }

    public static void readMessages() throws GameActionException {
        Message[] messages = rc.readMessages(rc.getRoundNum());
        for (Message m : messages) {
            splitMessage(m);
        }
    }

    public static void decipherMessage(int m) {
        int infoNum = m / 4096;
        InfoCategory info = InfoCategory.values()[infoNum];
        MapLocation loc = decodeLoc(m % 4096);
        switch (info) {
            case FRIEND_NON_PAINT_TOWER -> {
                friendlyNonPaintTowerLocations.add(loc);
                ruinLocations.add(loc);
            }
            case FRIEND_PAINT_TOWER -> {
                friendlyPaintTowerLocations.add(loc);
                ruinLocations.add(loc);
            }
            case ENEMY_NON_DEFENSE_TOWER -> {
                enemyNonDefenseTowerLocations.add(loc);
                ruinLocations.add(loc);
            }
            case ENEMY_DEFENSE_TOWER -> {
                enemyDefenseTowerLocations.add(loc);
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
        }
    }

    public static void senseInfo() throws GameActionException {
        RobotInfo[] robots = rc.senseNearbyRobots();
        for (RobotInfo robot : robots) {
            if (robot.getTeam() == myTeam) {
                if (robot.getType().isTowerType()) {
                    MapLocation loc = robot.getLocation();
                    if (robot.getType().getBaseType() == UnitType.LEVEL_ONE_PAINT_TOWER) {
                        if (!friendlyPaintTowerLocations.contains(loc)) {
                            friendlyPaintTowerLocations.add(loc);
                            addToMessageQueue(InfoCategory.FRIEND_PAINT_TOWER, loc, false);
                        }
                        if (!ruinLocations.contains(loc)) {
                            ruinLocations.add(loc);
                            addToMessageQueue(InfoCategory.RUIN, loc, false);
                        }
                    } else {
                        if (!friendlyNonPaintTowerLocations.contains(loc)) {
                            friendlyNonPaintTowerLocations.add(loc);
                            addToMessageQueue(InfoCategory.FRIEND_NON_PAINT_TOWER, loc, false);
                        }
                        if (!ruinLocations.contains(loc)) {
                            ruinLocations.add(loc);
                            addToMessageQueue(InfoCategory.RUIN, loc, false);
                        }
                    }
                }
            } else {
                if (robot.getType().isTowerType()) {
                    if (robot.getType().getBaseType() == UnitType.LEVEL_ONE_DEFENSE_TOWER) {
                        if (!enemyDefenseTowerLocations.contains(robot.getLocation())) {
                            enemyDefenseTowerLocations.add(robot.getLocation());
                            addToMessageQueue(InfoCategory.ENEMY_DEFENSE_TOWER, robot.getLocation(), false);
                        }
                        if (!ruinLocations.contains(robot.getLocation())) {
                            ruinLocations.add(robot.getLocation());
                            addToMessageQueue(InfoCategory.RUIN, robot.getLocation(), false);
                        }
                    } else {
                        if (!enemyNonDefenseTowerLocations.contains(robot.getLocation())) {
                            enemyNonDefenseTowerLocations.add(robot.getLocation());
                            addToMessageQueue(InfoCategory.ENEMY_NON_DEFENSE_TOWER, robot.getLocation(), false);
                        }
                        if (!ruinLocations.contains(robot.getLocation())) {
                            ruinLocations.add(robot.getLocation());
                            addToMessageQueue(InfoCategory.RUIN, robot.getLocation(), false);
                        }
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
                if (friendlyNonPaintTowerLocations.contains(loc)) {
                    friendlyNonPaintTowerLocations.remove(loc);
                }
                if (friendlyPaintTowerLocations.contains(loc)) {
                    friendlyPaintTowerLocations.remove(loc);
                }
                if (enemyNonDefenseTowerLocations.contains(loc)) {
                    enemyNonDefenseTowerLocations.remove(loc);
                }
                if (enemyDefenseTowerLocations.contains(loc)) {
                    enemyDefenseTowerLocations.remove(loc);
                }
                if (targetEnemyTowerLocation != null && targetEnemyTowerLocation.equals(loc)) {
                    targetEnemyTowerLocation = null;
                }
            }
        }

        if (!explored) {
            for (int i = 0; i < exploreLocations.length; i++) {
                if (!exploreLocationsVisited[i] && rc.canSenseLocation(exploreLocations[i])) {
                    exploreLocationsVisited[i] = true;
                    wandering = false;
                    wanderingCounter = 0;
                    addToMessageQueue(InfoCategory.EXPLORE_LOC_VISITED, exploreLocations[i], false);
                    boolean allVisited = true;
                    for (int j = 0; j < exploreLocations.length; j++) {
                        if (!exploreLocationsVisited[j]) {
                            allVisited = false;
                            break;
                        }
                    }
                    if (allVisited) {
                        explored = true;
                    }
                    break;
                }
            }
        }
        if (rushSoldier) {
            for (int i = 0; i < symmetryLocations.length; i++) {
                if (symmetryLocationsVisited[i]) {
                    continue;
                }
                if (rc.canSenseLocation(symmetryLocations[i])) {
                    symmetryLocationsVisited[i] = true;
                    symmetryBroken[i] = true;
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
