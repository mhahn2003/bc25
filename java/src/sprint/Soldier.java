package sprint;

import battlecode.common.GameActionException;
import battlecode.common.RobotInfo;

public class Soldier extends Unit {

    // TODO: make an evade function that avoids tower range, especially defense towers
    // If we simply just move back, might get stuck in an infinite loop

    // Currently this might be false but unit still might be within tower range, i.e. low paint
    public static boolean aggressiveMode = false;

    public void act() throws GameActionException {
        super.act();
        attack();
//        build();
//        paintSRP();
        move();
    }

    public void attack() throws GameActionException {
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, opponentTeam);
        for (RobotInfo robot : enemyRobots) {
            if (robot.getType().isTowerType()) {
                if (rc.canAttack(robot.location)) {
                    rc.attack(robot.location);
                    aggressiveMode = true;
                    return;
                }
            }
        }
        aggressiveMode = false;
    }

    public void move() throws GameActionException {

    }
}
