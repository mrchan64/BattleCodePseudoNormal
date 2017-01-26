package playerbeta;
import java.util.Arrays;

import battlecode.common.*;

public class Gardener {
	
	RobotController rc;
	Head head;
	RobotType type = RobotType.GARDENER;
	float stride, body;
	int numLumberjacks, numSoldiers;
	TreeInfo[] ti;
	RobotInfo[] ri;
	Direction general;
	Direction moving;
	MapLocation here;
	MapLocation enemyLocation;
	
	boolean migrating = true;
	MapLocation allyLocation;
	Direction[] buildDir = null;
	RobotType lastBuild = null;
	int buildPhase = 0;
	int treesBuilt = 0;
	int scoutsBuilt = 4;
	int treeMultiplier = 1;
	
	MapLocation[] previousLoc;
	
	public Gardener(RobotController rc){
		this.rc = rc;
		head = new Head(rc);
		stride = type.strideRadius;
		body = type.bodyRadius;
		previousLoc = new MapLocation[PlayerConstants.PREVIOUS_LOC_NUM];
	}
	
	public void go(){
		firstTurn();
		while(true){
			turn();
		}
	}
	
	public void firstTurn(){
		setGeneral();
	}
	
	public void turn(){
		try{
			here = rc.getLocation();
			ti = rc.senseNearbyTrees();
			ri = rc.senseNearbyRobots();
			numLumberjacks = BroadcastSystem.checkLumberjackCount(rc);
			numSoldiers = BroadcastSystem.checkSoldier(rc);
			int[] arr = BroadcastSystem.readEnemyLocation(rc);
			enemyLocation = new MapLocation(arr[0], arr[1]);
			head.runHead();
			BroadcastSystem.checkNumFarmers(rc, migrating || treesBuilt < 2);
			if(migrating){
				if(checkStuck()){
					migrating = false;
				}else{
					findSettle();
				}
			}else{
				initDirs();
				buildTurn();
			}
			Clock.yield();
		}catch(Exception e){
			System.out.println("[ERROR] Turn could not happen");
			e.printStackTrace();
		}
	}
	
