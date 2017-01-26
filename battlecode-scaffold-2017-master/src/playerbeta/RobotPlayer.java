package playerbeta;

import battlecode.common.*;

public class RobotPlayer {
	
	@SuppressWarnings("unused")
	public static void run(RobotController rc) throws GameActionException {
		switch (rc.getType()) {
        case ARCHON:
        	Archon archon = new Archon(rc);
        	archon.go();
        	break;
        case GARDENER:
        	Gardener gardener = new Gardener(rc);
        	gardener.go();
        	break;
        case SCOUT:
        	Scout scout = new Scout(rc);
        	scout.go();
        	break;
        case LUMBERJACK:
        	Lumberjack lumberjack = new Lumberjack(rc);
        	lumberjack.go();
        	break;
        case SOLDIER:
        	Soldier soldier = new Soldier(rc);
        	soldier.go();
        	break;
        case TANK:
        	Tank tank = new Tank(rc);
        	tank.go();
        	break;
		}
	}
}
