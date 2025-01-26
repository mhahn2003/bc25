package finals;

import battlecode.common.Clock;
import battlecode.common.GameConstants;
import battlecode.common.RobotController;

public class RobotPlayer extends Globals {
    public static void run(RobotController robotController) {
        Globals.init(robotController);

        while (true) {
            act();
            Clock.yield();
        }
    }

    private static void act() {
        int startRound = rc.getRoundNum();
        int startBytecodes = Clock.getBytecodeNum();

        try {
            r.act();
            Logger.flush();
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
        }

        int endRound = rc.getRoundNum();
        int endBytecodes = Clock.getBytecodeNum();

        int bytecodeLimit = rc.getType().isRobotType() ? GameConstants.ROBOT_BYTECODE_LIMIT : GameConstants.TOWER_BYTECODE_LIMIT;

        int usedBytecodes = startRound == endRound
                ? endBytecodes - startBytecodes
                : (bytecodeLimit - startBytecodes) + Math.max(0, endRound - startRound - 1) * bytecodeLimit + endBytecodes;

        double bytecodePercentage = (double) usedBytecodes / (double) bytecodeLimit * 100.0;

        if (startRound != endRound) {
            System.out.println(rc.getLocation() + " Bytecode overflow: " + usedBytecodes + " (" + bytecodePercentage + "%)");
        } else if (bytecodePercentage > 95) {
            System.out.println(rc.getLocation() + " High bytecode usage: " + usedBytecodes + " (" + bytecodePercentage + "%)");
        }
    }
}