	public void setGeneral(){
		MapLocation[] enemy;
		MapLocation[] ally;
		if(rc.getTeam()==Team.A){
			ally = rc.getInitialArchonLocations(Team.A);
			enemy = rc.getInitialArchonLocations(Team.B);
		}else{
			ally = rc.getInitialArchonLocations(Team.B);
			enemy = rc.getInitialArchonLocations(Team.A);
		}
		allyLocation = getCenter(ally);
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
	
	public boolean detectNeutralTree(){
		TreeInfo closest = null;
		float distance = Float.MAX_VALUE;
		for(int i = 0; i<ti.length; i++){
			if(ti[i].getTeam() == Team.NEUTRAL){
				float tempd = analyzeTree(ti[i]);
				if(tempd<distance){
					closest = ti[i];
					distance = tempd;
				}
			}
		}
		//Broadcast
		return closest == null;
	}
	
	public float analyzeTree(TreeInfo tree){
		float hypotenuse = here.distanceTo(tree.location);
		float theta = (float) Math.abs(moving.radiansBetween(here.directionTo(tree.location)));
		float opposite = hypotenuse * (float)Math.sin(theta) - body;
		if(opposite > tree.radius)return Float.MAX_VALUE;
		
		float dist = hypotenuse * (float)Math.cos(theta) - (tree.radius * tree.radius - opposite * opposite);
		return dist;
	}
	
	public void moveTowards(){
		Slice[] avoid = Slice.combine(evadeObstacles(), evadeTrees());
		if(avoid.length>0 && avoid[0].complete){
			return;
		}
		
		avoid = detectWallDir(avoid);

		for(int i = 0; i<avoid.length; i++){
			if(avoid[i].contains(general)){
				general = avoid[i].round(general);
				break;
			}
		}
		
		try{
			if(rc.canMove(general)){
				rc.move(general);
			}
		}catch(Exception e){
			System.out.println("[ERROR] Tried to move");
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
		
		if(east != null && !rc.canMove(east)){
			treeMultiplier = 2;
			BroadcastSystem.setWall(rc, here.x+body, "EAST");
			unavailable = Slice.combine(unavailable, new Slice[]{new Slice(east.rotateLeftRads((float)Math.PI/2+PlayerConstants.SOLDIER_EVADE_TREE), east.rotateRightRads((float)Math.PI/2+PlayerConstants.SOLDIER_EVADE_TREE))});
		}else if(west != null && !rc.canMove(west)){
			treeMultiplier = 2;
			BroadcastSystem.setWall(rc, here.x-body, "WEST");
			unavailable = Slice.combine(unavailable, new Slice[]{new Slice(west.rotateLeftRads((float)Math.PI/2+PlayerConstants.SOLDIER_EVADE_TREE), west.rotateRightRads((float)Math.PI/2+PlayerConstants.SOLDIER_EVADE_TREE))});
		}else if(north != null && !rc.canMove(north)){
			treeMultiplier = 2;
			BroadcastSystem.setWall(rc, here.y+body, "NORTH");
			unavailable = Slice.combine(unavailable, new Slice[]{new Slice(north.rotateLeftRads((float)Math.PI/2+PlayerConstants.SOLDIER_EVADE_TREE), north.rotateRightRads((float)Math.PI/2+PlayerConstants.SOLDIER_EVADE_TREE))});
		}else if(south != null && !rc.canMove(south)){
			treeMultiplier = 2;
			BroadcastSystem.setWall(rc, here.y-body, "SOUTH");
			unavailable = Slice.combine(unavailable, new Slice[]{new Slice(south.rotateLeftRads((float)Math.PI/2+PlayerConstants.SOLDIER_EVADE_TREE), south.rotateRightRads((float)Math.PI/2+PlayerConstants.SOLDIER_EVADE_TREE))});
		}
		return Slice.simplify(unavailable);
	}
	
	public Slice[] evadeObstacles(){
		Slice[] unavailable = null;
		for(int i = 0; i<ri.length; i++){
			if(here.distanceTo(ri[i].location) > ri[i].getRadius()+stride+body){
				continue;
			}
			float half = (float) Math.asin((ri[i].getRadius()+body)/here.distanceTo(ri[i].location));
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
		
		unavailable = Slice.simplify(unavailable);
		return unavailable;
	}
	
	public Slice[] evadeTrees(){
		Slice[] unavailable = null;
		for(int i = 0; i<ti.length; i++){
			if(here.distanceTo(ti[i].location) > ti[i].radius+stride+body){
				continue;
			}
			float half = (float) Math.asin((ti[i].radius+body)/here.distanceTo(ti[i].location));
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
		
		unavailable = Slice.simplify(unavailable);
		return unavailable;
	}
	
	/*public void setup(){
		try{
			int planted = 0;
			while(planted < 5){
				// if any of these fail because a lumberjack is in the way, just restart loop
				if(rc.canPlantTree(general)){
					rc.plantTree(general);
					treePos[planted] = here.add(general, body + 1.01F);
					planted++;
					if(rc.canMove(moving)){
						rc.move(moving);
					}
					Clock.yield();
				}else{
					if(rc.canMove(general.rotateLeftDegrees((float)Math.PI))){
						rc.move(general.rotateLeftDegrees((float)Math.PI));
						Clock.yield();
						continue;
					}
				}
				if(rc.canMove(moving)){
					rc.move(moving);
				}
				Clock.yield();
			}
			
			waterPos[2] = treePos[2].add(direction, dist)
		}catch(Exception e){
			System.out.println("[ERROR] Tree setup failed");
		}
	}*/
	
	public void findSettle(){
		MapLocation moveVect = new MapLocation(0,0);
		for(int i = 0; i<ri.length; i++){
			if(ri[i].getTeam() != rc.getTeam())continue;
			float vectMag = 0;
			Direction vectRad = new Direction(0);
			if(ri[i].type == RobotType.GARDENER && here.distanceTo(ri[i].location) < PlayerConstants.DIST_GARDENER){
				vectMag = PlayerConstants.DIST_GARDENER - here.distanceTo(ri[i].location);
				vectRad = ri[i].location.directionTo(here);
			}else if(ri[i].type == RobotType.ARCHON && here.distanceTo(ri[i].location) < PlayerConstants.DIST_ARCHON){
				vectMag = PlayerConstants.DIST_ARCHON - here.distanceTo(ri[i].location);
				vectRad = ri[i].location.directionTo(here);
			}
			moveVect = moveVect.add(vectRad, vectMag);
		}
		if(moveVect.x == 0 && moveVect.y == 0){
			migrating = false;
			System.out.println("migrated");
			return;
		}else{
			moveVect.add(allyLocation.directionTo(here), stride);
		}
		general = new MapLocation(0,0).directionTo(moveVect);
		moveTowards();
	}
	
	public void buildTurn(){
		boolean td = treeDense();
		boolean cr = closeRange();
		/*if(td){
			if(cr){
				buildLogic1();
			}else{
				buildLogic2();
			}
		}else{
			if(cr){
				buildLogic3();
			}else{
				buildLogic4();
			}
		}*/
		buildLogic1();
		//need another statement for building tanks and soldiers only
		
		try{
			for(int i = 0; i<ti.length; i++){
				if(ti[i].maxHealth-ti[i].health >= GameConstants.WATER_HEALTH_REGEN_RATE){
					if(rc.canWater(ti[i].ID)){
						rc.water(ti[i].ID);
						break;
					}
				}
			}
		}catch(Exception e){
			System.out.println("[Error] Attempt to water failed");
		}
		
		if(buildPhase>=2)return;
		
		for(int i = 1; i<buildDir.length; i++){
			if(rc.canPlantTree(buildDir[i])){
				try{
					rc.plantTree(buildDir[i]);
					buildPhase++;
					treesBuilt++;
				}catch(Exception e){
					System.out.println("[ERROR] Calculation Tree Plant Off");
				}
			}
		}
	}
	
	/**
	 * Build Logic for close range and dense trees.
	 * Lumberjack -> Soldier -> Scout
	 */
	public void buildLogic1(){
		if(lastBuild == RobotType.SCOUT || lastBuild == null){
			if(rc.canBuildRobot(RobotType.LUMBERJACK, buildDir[0])){
				try{
					rc.buildRobot(RobotType.LUMBERJACK, buildDir[0]);
					lastBuild = RobotType.LUMBERJACK;
					buildPhase = 0;
				}catch(Exception e){
					System.out.println("[ERROR] Calculation Lumberjack Build Off");
				}
			}
		}else if(lastBuild == RobotType.SOLDIER){
			if(rc.canBuildRobot(RobotType.SCOUT, buildDir[0])){
				try{
					rc.buildRobot(RobotType.SCOUT, buildDir[0]);
					lastBuild = RobotType.SCOUT;
					buildPhase = 0;
				}catch(Exception e){
					System.out.println("[ERROR] Calculation Scout Build Off");
				}
			}
		}else if(lastBuild == RobotType.LUMBERJACK){
			if(rc.canBuildRobot(RobotType.SOLDIER, buildDir[0])){
				try{
					rc.buildRobot(RobotType.SOLDIER, buildDir[0]);
					lastBuild = RobotType.SOLDIER;
					buildPhase = 0;
				}catch(Exception e){
					System.out.println("[ERROR] Calculation Soldier Build Off");
				}
			}
		}
	}
	
	/**
	 * Build Logic for not close range and dense trees.
	 * Lumberjack -> Scout
	 */
	public void buildLogic2(){
		if((lastBuild == RobotType.SCOUT && scoutsBuilt > 3) || lastBuild == null){
			if(rc.canBuildRobot(RobotType.LUMBERJACK, buildDir[0])){
				try{
					rc.buildRobot(RobotType.LUMBERJACK, buildDir[0]);
					lastBuild = RobotType.LUMBERJACK;
					buildPhase = 0;
					scoutsBuilt = 0;
				}catch(Exception e){
					System.out.println("[ERROR] Calculation Lumberjack Build Off");
				}
			}
		}else{
			if(rc.canBuildRobot(RobotType.SCOUT, buildDir[0])){
				try{
					rc.buildRobot(RobotType.SCOUT, buildDir[0]);
					lastBuild = RobotType.SCOUT;
					buildPhase = 0;
					scoutsBuilt++;
				}catch(Exception e){
					System.out.println("[ERROR] Calculation Scout Build Off");
				}
			}
		}
	}
	
	/**
	 * Build Logic for close range and sparse trees.
	 * Soldier -> Scout
	 */
	public void buildLogic3(){
		if(lastBuild == RobotType.LUMBERJACK || lastBuild == RobotType.SCOUT || lastBuild == null){
			if(rc.canBuildRobot(RobotType.SOLDIER, buildDir[0])){
				try{
					rc.buildRobot(RobotType.SOLDIER, buildDir[0]);
					lastBuild = RobotType.SOLDIER;
					buildPhase = 0;
				}catch(Exception e){
					System.out.println("[ERROR] Calculation Lumberjack Build Off");
				}
			}
		}else if(lastBuild == RobotType.SOLDIER){
			if(rc.canBuildRobot(RobotType.SCOUT, buildDir[0])){
				try{
					rc.buildRobot(RobotType.SCOUT, buildDir[0]);
					lastBuild = RobotType.SCOUT;
					buildPhase = 0;
				}catch(Exception e){
					System.out.println("[ERROR] Calculation Scout Build Off");
				}
			}
		}
	}
	
	/**
	 * Build Logic for not close range and sparse trees.
	 * Scout -> Soldier
	 */
	public void buildLogic4(){
		if(lastBuild == RobotType.LUMBERJACK || lastBuild == RobotType.SOLDIER || lastBuild == null){
			if(rc.canBuildRobot(RobotType.SCOUT, buildDir[0])){
				try{
					rc.buildRobot(RobotType.SCOUT, buildDir[0]);
					lastBuild = RobotType.SCOUT;
					buildPhase = 0;
				}catch(Exception e){
					System.out.println("[ERROR] Calculation Lumberjack Build Off");
				}
			}
		}else if(lastBuild == RobotType.SCOUT){
			if(rc.canBuildRobot(RobotType.SOLDIER, buildDir[0])){
				try{
					rc.buildRobot(RobotType.SOLDIER, buildDir[0]);
					lastBuild = RobotType.SOLDIER;
					buildPhase = 0;
				}catch(Exception e){
					System.out.println("[ERROR] Calculation Scout Build Off");
				}
			}
		}
	}
	
	public void initDirs(){
		if(buildDir != null)return;
		
		Slice[] avoid = Slice.combine(detectObstacles(), detectTrees());
		avoid = detectWallDir(avoid);
		System.out.println("Avoid "+avoid.length);

		Direction buildZero = here.directionTo(enemyLocation);
		for(int i = 0; i<avoid.length; i++){
			if(avoid[i].contains(buildZero)){
				buildZero = avoid[i].round(buildZero);
				break;
			}
		}
		System.out.println("BuildZero "+buildZero);
		
		buildDir = new Direction[6];
		for(int i = 0; i<6; i++){
			buildDir[i] = buildZero.rotateLeftRads((float)Math.PI / 3 * i);
		}
	}
	
	public Slice[] detectObstacles(){
		Slice[] unavailable = null;
		for(int i = 0; i<ri.length; i++){
			if(here.distanceTo(ri[i].location) > ri[i].getRadius()+PlayerConstants.PLANT_MARGIN){
				continue;
			}
			float half = (float) Math.asin((ri[i].getRadius()+body)/here.distanceTo(ri[i].location));
			float middle = here.directionTo(ri[i].location).radians;
			Slice cone = new Slice(new Direction(middle+half+PlayerConstants.PLANT_RADIUS), new Direction(middle-half-PlayerConstants.PLANT_RADIUS));
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
		
		unavailable = Slice.simplify(unavailable);
		return unavailable;
	}
	
	public Slice[] detectTrees(){
		Slice[] unavailable = null;
		TreeInfo[] ti = rc.senseNearbyTrees();
		for(int i = 0; i<ti.length; i++){
			if(here.distanceTo(ti[i].location) > ti[i].radius+PlayerConstants.PLANT_MARGIN){
				continue;
			}
			System.out.println("Dist "+here.distanceTo(ti[i].location)+" "+ti[i].ID);
			float half = (float) Math.asin((ti[i].radius+body)/here.distanceTo(ti[i].location));
			float middle = here.directionTo(ti[i].location).radians;
			Slice cone = new Slice(new Direction(middle+half+PlayerConstants.PLANT_RADIUS), new Direction(middle-half-PlayerConstants.PLANT_RADIUS));
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
		
		unavailable = Slice.simplify(unavailable);
		return unavailable;
	}
	
	public boolean checkStuck(){
		int total = 0;
		for(int i = 0; i< previousLoc.length-1; i++){
			previousLoc[i] = previousLoc[i+1];
			if(previousLoc[i]==null)continue;
			if(previousLoc[i].equals(here)){
				total++;
			}
		}
		previousLoc[previousLoc.length-1] = here;
		total++;
		return total >= PlayerConstants.CHECK_STUCK_NUM;
	}
	
	public boolean treeDense(){
		float total = type.sensorRadius * type.sensorRadius * (float)Math.PI;
		float trees = 0F;
		for(int i = 0; i<ti.length; i++){
			if(ti[i].getTeam()!=rc.getTeam()){
				trees += ti[i].radius * ti[i].radius *(float)Math.PI;
			}
		}
		System.out.println(trees*treeMultiplier/total);
		return trees*treeMultiplier/total >= PlayerConstants.TREE_DENSITY_PERCENT;
	}
	
	public boolean closeRange(){
		return here.distanceTo(enemyLocation)<PlayerConstants.CLOSE_RANGE_MARGIN;
	}
}
