package playerbeta;

import battlecode.common.*;

public class RobotPlayer {
	public static RobotController rc;
	public static final int ROUND_ONLY_FARM = 20;
	public static final int NUM_BULLETS_BUILD = 50;
	public static final int MAX_MIGRATED = 10;
	public static final int MAX_ON_HOLD = 1;
	public static final int LIMIT_GARDENER_TURN = 100;
	
	@SuppressWarnings("unused")
	public static void run(RobotController rc) throws GameActionException {
		RobotPlayer.rc = rc;
		switch (rc.getType()) {
        case ARCHON:
        	Archon archon = new Archon(rc);
        	archon.go();
        	break;
        case GARDENER:
        	//createGardener(rc);
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
        case SOLDIER:
        	Soldier soldier = new Soldier(rc);
        	soldier.go();
		}
	}
}
