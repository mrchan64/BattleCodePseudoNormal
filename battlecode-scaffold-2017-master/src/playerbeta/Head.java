package playerbeta;

import battlecode.common.*;

public class Head {
	
	public RobotController rc;
	int numBuilders = 0;
	int numScouts = 0;
	
	public Head(RobotController rc){
		this.rc = rc;
		BroadcastSystem.initHead(rc);
	}
	
	public void runHead(){
		boolean isHead = BroadcastSystem.checkHead(rc);
		if(isHead){
			if(!BroadcastSystem.readSensed(rc)){
				BroadcastSystem.setScoutFormation(rc);
			}
			numScouts = BroadcastSystem.resetScoutsCode(rc);
			BroadcastSystem.resetUnitCount(rc);
			BroadcastSystem.resetSensed(rc);
			BroadcastSystem.resetGardenerDest(rc);
			numBuilders = BroadcastSystem.numBuilders(rc);
			cashIn();
		}
	}
	
	public void cashIn(){
		if(rc.getTeamBullets()>1000){
			try {
				rc.donate(rc.getTeamBullets()-1000);
			} catch (GameActionException e) {
				System.out.println("[Error] Couldn't donate");
			}
		}
	}
	
}
