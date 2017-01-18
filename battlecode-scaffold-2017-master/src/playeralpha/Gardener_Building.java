package playeralpha;

import battlecode.common.*;

public class Gardener_Building {
	
	RobotController rc;
	RobotType type = RobotType.GARDENER;
	float stride, body;
	MapLocation here;
	MapLocation enemyLocation;
	RobotInfo[] ri;
	boolean migrating = false;
	
	public Gardener_Building(RobotController rc){
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
			int[] arr = BroadcastSystem.readEnemyLocation(rc);
			enemyLocation = new MapLocation(arr[0],arr[1]);
			//numFarmers = BroadcastSystem.checkNumFarmers(rc, !migrating);
			if(migrating){
				/*if(!BroadcastSystem.amMigrating(rc))return;
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
				here = rc.getLocation();*/
			}else{
				/*if(availLocs == null){
					setAvailLocs();
				}
				nearByOpenings();
				buildTrees();
				waterTrees();*/
			}
			Clock.yield();
		}catch(Exception e){
			System.out.println("[ERROR] Turn could not happen");
			e.printStackTrace();
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
	
	public void attemptBuild(){
		Direction buildDir = here.directionTo(enemyLocation);
		
		/*if(rc.getTeamBullets()<(float)rc.getRoundNum()/2){
			return;
		}*/
		Slice[] avoid = Slice.combine(evadeObstacles(), evadeTrees());
		
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
