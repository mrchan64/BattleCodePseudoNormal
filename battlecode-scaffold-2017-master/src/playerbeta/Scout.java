package playerbeta;

import battlecode.common.*;

public class Scout {
	boolean efficient = true;
	
	RobotController rc;
	RobotType type = RobotType.SCOUT;
	int scoutingTurns = 0;
	boolean cantMove = false;
	float scoutingDir;
	float stride, body;
	MapLocation enemylocation;
	MapLocation here;
	MapLocation shooting;
	Direction general;
	int generalThreat;
	int generalID;
	RobotInfo[] ri;
	boolean uploadEnemy;
	BodyInfo target;
	
	public Scout(RobotController rc){
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
			scoutingDir = BroadcastSystem.scoutingMission(rc);
			if(scoutingDir>-1){
				generalScouting();
			}else{
				scoutingTurns = 0;
				int[] el = BroadcastSystem.readEnemyLocation(rc);
				enemylocation = new MapLocation(el[0],el[1]);
				generalThreat = el[2];
				generalID = el[3];
			}
			ri = rc.senseNearbyRobots();
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
			here = rc.getLocation();
			if(shooting != null && rc.canFireSingleShot() && !allyBetween()){
				rc.fireSingleShot(here.directionTo(shooting));
			}
			Clock.yield();
		}catch(Exception e){
			System.out.println("[ERROR] Turn could not happen");
			e.printStackTrace();
		}
	}
	
	public void generalScouting(){
		generalThreat = 0;
		
		if(cantMove){
			cantMove = false;
			scoutingTurns++;
			if(Math.PI/4*3 > scoutingDir && scoutingDir >= Math.PI/4){
				BroadcastSystem.setWall(rc, here.y+body, "NORTH");
			}else if(Math.PI/4 > scoutingDir && scoutingDir >= -1*Math.PI/4){
				BroadcastSystem.setWall(rc, here.x+body, "EAST");
			}else if(-1*Math.PI/4 > scoutingDir && scoutingDir >= -1*Math.PI/4*3){
				BroadcastSystem.setWall(rc, here.y-body, "SOUTH");
			}else{
				BroadcastSystem.setWall(rc, here.x-body, "WEST");
			}
		}
		
		if(scoutingDir>0){
			enemylocation = here.add(scoutingDir + (float)Math.PI / 3 * 2 * scoutingTurns, GameConstants.MAP_MAX_HEIGHT * 2);
		}
	}
	
	public void moveTowards(){
		Slice[] avoid = Slice.combine(evadeBullets_efficient(), evadeObstacles());
		
		shakeTrees();

		avoid = Slice.simplify(avoid);
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
			}else{
				cantMove = true;
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
		float beta = (float) (Math.PI-Math.asin(d/a)) * PlayerConstants.KITE_MULTIPLIER;
		general = general.rotateRightRads((float) (Math.PI+beta));
	}
	
	public boolean allyBetween(){
		if(here.distanceTo(target.getLocation())<PlayerConstants.CLOSENESS_MARGIN + target.getRadius()*PlayerConstants.CLOSENESS_MULTIPLIER)return false;
		for(int i = 0; i<ri.length;i++){
			if(ri[i].team != rc.getTeam())continue;
			if(here.distanceTo(ri[i].location)>here.distanceTo(shooting))continue;
			float theta = (float)Math.asin(ri[i].getRadius()/here.distanceTo(ri[i].location));
			if(Math.abs(theta) > Math.abs(here.directionTo(ri[i].location).radiansBetween(here.directionTo(target.getLocation()))))return true;
		}
		return false;
	}
	
	public void shakeTrees(){
		TreeInfo[] ti = rc.senseNearbyTrees();
		float distance = Float.MAX_VALUE;
		for(int i = 0; i<ti.length; i++){
			if(ti[i].getContainedBullets()>0){
				if(rc.canShake(ti[i].ID)){
					try{
						rc.shake(ti[i].ID);
						return;
					}catch(Exception e){
						System.out.println("[ERROR] Could Not Shake");
					}
				}else{
					if(here.distanceTo(ti[i].location)<distance){
						general = here.directionTo(ti[i].location);
						distance = here.distanceTo(ti[i].location);
					}
				}
			}
		}
	}
	
	public Slice[] evadeObstacles(){
		Slice[] unavailable = null;
		int targThreat = 0;
		for(int i = 0; i<ri.length; i++){
			if(ri[i].team != rc.getTeam()){
				if(efficient){
					switch(ri[i].getType()){
					case TANK:
						if(targThreat<1){
							targThreat = 1;
							shooting = ri[i].location;
							enemylocation = ri[i].location;
							target = ri[i];
						}
					case LUMBERJACK:
						if(targThreat<2){
							targThreat = 2;
							shooting = ri[i].location;
							enemylocation = ri[i].location;
							target = ri[i];
						}
					case SOLDIER:
						if(targThreat<3){
							targThreat = 3;
							shooting = ri[i].location;
							enemylocation = ri[i].location;
							target = ri[i];
						}
					case SCOUT:
						if(targThreat<4){
							targThreat = 4;
							shooting = ri[i].location;
							enemylocation = ri[i].location;
							target = ri[i];
						}
					case GARDENER:
						if(targThreat<5){
							targThreat = 5;
							shooting = ri[i].location;
							enemylocation = ri[i].location;
							target = ri[i];
						}
					case ARCHON:
						if(targThreat<6){
							targThreat = 6;
							shooting = ri[i].location;
							enemylocation = ri[i].location;
							target = ri[i];
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
	
	public Slice[] evadeBullets_efficient(){
		Slice[] unavailable = null;
		
		BulletInfo[] bi = rc.senseNearbyBullets();
		
		MapLocation origin = new MapLocation(0,0);
		MapLocation point = new MapLocation(stride,body);
		
		float beta = origin.directionTo(point).radians+PlayerConstants.BERTH;
		
		for(int i = 0; i<bi.length; i++){
			MapLocation endpoint1 = bi[i].getLocation();
			Direction dir = bi[i].getDir();
			float speed = bi[i].getSpeed();
			MapLocation endpoint2 = endpoint1.add(dir,speed);
			
			Line bulletPath = new Line(endpoint1, dir);
			Line altitude = new Line(here, new Direction((float) (dir.radians + Math.PI/2)));
			
			MapLocation altitudeIntersect = bulletPath.intersect(altitude);
			float altitudeLength = here.distanceTo(altitudeIntersect);
			if(altitudeLength == 0){
				continue;
			}
			if(altitudeLength > stride + body){
				continue;
			}
			
			Direction theta1 = here.directionTo(endpoint1);
			Direction theta2 = here.directionTo(endpoint2);
			
			Direction open, close;
			
			if((theta1).radiansBetween(theta2)>0){
				open = new Direction(theta2.radians + beta);
				close = new Direction(theta1.radians - beta);
			}else{
				close = new Direction(theta2.radians - beta);
				open = new Direction(theta1.radians + beta);
			}
			
			if(unavailable == null){
				unavailable = new Slice[1];
				unavailable[0] = new Slice(open,close);
			}else{
				Slice[] newSet = new Slice[unavailable.length+1];
				for(int j = 0; j<unavailable.length; j++){
					newSet[j+1] = unavailable[j];
				}
				newSet[0] = new Slice(open, close);
				unavailable = newSet;
			}
		}
		if(unavailable == null)unavailable = new Slice[0];
		
		unavailable = Slice.simplify(unavailable);
		
		return unavailable;
		
	}
}
