package playeralpha;
import battlecode.common.*;

public class Archon {
	
	RobotController rc;
	RobotType type = RobotType.ARCHON;
	float stride, body;
	float gardenerbody, plantDist;
	float safetyMultiplier = 1.2F;
	float plantMargin = 1F;
	MapLocation relativeSafety;
	MapLocation here;
	RobotInfo[] ri;
	Direction buildDir;
	
	public Archon(RobotController rc){
		this.rc = rc;
		stride = type.strideRadius;
		body = type.bodyRadius;
		gardenerbody = RobotType.GARDENER.bodyRadius;
		plantDist = gardenerbody+body+plantMargin;
	}
	
	public void go(){
		while(true){
			turn();
		}
	}
	
	public void turn(){
		try{
			here = rc.getLocation();
			ri = rc.senseNearbyRobots();
			if(rc.getRoundNum() == 1){
				firstTurn();
			}
			runHead();
			
			Clock.yield();
		}catch(Exception e){
			System.out.println("[ERROR] Turn could not happen");
		}
	}
	
	public void firstTurn(){
		MapLocation[] enemy;
		MapLocation[] ally;
		if(rc.getTeam()==Team.A){
			ally = rc.getInitialArchonLocations(Team.A);
			enemy = rc.getInitialArchonLocations(Team.B);
		}else{
			ally = rc.getInitialArchonLocations(Team.B);
			enemy = rc.getInitialArchonLocations(Team.A);
		}
		MapLocation enemyLocation = getCenter(enemy);
		MapLocation allyLocation = getCenter(ally);
		relativeSafety = enemyLocation.add(enemyLocation.directionTo(allyLocation), enemyLocation.distanceTo(allyLocation)*safetyMultiplier);
		System.out.println("rs "+relativeSafety);
		BroadcastSystem.sendEnemyLocation(rc, enemyLocation, 0, 0);
	}
	
	public MapLocation getCenter(MapLocation[] list){
		float totX = 0;
		float totY = 0;
		for(int i = 0; i<list.length; i++){
			totX += list[i].x;
			totY += list[i].y;
		}
		totX = totX/(float)list.length;
		totY = totY/(float)list.length;
		return new MapLocation(totX, totY);
	}
	
	public void runHead(){
		boolean isHead = BroadcastSystem.checkHead(rc);
		if(isHead){
			if(!BroadcastSystem.readSensed(rc)){
				BroadcastSystem.setScoutFormation(rc);
			}
			BroadcastSystem.resetScoutsCode(rc);
			BroadcastSystem.resetNumFarmers(rc);
			BroadcastSystem.resetSensed(rc);
			BroadcastSystem.resetGardenerDest(rc);
			BroadcastSystem.setRelativeSafety(rc, relativeSafety);
			//cashIn();
		}
		attemptBuild();
	}
	
	public void cashIn(){
		if(rc.getTeamBullets()>GameConstants.VICTORY_POINTS_TO_WIN * GameConstants.BULLET_EXCHANGE_RATE+1){
			try {
				rc.donate(GameConstants.VICTORY_POINTS_TO_WIN * GameConstants.BULLET_EXCHANGE_RATE + 1);
			} catch (GameActionException e) {
				System.out.println("[Error] Couldn't donate");
			}
		}
	}
	
	public void build(){
		
	}
	
	public void attemptBuild(){
		Direction buildDir = here.directionTo(relativeSafety);
		
		/*if(rc.getTeamBullets()<(float)rc.getRoundNum()/2){
			return;
		}*/
		Slice[] avoid = Slice.combine(detectObstacles(), detectTrees());
		
		for(int i = 0; i<avoid.length; i++){
			if(avoid[i].contains(buildDir)){
				if(Math.abs(avoid[i].open.radiansBetween(buildDir))<Math.abs(avoid[i].close.radiansBetween(buildDir))){
					buildDir = avoid[i].open;
				}else{
					buildDir = avoid[i].close;
				}
				break;
			}
		}
		
		if(rc.canBuildRobot(RobotType.GARDENER, buildDir)){
			try{
				rc.buildRobot(RobotType.GARDENER, buildDir);
			}catch(Exception e){
				System.out.println("Can't build gardener there");
			}
		}
	}
	
	public Slice[] detectObstacles(){
		Slice[] unavailable = null;
		for(int i = 0; i<ri.length; i++){
			if(here.distanceTo(ri[i].location) > ri[i].getRadius()+plantDist+gardenerbody){
				continue;
			}
			float half = (float) Math.asin((ri[i].getRadius()+gardenerbody)/here.distanceTo(ri[i].location));
			float middle = here.directionTo(ri[i].location).radians;
			Slice cone = new Slice(new Direction(middle+half), new Direction(middle-half));
			if(unavailable == null){
				unavailable = new Slice[1];
				unavailable[0] = cone;
			}else{
				Slice[] newSet = new Slice[unavailable.length+1];
				for(int j = 0; j<unavailable.length; j++){
					newSet[j+1] = unavailable[j];
				}
				newSet[0] = cone;
				unavailable = newSet;
			}
		}
		if(unavailable == null)unavailable = new Slice[0];
		
		for(int i = 0; i<unavailable.length; i++){
			for(int j = i+1; j<unavailable.length; j++){
				if(unavailable[i].add(unavailable[j])){
					unavailable = remove(unavailable, j);
					j = i;
				}
			}
		}
		return unavailable;
	}
	
	public Slice[] detectTrees(){
		Slice[] unavailable = null;
		TreeInfo[] ti = rc.senseNearbyTrees();
		for(int i = 0; i<ti.length; i++){
			if(here.distanceTo(ti[i].location) > ti[i].radius+plantDist+gardenerbody){
				continue;
			}
			float half = (float) Math.asin((ti[i].radius+gardenerbody)/here.distanceTo(ti[i].location));
			float middle = here.directionTo(ti[i].location).radians;
			Slice cone = new Slice(new Direction(middle+half), new Direction(middle-half));
			if(unavailable == null){
				unavailable = new Slice[1];
				unavailable[0] = cone;
			}else{
				Slice[] newSet = new Slice[unavailable.length+1];
				for(int j = 0; j<unavailable.length; j++){
					newSet[j+1] = unavailable[j];
				}
				newSet[0] = cone;
				unavailable = newSet;
			}
		}
		if(unavailable == null)unavailable = new Slice[0];
		
		for(int i = 0; i<unavailable.length; i++){
			for(int j = i+1; j<unavailable.length; j++){
				if(unavailable[i].add(unavailable[j])){
					unavailable = remove(unavailable, j);
					j = i;
				}
			}
		}
		return unavailable;
	}
	
	public Slice[] remove(Slice[] arg, int index){
		Slice[] newArg = new Slice[arg.length - 1];
		for(int i = 0; i<index; i++){
			newArg[i] = arg[i];
		}
		for(int i = index; i<newArg.length; i++){
			newArg[i] = arg[i+1];
		}
		return newArg;
	}
}
