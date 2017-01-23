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
	
	float distGardener = 8F;
	float distArchon = 7F;
	float plantRad = .521F;
	boolean migrating = true;
	MapLocation allyLocation;
	Direction[] buildDir = null;
	RobotType lastBuild = null;
	int buildPhase = 0;
	
	public final int MAX_LUMBERJACKS = 10;
	
	public Gardener(RobotController rc){
		this.rc = rc;
		head = new Head(rc);
		stride = type.strideRadius;
		body = type.bodyRadius;
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
			if(migrating){
				findSettle();
			}else{
				initDirs();
				buildTurn();
			}
			BroadcastSystem.checkNumFarmers(rc, migrating);
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
		
		Direction other = null;

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
			BroadcastSystem.setWall(rc, here.x+body, "EAST");
			unavailable = Slice.combine(unavailable, new Slice[]{new Slice(east.rotateLeftRads((float)Math.PI/2), east.rotateRightDegrees((float)Math.PI/2))});
		}else if(west != null && !rc.canMove(west)){
			BroadcastSystem.setWall(rc, here.x-body, "WEST");
			unavailable = Slice.combine(unavailable, new Slice[]{new Slice(west.rotateLeftRads((float)Math.PI/2), west.rotateRightDegrees((float)Math.PI/2))});
		}else if(north != null && !rc.canMove(north)){
			BroadcastSystem.setWall(rc, here.y+body, "NORTH");
			unavailable = Slice.combine(unavailable, new Slice[]{new Slice(north.rotateLeftRads((float)Math.PI/2), north.rotateRightDegrees((float)Math.PI/2))});
		}else if(south != null && !rc.canMove(south)){
			BroadcastSystem.setWall(rc, here.y-body, "SOUTH");
			unavailable = Slice.combine(unavailable, new Slice[]{new Slice(south.rotateLeftRads((float)Math.PI/2), south.rotateRightDegrees((float)Math.PI/2))});
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
			if(ri[i].type == RobotType.GARDENER && here.distanceTo(ri[i].location) < distGardener){
				vectMag = distGardener - here.distanceTo(ri[i].location);
				vectRad = ri[i].location.directionTo(here);
			}else if(ri[i].type == RobotType.ARCHON && here.distanceTo(ri[i].location) < distArchon){
				vectMag = distArchon - here.distanceTo(ri[i].location);
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
		if(rc.getRoundNum()<500 || numLumberjacks < MAX_LUMBERJACKS){
			buildLogic1();
		}else if(rc.getRoundNum()<750){
			buildLogic2();
		}else{
			buildLogic3();
		}
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
				}catch(Exception e){
					System.out.println("[ERROR] Calculation Tree Plant Off");
				}
			}
		}
	}
	
	public void buildLogic1(){
		if(lastBuild == RobotType.SOLDIER || lastBuild == null){
			if(rc.canBuildRobot(RobotType.LUMBERJACK, buildDir[0])){
				try{
					rc.buildRobot(RobotType.LUMBERJACK, buildDir[0]);
					lastBuild = RobotType.LUMBERJACK;
					buildPhase = 0;
				}catch(Exception e){
					System.out.println("[ERROR] Calculation Lumberjack Build Off");
				}
			}
		}else if(lastBuild == RobotType.LUMBERJACK){
			if(rc.canBuildRobot(RobotType.SCOUT, buildDir[0])){
				try{
					rc.buildRobot(RobotType.SCOUT, buildDir[0]);
					lastBuild = RobotType.SCOUT;
					buildPhase = 0;
				}catch(Exception e){
					System.out.println("[ERROR] Calculation Scout Build Off");
				}
			}
		}else if(lastBuild == RobotType.SCOUT){
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
	
	public void buildLogic2(){
		if(lastBuild == RobotType.SOLDIER || lastBuild == RobotType.LUMBERJACK){
			if(rc.canBuildRobot(RobotType.SCOUT, buildDir[0])){
				try{
					rc.buildRobot(RobotType.SCOUT, buildDir[0]);
					lastBuild = RobotType.SCOUT;
				}catch(Exception e){
					System.out.println("[ERROR] Calculation Scout Build Off");
				}
			}
		}else if(lastBuild == RobotType.SCOUT){
			if(rc.canBuildRobot(RobotType.SOLDIER, buildDir[0])){
				try{
					rc.buildRobot(RobotType.SOLDIER, buildDir[0]);
					lastBuild = RobotType.SOLDIER;
				}catch(Exception e){
					System.out.println("[ERROR] Calculation Soldier Build Off");
				}
			}
		}
	}
	public void buildLogic3(){
		if(rc.canBuildRobot(RobotType.SOLDIER, buildDir[0])){
			try{
				rc.buildRobot(RobotType.SOLDIER, buildDir[0]);
				lastBuild = RobotType.SOLDIER;
			}catch(Exception e){
				System.out.println("[ERROR] Calculation Soldier Build Off");
			}
		}
	}
	
	public void initDirs(){
		if(buildDir != null)return;
		
		Slice[] avoid = Slice.combine(detectObstacles(), detectTrees());
		
		Direction buildZero = here.directionTo(enemyLocation);
		for(int i = 0; i<avoid.length; i++){
			if(avoid[i].contains(buildZero)){
				buildZero = avoid[i].round(buildZero);
				break;
			}
		}
		
		buildDir = new Direction[6];
		for(int i = 0; i<6; i++){
			buildDir[i] = buildZero.rotateLeftRads((float)Math.PI / 3 * i);
		}
	}
	
	public Slice[] detectObstacles(){
		Slice[] unavailable = null;
		for(int i = 0; i<ri.length; i++){
			if(here.distanceTo(ri[i].location) > ri[i].getRadius()+1.01F){
				continue;
			}
			float half = (float) Math.asin((ri[i].getRadius()+body)/here.distanceTo(ri[i].location));
			float middle = here.directionTo(ri[i].location).radians;
			Slice cone = new Slice(new Direction(middle+half+plantRad), new Direction(middle-half-plantRad));
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
			if(here.distanceTo(ti[i].location) > ti[i].radius+1.01F){
				continue;
			}
			float half = (float) Math.asin((ti[i].radius+body)/here.distanceTo(ti[i].location));
			float middle = here.directionTo(ti[i].location).radians;
			Slice cone = new Slice(new Direction(middle+half+plantRad), new Direction(middle-half-plantRad));
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
}
