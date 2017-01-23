package playerbeta;

import battlecode.common.*;

public class Soldier {
	final boolean efficient = true;
	
	RobotController rc;
	RobotType type = RobotType.SOLDIER;
	final float kiteMultiplier = .8F;
	float stride, body;
	MapLocation enemylocation;
	MapLocation here;
	MapLocation shooting;
	Direction general;
	int generalThreat;
	int generalID;
	RobotInfo[] ri;
	TreeInfo[] ti;
	Slice[] allies;
	boolean uploadEnemy;
	
	public Soldier(RobotController rc){
		this.rc = rc;
		enemylocation = new MapLocation(22, 500);
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
			ti = rc.senseNearbyTrees();
			int[] el = BroadcastSystem.readEnemyLocation(rc);
			enemylocation = new MapLocation(el[0],el[1]);
			generalThreat = el[2];
			generalID = el[3];
			general = here.directionTo(enemylocation);
			shooting = null;
			uploadEnemy = false;
			moveTowards();
			if(uploadEnemy){
				BroadcastSystem.sendEnemyLocation(rc, enemylocation, generalThreat, generalID);
			}else{
				if(here.distanceTo(enemylocation)<=stride){
					BroadcastSystem.sendInRange(rc);
				}
			}
			if(shooting != null && rc.canFireTriadShot()){
				rc.fireTriadShot(rc.getLocation().directionTo(shooting));
			}
			BroadcastSystem.countSoldier(rc);
			Clock.yield();
		}catch(Exception e){
			System.out.println("[ERROR] Turn could not happen");
			e.printStackTrace();
		}
	}
	
	public void moveTowards(){
		Slice[] avoid = Slice.combine(evadeTrees(), evadeObstacles());
		if(avoid.length>0 && avoid[0].complete){
			return;
		}
		for(int i = 0; i<avoid.length; i++){
			if(avoid[i].contains(general)){
				if(Math.abs(avoid[i].open.radiansBetween(general))<Math.abs(avoid[i].close.radiansBetween(general))){
					general = avoid[i].open;
				}else{
					general = avoid[i].close;
				}
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
	
	public void newGeneral(RobotInfo ri){
		float a = stride;
		float b = ri.getType().strideRadius;
		float c = here.distanceTo(ri.location);
		float d = (float)Math.sqrt((a*a+b*b-c*c)/2);
		float beta = (float) (Math.PI-Math.asin(d/a)) * kiteMultiplier;
		general.rotateRightRads((float) (Math.PI+beta));
	}
	
	public Slice[] detectAllies(){
		Slice[] unavailable = null;
		for(int i = 0; i<ri.length; i++){
			if(ri[i].team != rc.getTeam()){
				continue;
			}
			float half = (float) Math.asin(ri[i].getRadius()/here.distanceTo(ri[i].location));
			float middle = here.directionTo(ri[i].location).radians;
			Slice cone = new Slice(new Direction(middle+half), new Direction(middle-half));
			if(unavailable == null){
				unavailable = new Slice[1];
				unavailable[0] = cone;
			}else{
				Slice[] newSet = new Slice[unavailable.length+1];
				for(int j = 0; j<unavailable.length; i++){
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
	
	public boolean allyBetween(Direction dir){
		for(int i = 0; i<allies.length;i++){
			if(allies[i].contains(dir)){
				return true;
			}
		}
		return false;
	}
	
	public void shakeTrees(){
		TreeInfo[] ti = rc.senseNearbyTrees();
		for(int i = 0; i<ti.length; i++){
			if(rc.canShake(ti[i].ID)){
				try{
					rc.shake(ti[i].ID);
					break;
				}catch(Exception e){
					System.out.println("[ERROR] Could Not Shake");
				}
			}
		}
	}
	
	public Slice[] evadeObstacles(){
		Slice[] unavailable = null;
		int targThreat = 0;
		for(int i = 0; i<ri.length; i++){
			if(ri[i].team != rc.getTeam()){
				if(efficient || !allyBetween(here.directionTo(ri[i].location))){
					switch(ri[i].getType()){
					case TANK:
						if(targThreat<1){
							targThreat = 1;
							shooting = ri[i].location;
						}
					case LUMBERJACK:
						if(targThreat<2){
							targThreat = 2;
							shooting = ri[i].location;
						}
					case SOLDIER:
						if(targThreat<3){
							targThreat = 3;
							shooting = ri[i].location;
						}
					case SCOUT:
						if(targThreat<4){
							targThreat = 4;
							shooting = ri[i].location;
						}
					case GARDENER:
						if(targThreat<5){
							targThreat = 5;
							shooting = ri[i].location;
							enemylocation = ri[i].location;
						}
					case ARCHON:
						if(targThreat<6){
							targThreat = 6;
							shooting = ri[i].location;
							enemylocation = ri[i].location;
						}
					}
					if(targThreat>generalThreat){
						newGeneral(ri[i]);
						enemylocation = ri[i].location;
						generalID = ri[i].ID;
						generalThreat = targThreat;
						uploadEnemy = true;
					}
				}
			}
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
}
