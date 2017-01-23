package playerbeta;

import battlecode.common.*;

public class RobotPlayer {
	public static RobotController rc;
	public static final int ROUND_ONLY_FARM = 20;
	public static final int NUM_BULLETS_BUILD = 50;
	public static final int MAX_MIGRATED = 10;
	public static final int MAX_ON_HOLD = 1;
	
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
		}
	}
	
	public static void createGardener(RobotController rc){
		if(rc.getRoundNum()>ROUND_ONLY_FARM){
			int[] arr = BroadcastSystem.checkFarmers(rc);
			int migrated = arr[0];
			int onHold = arr[1];
			if(/*rc.getTeamBullets()<=NUM_BULLETS_BUILD &&*/ migrated<=MAX_MIGRATED && onHold<=MAX_ON_HOLD){
				Gardener_Farming gf = new Gardener_Farming(rc);
				gf.go();
			}else{
				System.out.println("buildGardenerCreated");
				Gardener_Building gb = new Gardener_Building(rc);
				gb.go();
			}
		}else{
			Gardener_Farming gf = new Gardener_Farming(rc);
			gf.go();
		}
	}
}
