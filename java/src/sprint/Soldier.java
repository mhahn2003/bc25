package sprint;

import battlecode.common.GameActionException;

public class Soldier extends Unit {
    public void act() throws GameActionException {
        super.act();
        attack();

    }
}
