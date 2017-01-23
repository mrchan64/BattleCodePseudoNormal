package playerbeta;

import battlecode.common.*;

public class Lumberjack {
	
	RobotController rc;
	RobotType type = RobotType.LUMBERJACK;
	float stride, body;
	int numFarmers;
	TreeInfo[] ti;
	RobotInfo[] ri;
	Direction general;
	MapLocation here;
	MapLocation enemyLocation;
	
	public Lumberjack(RobotController rc){
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
			ti = rc.senseNearbyTrees();
			ri = rc.senseNearbyRobots();
			int[] arr = BroadcastSystem.readEnemyLocation(rc);
			enemyLocation = new MapLocation(arr[0], arr[1]);
			general = here.directionTo(enemyLocation);
			if(!targetTrees()){
				moveTowards();
			}
			Clock.yield();
		}catch(Exception e){
			System.out.println("[ERROR] Turn could not happen");
			e.printStackTrace();
		}
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
	
	public boolean targetTrees(){
		TreeInfo closest = null;
		float distance = Float.MAX_VALUE;
		for(int i = 0; i<ti.length; i++){
			if(ti[i].getTeam() != Team.NEUTRAL)continue;
			float d = here.distanceTo(ti[i].location);
			if(d<distance){
				closest = ti[i];
				distance = d;
			}
		}
		if(closest != null){
			if(rc.canChop(closest.ID)){
				try{
					rc.chop(closest.ID);
					return true;
				}catch(Exception e){
					System.out.println("[ERROR] Chop Calculation Off");
				}
			}else{
				general = here.directionTo(closest.location);
			}
		}
		return false;
	}
}
