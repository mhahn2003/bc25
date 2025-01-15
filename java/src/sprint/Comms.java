package sprint;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Message;

// location can be stored with 12 bits, 4 bits for classifications to store two locations per one message
public class Comms extends Globals {

    static int[] messageQueue = new int[40];

    public enum InfoCategory {
        FRIEND_NON_PAINT_TOWER,
        FRIEND_PAINT_TOWER,
        ENEMY_TOWER,
        RUIN,
        ATTACK,
        EMPTY,
    }

    public static int encodeMessage(InfoCategory info, MapLocation loc) {
        return (info.ordinal() << 12) + encodeLoc(loc);
    }

    public static void sendMessage(InfoCategory info, MapLocation loc, MapLocation sendTo) throws GameActionException {
        int message = encodeMessage(info, loc);

    }

    public static int encodeLoc(MapLocation loc) {
        return (loc.x << 6) + loc.y;
    }

    public static MapLocation decodeLoc(int loc) {
        return new MapLocation(loc / 64, loc % 64);
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

    public static void decipherMessage(int m) throws GameActionException {
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
}
