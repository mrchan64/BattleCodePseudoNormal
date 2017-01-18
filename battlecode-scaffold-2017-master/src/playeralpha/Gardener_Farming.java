package playeralpha;

import battlecode.common.*;

public class Gardener_Farming {

	RobotController rc;
	RobotType type = RobotType.GARDENER;
	float stride, body;
	int numFarmers;
	boolean migrating = true;
	MapLocation destination;
	MapLocation relativeSafety;
	Direction general;
	RobotInfo[] ri;
	MapLocation here;
	MapLocation[] previous = {null, null};
	float ggDistance = 5.77F;
	float distMargin = .708F;
	float floatError = .00007F;
	float treeBerth = 1F;
	float backtrackError = 1.5F;
	Direction previousDir = null;
	int numWrongDir = 0;
	int branchesChecked = 0;
	int onBranch = 0;
	final int maxWrongDir = 25;
	final int deathOnBranch = 180;
	final int maxOnBranch = 160;
	Direction[] dirs = {new Direction(0), 
			new Direction((float) (Math.PI/3)), 
			new Direction((float) (Math.PI/3*2)), 
			new Direction((float) Math.PI),
			new Direction((float) (Math.PI/3*4)),
			new Direction((float) (Math.PI/3*5))};
	Direction[] availDirs = {new Direction((float) (Math.PI/6)), 
			new Direction((float) (Math.PI/2)), 
			new Direction((float) (Math.PI/6*5)), 
			new Direction((float) Math.PI/6*7),
			new Direction((float) (Math.PI/2*3)),
			new Direction((float) (Math.PI/6*11))};
	MapLocation[] availLocs = null;
	
