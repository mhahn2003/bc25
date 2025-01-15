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
        ENEMY_TOWER,
        RUIN,
        ATTACK,
        ENEMY_UNIT,
        REQUEST_INITIALIZE,
    }

    public static int encodeMessage(InfoCategory info, MapLocation loc) {
        return (info.ordinal() << 12) + encodeLoc(loc);
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

    public static void initialize() throws GameActionException {
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
        int messageBytes = m.getBytes();
        decipherMessage(messageBytes / 65536);
        decipherMessage(messageBytes % 65536);
    }

    public static void readMessages() throws GameActionException {
        Message[] messages = rc.readMessages(rc.getRoundNum());
        for (Message m : messages) {
            splitMessage(m);
        }
    }

    public static void decipherMessage(int m) {
        int infoNum = m >> 12;
        InfoCategory info = InfoCategory.values()[infoNum];
        MapLocation loc = decodeLoc(m % 4096);
        switch (info) {
            case FRIEND_NON_PAINT_TOWER -> {
                for (int i = 0; i < friendlyNonPaintTowerLocations.length; i++) {
                    if (friendlyNonPaintTowerLocations[i] == null || friendlyNonPaintTowerLocations[i].equals(new MapLocation(-1, -1))) {
                        friendlyNonPaintTowerLocations[i] = loc;
                        break;
                    } else if (friendlyNonPaintTowerLocations[i].equals(loc)) {
                        break;
                    }
                }
                ruinLocations.add(loc);
            }
            case FRIEND_PAINT_TOWER -> {
                for (int i = 0; i < friendlyPaintTowerLocations.length; i++) {
                    if (friendlyPaintTowerLocations[i] == null || friendlyPaintTowerLocations[i].equals(new MapLocation(-1, -1))) {
                        friendlyPaintTowerLocations[i] = loc;
                        break;
                    } else if (friendlyPaintTowerLocations[i].equals(loc)) {
                        break;
                    }
                }
                ruinLocations.add(loc);
            }
            case ENEMY_TOWER -> {
                for (int i = 0; i < enemyTowerLocations.length; i++) {
                    if (enemyTowerLocations[i] == null || enemyTowerLocations[i].equals(new MapLocation(-1, -1))) {
                        enemyTowerLocations[i] = loc;
                        break;
                    } else if (enemyTowerLocations[i].equals(loc)) {
                        break;
                    }
                }
                ruinLocations.add(loc);
            }
            case RUIN -> {
                ruinLocations.add(loc);
                for (int i = 0; i < friendlyNonPaintTowerLocations.length; i++) {
                    if (friendlyNonPaintTowerLocations[i] == null) {
                        break;
                    } else if (friendlyNonPaintTowerLocations[i].equals(loc)) {
                        friendlyNonPaintTowerLocations[i] = new MapLocation(-1, -1);
                        break;
                    }
                }
                for (int i = 0; i < friendlyPaintTowerLocations.length; i++) {
                    if (friendlyPaintTowerLocations[i] == null) {
                        break;
                    } else if (friendlyPaintTowerLocations[i].equals(loc)) {
                        friendlyPaintTowerLocations[i] = new MapLocation(-1, -1);
                        break;
                    }
                }
                for (int i = 0; i < enemyTowerLocations.length; i++) {
                    if (enemyTowerLocations[i] == null) {
                        break;
                    } else if (enemyTowerLocations[i].equals(loc)) {
                        enemyTowerLocations[i] = new MapLocation(-1, -1);
                        break;
                    }
                }
            }
        }
    }

    public static void senseInfo() throws GameActionException {
        RobotInfo[] robots = rc.senseNearbyRobots();
        for (RobotInfo robot : robots) {
            if (robot.getTeam() == myTeam) {
                if (robot.getType().isTowerType()) {
                    MapLocation loc = robot.getLocation();
                    if (robot.getType().getBaseType() == UnitType.LEVEL_ONE_PAINT_TOWER) {
                        for (int i = 0; i < friendlyPaintTowerLocations.length; i++) {
                            if (friendlyPaintTowerLocations[i] == null || friendlyPaintTowerLocations[i].equals(new MapLocation(-1, -1))) {
                                friendlyPaintTowerLocations[i] = robot.getLocation();
                                addToMessageQueue(InfoCategory.FRIEND_PAINT_TOWER, robot.getLocation(), false);
                                if (!ruinLocations.contains(loc)) {
                                    ruinLocations.add(loc);
                                    addToMessageQueue(InfoCategory.RUIN, loc, false);
                                }
                                break;
                            } else if (friendlyPaintTowerLocations[i].equals(robot.getLocation())) {
                                break;
                            }
                        }
                    } else {
                        for (int i = 0; i < friendlyNonPaintTowerLocations.length; i++) {
                            if (friendlyNonPaintTowerLocations[i] == null || friendlyNonPaintTowerLocations[i].equals(new MapLocation(-1, -1))) {
                                friendlyNonPaintTowerLocations[i] = robot.getLocation();
                                addToMessageQueue(InfoCategory.FRIEND_NON_PAINT_TOWER, robot.getLocation(), false);
                                if (!ruinLocations.contains(loc)) {
                                    ruinLocations.add(loc);
                                    addToMessageQueue(InfoCategory.RUIN, loc, false);
                                }
                                break;
                            } else if (friendlyNonPaintTowerLocations[i].equals(robot.getLocation())) {
                                break;
                            }
                        }
                    }
                }
            } else {
                if (robot.getType().isTowerType()) {
                    for (int i = 0; i < enemyTowerLocations.length; i++) {
                        if (enemyTowerLocations[i] == null || enemyTowerLocations[i].equals(new MapLocation(-1, -1))) {
                            enemyTowerLocations[i] = robot.getLocation();
                            addToMessageQueue(InfoCategory.ENEMY_TOWER, robot.getLocation(), false);
                            break;
                        } else if (enemyTowerLocations[i].equals(robot.getLocation())) {
                            break;
                        }
                    }
                }
                int emptyIndex = -1;
                for (int i = 0; i < latestEnemyLocations.length; i++) {
                    if (emptyIndex == -1 && (latestEnemyLocations[i] == null || latestEnemyLocations[i].equals(new MapLocation(-1, -1)))) {
                        emptyIndex = i;
                    }
                    if (latestEnemyLocations[i] != null && latestEnemyLocations[i].distanceSquaredTo(robot.getLocation()) < enemyLocMinDist) {
                        break;
                    }
                }
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

        MapInfo[] mapInfos = rc.senseNearbyMapInfos();
        for (MapInfo mapInfo : mapInfos) {
            if (mapInfo.hasRuin()) {
                MapLocation loc = mapInfo.getMapLocation();
                if (!ruinLocations.contains(loc)) {
                    ruinLocations.add(loc);
                    addToMessageQueue(InfoCategory.RUIN, loc, false);
                }

                for (int i = 0; i < friendlyNonPaintTowerLocations.length; i++) {
                    if (friendlyNonPaintTowerLocations[i] == null) {
                        break;
                    } else if (friendlyNonPaintTowerLocations[i].equals(loc)) {
                        friendlyNonPaintTowerLocations[i] = new MapLocation(-1, -1);
                        break;
                    }
                }
                for (int i = 0; i < friendlyPaintTowerLocations.length; i++) {
                    if (friendlyPaintTowerLocations[i] == null) {
                        break;
                    } else if (friendlyPaintTowerLocations[i].equals(loc)) {
                        friendlyPaintTowerLocations[i] = new MapLocation(-1, -1);
                        break;
                    }
                }
                for (int i = 0; i < enemyTowerLocations.length; i++) {
                    if (enemyTowerLocations[i] == null) {
                        break;
                    } else if (enemyTowerLocations[i].equals(loc)) {
                        enemyTowerLocations[i] = new MapLocation(-1, -1);
                        break;
                    }
                }
            }
        }
    }
}
