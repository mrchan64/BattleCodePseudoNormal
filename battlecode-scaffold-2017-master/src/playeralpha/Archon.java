package playeralpha;
import battlecode.common.*;

public class Archon {
	
	RobotController rc;
	RobotType type = RobotType.ARCHON;
	float stride, body;
	float gardenerbody, plantDist;
	float safetyMultiplier = 1.2F;
	float safetyMultiplier2 = .8F;
	float plantMargin = 1F;
	float wallMargin = 1.233F;
	MapLocation relativeSafety;
	MapLocation here;
	RobotInfo[] ri;
	Direction buildDir;
	int numFarmers = 0;
	int numBuilders = 0;
	int numOnHold = 0;
	int numScouts = 0;
	
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
			}else{
				relativeSafety = BroadcastSystem.getRelativeSafety(rc);
			}
			runHead();
			attemptBuild();
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
		BroadcastSystem.setAllyLocation(rc, allyLocation);
		BroadcastSystem.sendEnemyLocation(rc, enemyLocation, 0, 0);
		BroadcastSystem.initWalls(rc);
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
			numScouts = BroadcastSystem.resetScoutsCode(rc);
			int[] arr = BroadcastSystem.checkFarmers(rc);
			numFarmers = arr[0];
			numOnHold = arr[1];
			BroadcastSystem.resetNumFarmers(rc);
			BroadcastSystem.resetSensed(rc);
			BroadcastSystem.resetGardenerDest(rc);
			relocateRelativeSafety();
			BroadcastSystem.setRelativeSafety(rc, relativeSafety);
			numBuilders = BroadcastSystem.numBuilders(rc);
			cashIn();
		}
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
	
	public void attemptBuild(){
		
		if(BroadcastSystem.allOrNothing(rc)) return;
		
		if(!rc.isBuildReady() || rc.getTeamBullets() < RobotType.GARDENER.bulletCost)return;
		
		if(numOnHold == RobotPlayer.MAX_ON_HOLD && numBuilders>numScouts + 2)return;
		
		Direction buildDir = here.directionTo(relativeSafety);
		Slice[] avoid = Slice.combine(detectObstacles(), detectTrees());
		
		for(int i = 0; i<avoid.length; i++){
			for(int j = i+1; j<avoid.length; j++){
				if(avoid[i].add(avoid[j])){
					avoid = remove(avoid, j);
					j = i;
				}
			}
		}
		avoid = detectWallDir(avoid);
		
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
		}else{
			//System.out.println("Can't build gardener there, calculation is off");
			//find walldir right here
		}
	}
	
	public Slice[] detectWallDir(Slice[] unavailable){
		Direction east = new Direction(0);
		Direction west = new Direction((float)Math.PI);
		Direction north = new Direction((float)Math.PI/2);
		Direction south = new Direction(-1*(float)Math.PI/2);
		
		for(int i = 0; i<unavailable.length; i++){
			if(east != null && unavailable[i].contains(east)){
				east = null;
			}
			if(west != null && unavailable[i].contains(west)){
				west = null;
			}
			if(north != null && unavailable[i].contains(north)){
				north = null;
			}
			if(south != null && unavailable[i].contains(south)){
				south = null;
			}
		}
		
		if(east != null && !rc.canBuildRobot(RobotType.GARDENER, east)){
			BroadcastSystem.setWall(rc, here.x+body, "EAST");
			Slice wall = new Slice(east.rotateLeftRads(wallMargin), east.rotateRightRads(wallMargin));
			Slice[] newSet = new Slice[unavailable.length+1];
			for(int i = 0; i<unavailable.length; i++){
				newSet[i+1] = unavailable[i];
			}
			newSet[0] = wall;
			unavailable = newSet;
		}else if(west != null && !rc.canBuildRobot(RobotType.GARDENER, west)){
			BroadcastSystem.setWall(rc, here.x-body, "WEST");
			Slice wall = new Slice(west.rotateLeftRads(wallMargin), west.rotateRightRads(wallMargin));
			Slice[] newSet = new Slice[unavailable.length+1];
			for(int i = 0; i<unavailable.length; i++){
				newSet[i+1] = unavailable[i];
			}
			newSet[0] = wall;
			unavailable = newSet;
		}else if(north != null && !rc.canBuildRobot(RobotType.GARDENER, north)){
			BroadcastSystem.setWall(rc, here.y+body, "NORTH");
			Slice wall = new Slice(north.rotateLeftRads(wallMargin), north.rotateRightRads(wallMargin));
			Slice[] newSet = new Slice[unavailable.length+1];
			for(int i = 0; i<unavailable.length; i++){
				newSet[i+1] = unavailable[i];
			}
			newSet[0] = wall;
			unavailable = newSet;
		}else if(south != null && !rc.canBuildRobot(RobotType.GARDENER, south)){
			BroadcastSystem.setWall(rc, here.y-body, "SOUTH");
			Slice wall = new Slice(south.rotateLeftRads(wallMargin), south.rotateRightRads(wallMargin));
			Slice[] newSet = new Slice[unavailable.length+1];
			for(int i = 0; i<unavailable.length; i++){
				newSet[i+1] = unavailable[i];
			}
			newSet[0] = wall;
			unavailable = newSet;
		}
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
	
	public void relocateRelativeSafety(){
		if(rc.getRoundNum() == 1)return;
		int[] walls = BroadcastSystem.getWalls(rc);
		if(relativeSafety.x>walls[0] || relativeSafety.x<walls[1] || relativeSafety.y>walls[2] || relativeSafety.y<walls[3]){
			MapLocation enemy[];
			MapLocation ally[];
			if(rc.getTeam()==Team.A){
				ally = rc.getInitialArchonLocations(Team.A);
				enemy = rc.getInitialArchonLocations(Team.B);
			}else{
				ally = rc.getInitialArchonLocations(Team.B);
				enemy = rc.getInitialArchonLocations(Team.A);
			}
			float[] pseudoWalls = getPseudoWalls(ally, enemy);
			MapLocation e = getCenter(enemy);
			MapLocation a = getCenter(ally);
			Direction theta = e.directionTo(a);
			MapLocation intersect = e.add(theta, e.distanceTo(a) * safetyMultiplier2);
			Direction newTheta = theta.rotateLeftRads((float)Math.PI/2);
			int region = (int)((theta.radians+Math.PI*2)/(Math.PI/2))%4;
			float yDiff;
			float xDiff;
			float ratio;
			float dist;
			switch(region){
			case 0:
				yDiff = pseudoWalls[2] - intersect.y;
				ratio = (float) Math.sin(theta.radians);
				dist = yDiff * ratio;
				relativeSafety = intersect.add(newTheta, dist);
				break;
			case 1:
				xDiff = intersect.x - pseudoWalls[1];
				ratio = (float) Math.sin(theta.radians-Math.PI/2);
				dist = xDiff * ratio;
				relativeSafety = intersect.add(newTheta, dist);
				break;
			case 2:
				yDiff = intersect.y - pseudoWalls[3];
				ratio = (float) Math.sin(theta.radians-Math.PI);
				dist = yDiff * ratio;
				relativeSafety = intersect.add(newTheta, dist);
				break;
			case 3:
				xDiff = pseudoWalls[0] - intersect.x;
				ratio = (float) Math.sin(theta.radians-Math.PI/2*3);
				dist = xDiff * ratio;
				relativeSafety = intersect.add(newTheta, dist);
				break;
			}
		}
	}
	
	public float[] getPseudoWalls(MapLocation[] ally, MapLocation[] enemy){
		float[] walls = {Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE};
		for(int i = 0; i<ally.length; i++){
			if(ally[i].x>walls[0]){
				walls[0] = ally[i].x;
			}else if(ally[i].x<walls[1]){
				walls[1] = ally[i].x;
			}
			if(ally[i].y>walls[2]){
				walls[2] = ally[i].y;
			}else if(ally[i].y<walls[3]){
				walls[3] = ally[i].y;
			}
		}
		for(int i = 0; i<enemy.length; i++){
			if(enemy[i].x>walls[0]){
				walls[0] = enemy[i].x;
			}else if(enemy[i].x<walls[1]){
				walls[1] = enemy[i].x;
			}
			if(enemy[i].y>walls[2]){
				walls[2] = enemy[i].y;
			}else if(enemy[i].y<walls[3]){
				walls[3] = enemy[i].y;
			}
		}
		walls[0] += body;
		walls[1] -= body;
		walls[2] += body;
		walls[3] -= body;
		return walls;
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