	public Gardener_Farming(RobotController rc){
		this.rc = rc;
		stride = type.strideRadius;
		body = type.bodyRadius;
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
			numFarmers = BroadcastSystem.checkNumFarmers(rc, !migrating);
			if(migrating){
				if(!BroadcastSystem.amMigrating(rc))return;
				setMigrationPath();
				if(here.distanceTo(destination)<stride){
					if(rc.canMove(destination)){
						try{
							rc.move(destination);
							migrating = false;
						}catch(Exception e){
							System.out.println("Way blocked");
						}
					}else{
						BroadcastSystem.setInvalidLocation(rc, destination);
					}
				}else{
					general = here.directionTo(destination);
					moveTowards();
					onBranch++;
				}
				here = rc.getLocation();
			}else{
				if(availLocs == null){
					setAvailLocs();
				}
				nearByOpenings();
				buildTrees();
				waterTrees();
			}
			Clock.yield();
		}catch(Exception e){
			System.out.println("[ERROR] Turn could not happen");
			e.printStackTrace();
		}
	}
	
	public void setAvailLocs(){
		availLocs = new MapLocation[6];
		for(int i = 0; i<availDirs.length; i++){
			availLocs[i] = here.add(availDirs[i].radians ,ggDistance);
		}
	}
	
	public void setMigrationPath(){
		relativeSafety = BroadcastSystem.getRelativeSafety(rc);
		destination = BroadcastSystem.getGardenerDest(rc);
		if(destination == null){
			destination = relativeSafety;
		}
		alignDest();
	}
	
	public void alignDest(){
		int id = BroadcastSystem.getAlignID(rc);
		for(int i = 0; i<ri.length; i++){
			if(ri[i].ID!=id)continue;
			//if(ri[i].team!=rc.getTeam() || ri[i].type!=RobotType.GARDENER)continue;
			float distTo = ri[i].location.distanceTo(destination);
			if(distTo < ggDistance+distMargin){
				int region = (int)((ri[i].location.directionTo(destination).radians + Math.PI * 2) / (Math.PI / 3)) % 6;
				destination = ri[i].location.add((float) (availDirs[region].radians), ggDistance);
				break;
			}
		}
	}
	
	public void waterTrees(){
		TreeInfo[] trees = rc.senseNearbyTrees();
		try{
			for(int i = 0; i<trees.length; i++){
				if(trees[i].maxHealth-trees[i].health >= GameConstants.WATER_HEALTH_REGEN_RATE){
					if(rc.canWater(trees[i].ID)){
						rc.water(trees[i].ID);
					}
				}
			}
		}catch(Exception e){
			System.out.println("[Error] Attempt to water failed");
		}
	}
	
	public void buildTrees(){
		if(rc.isBuildReady()){
			try{
				for(int i = 0; i<dirs.length; i++){
					if(rc.canPlantTree(dirs[i])){
						rc.plantTree(dirs[i]);
						break;
					}
				}
			}catch(Exception e){
				System.out.println("[Error] Planting Tree Failed");
			}
		}
	}
	
	public void moveTowards(){
		Slice[] avoid = Slice.combine(evadeObstacles(), evadeTrees());
		for(int i = 0; i<avoid.length; i++){
			for(int j = i+1; j<avoid.length; j++){
				if(avoid[i].add(avoid[j])){
					avoid = remove(avoid, j);
					j = i;
				}
			}
		}
		if(avoid.length>0 && avoid[0].complete){
			rc.disintegrate();
			return;
		}
		
		Direction other = null;

		for(int i = 0; i<avoid.length; i++){
			if(avoid[i].contains(general)){
				if(Math.abs(avoid[i].open.radiansBetween(general))<Math.abs(avoid[i].close.radiansBetween(general))){
					general = avoid[i].open;
					other = avoid[i].close;
				}else{
					general = avoid[i].close;
					other = avoid[i].open;
				}
				break;
			}
		}
		if(checkStuck(other)){
			BroadcastSystem.setInvalidLocation(rc, destination);
		}
		
		try{
			if(rc.canMove(general)){
				rc.move(general);
			}else{
				//System.out.println("Blocked "+general+"\nDestination "+destination);
				if(numFarmers == 0){
					migrating = false;
				}else{
					numWrongDir = maxWrongDir +1;
					onBranch+=2;
					if(onBranch>maxOnBranch){
						BroadcastSystem.setInvalidLocation(rc, destination);
						rc.disintegrate();
					}
				}
			}
		}catch(Exception e){
			System.out.println("[ERROR] Tried to move");
		}
	}
	
	public void detectWallDir(Slice[] unavailable){
		Direction east = new Direction(0);
		Direction west = new Direction((float)Math.PI);
		Direction north = new Direction((float)Math.PI/2);
		Direction south = new Direction(-1*(float)Math.PI/2);
		
		for(int i = 0; i<unavailable.length; i++){
			if(unavailable[i].contains(east)){
				east = null;
			}
			if(unavailable[i].contains(west)){
				west = null;
			}
			if(unavailable[i].contains(north)){
				north = null;
			}
			if(unavailable[i].contains(south)){
				south = null;
			}
		}
		
		if(east != null && !rc.canMove(east)){
			BroadcastSystem.setWall(rc, here.x+body, "EAST");
		}else if(west != null && !rc.canMove(west)){
			BroadcastSystem.setWall(rc, here.x-body, "WEST");
		}else if(north != null && !rc.canMove(north)){
			BroadcastSystem.setWall(rc, here.y+body, "NORTH");
		}else if(south != null && !rc.canMove(south)){
			BroadcastSystem.setWall(rc, here.y-body, "SOUTH");
		}
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
	
	public Slice[] evadeTrees(){
		Slice[] unavailable = null;
		TreeInfo[] ti = rc.senseNearbyTrees();
		for(int i = 0; i<ti.length; i++){
			if(here.distanceTo(ti[i].location) > ti[i].radius+stride+body){
				continue;
			}
			float half = (float) Math.asin((ti[i].radius+body)/here.distanceTo(ti[i].location)) * treeBerth;
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
	
	public boolean checkStuck(Direction other){
		boolean stuck = false;
		if(previousDir != null){
			float error;
			if(branchesChecked == 0){
				error = backtrackError/2;
			}else{
				error = backtrackError;
			}
			if(Math.abs(new Direction(previousDir.radians).radiansBetween(new Direction(general.radians)))<error){
				onBranch++;
				numWrongDir++;
				if(branchesChecked == 1){
					branchesChecked = 2;
				}
				if(numWrongDir>maxWrongDir){
					if(branchesChecked==0){
						branchesChecked = 1;
					}else{
						stuck = true;
						branchesChecked = 0;
					}
					numWrongDir = 0;
				}else{
					if(other!=null){
						general = other;
					}else{
						if(branchesChecked==1){
							branchesChecked = 2;
						}/*else{
							stuck = true;
							branchesChecked = 0;
							numWrongDir = 0;
						}*/
					}
				}
			}else{
				numWrongDir=0;
				if(branchesChecked == 2){
					branchesChecked = 0;
				}
			}
		}
		previousDir = general.rotateRightRads((float)Math.PI);
		if(onBranch > maxOnBranch){
			stuck = true;
			BroadcastSystem.setInvalidLocation(rc, destination);
		}
		if(onBranch > deathOnBranch){
			rc.disintegrate();
		}
		return stuck;
	}
	
	public void nearByOpenings(){
		if(availLocs.length == 0){
			return;
		}
		MapLocation n = BroadcastSystem.getInvalidLocation(rc);
		
		MapLocation closest = null;
		float distance = 0;
		for(int i = 0; i<availLocs.length; i++){
			try{
				if(n!=null && Math.abs(n.x-availLocs[i].x)<.5 && Math.abs(n.y-availLocs[i].y)<.5){
					removeLoc(i);
					i--;
				}else{
					RobotInfo check = rc.senseRobotAtLocation(availLocs[i]);
					if(check != null && Math.abs(check.location.x - availLocs[i].x) < floatError && Math.abs(check.location.y - availLocs[i].y) < floatError){
						removeLoc(i);
						i--;
					}else{
						TreeInfo check2 = rc.senseTreeAtLocation(availLocs[i]);
						if(check2 != null && availLocs[i].distanceTo(check2.location)<check2.radius+body){
							removeLoc(i);
							i--;
						}else if(closest == null){
							closest = availLocs[i];
							distance = availLocs[i].distanceTo(relativeSafety);
						}else if(availLocs[i].distanceTo(relativeSafety)<distance){
							closest = availLocs[i];
							distance = availLocs[i].distanceTo(relativeSafety);
						}
					}
				}
			}catch(Exception e){
				System.out.println("[ERROR] Sense location out of range");
			}
		}
		if(closest == null)return;
		BroadcastSystem.setGardenerDest(rc, closest, distance);
	}
	
	public void removeLoc(int index){
		MapLocation[] newLoc = new MapLocation[availLocs.length - 1];
		for(int i = 0; i<newLoc.length; i++){
			if(i<index){
				newLoc[i] = availLocs[i];
			}else{
				newLoc[i] = availLocs[i+1];
			}
		}
		availLocs = newLoc;
	}
}
