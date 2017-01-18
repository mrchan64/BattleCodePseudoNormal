package playeralpha;

import battlecode.common.*;

public class Scout {
	boolean efficient = true;
	
	RobotController rc;
	RobotType type = RobotType.SCOUT;
	final float berth = .06141592654F;
	final float kiteMultiplier = .8F;
	int scoutingTurns = 0;
	boolean cantMove = false;
	float scoutingDir;
	float stride, body;
	MapLocation enemylocation;
	MapLocation here;
	Direction shooting;
	Direction general;
	int generalThreat;
	int generalID;
	RobotInfo[] ri;
	Slice[] allies;
	boolean uploadEnemy;
	
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
			if(!efficient)allies = detectAllies();
			moveTowards();
			if(uploadEnemy){
				BroadcastSystem.sendEnemyLocation(rc, enemylocation, generalThreat, generalID);
			}else{
				if(here.distanceTo(enemylocation)<=stride){
					BroadcastSystem.sendInRange(rc);
				}
			}
			if(shooting != null && rc.canFireSingleShot()){
				rc.fireSingleShot(shooting);
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
		}
		
		if(scoutingDir>0){
			enemylocation = here.add(scoutingDir + (float)Math.PI / 3 * 2 * scoutingTurns, GameConstants.MAP_MAX_HEIGHT * 2);
		}
	}
	
	public void moveTowards(){
		Slice[] avoid;
		if(efficient){
			avoid = Slice.combine(evadeBullets_efficient(), evadeObstacles());
		}else{
			avoid = Slice.combine(evadeBullets(), evadeObstacles());
		}
		for(int i = 0; i<avoid.length; i++){
			for(int j = i+1; j<avoid.length; j++){
				if(avoid[i].add(avoid[j])){
					avoid = remove(avoid, j);
					j = i+1;
				}
			}
		}
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
		
		for(int i = 0; i<unavailable.length; i++){
			for(int j = i+1; j<unavailable.length; j++){
				if(unavailable[i].add(unavailable[j])){
					unavailable = remove(unavailable, j);
					j = i+1;
				}
			}
		}
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
	
	public Slice[] evadeObstacles(){
		Slice[] unavailable = null;
		int targThreat = 0;
		for(int i = 0; i<ri.length; i++){
			if(ri[i].team != rc.getTeam()){
				Direction toOpponent = here.directionTo(ri[i].location);
				if(efficient || !allyBetween(toOpponent)){
					switch(ri[i].getType()){
					case TANK:
						if(targThreat<1){
							targThreat = 1;
							shooting = toOpponent;
						}
					case LUMBERJACK:
						if(targThreat<2){
							targThreat = 2;
							shooting = toOpponent;
						}
					case SOLDIER:
						if(targThreat<3){
							targThreat = 3;
							shooting = toOpponent;
						}
					case SCOUT:
						if(targThreat<4){
							targThreat = 4;
							shooting = toOpponent;
						}
					case GARDENER:
						if(targThreat<5){
							targThreat = 5;
							shooting = toOpponent;
							enemylocation = ri[i].location;
						}
					case ARCHON:
						if(targThreat<6){
							targThreat = 6;
							shooting = toOpponent;
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
	
	public Slice[] evadeBullets(){
		Slice[] unavailable = null;
		
		BulletInfo[] bi = rc.senseNearbyBullets();
		
		MapLocation origin = new MapLocation(0,0);
		MapLocation point = new MapLocation(stride,body);
		
		float beta = origin.directionTo(point).radians+berth;
		float oa = origin.distanceTo(point);
		
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
			Direction altitudeAngle = here.directionTo(altitudeIntersect);
			
			float theta1, theta2;
			
			if(here.distanceTo(endpoint1) > oa){
				if(altitudeAngle
						.radiansBetween(here
								.directionTo(endpoint1))>0){
					theta1 = altitudeAngle.radians + acos(altitudeLength, oa);
				}else{
					theta1 = altitudeAngle.radians - acos(altitudeLength, oa);
				}
			}else{
				float hypotenuse = here.distanceTo(endpoint1);
				if(altitudeAngle.radiansBetween(here.directionTo(endpoint1))>0){
					theta1 = altitudeAngle.radians + acos(altitudeLength, hypotenuse);
				}else{
					theta1 = altitudeAngle.radians - acos(altitudeLength, hypotenuse);
				}
			}
			
			if(here.distanceTo(endpoint2) > oa){
				if(altitudeAngle.radiansBetween(here.directionTo(endpoint2))>0){
					theta2 = altitudeAngle.radians + acos(altitudeLength, oa);
				}else{
					theta2 = altitudeAngle.radians - acos(altitudeLength, oa);
				}
			}else{
				float hypotenuse = here.distanceTo(endpoint2);
				if(altitudeAngle.radiansBetween(here.directionTo(endpoint2))>0){
					theta2 = altitudeAngle.radians + acos(altitudeLength, hypotenuse);
				}else{
					theta2 = altitudeAngle.radians - acos(altitudeLength, hypotenuse);
				}
			}
			
			Direction open, close;
			
			if((new Direction(theta1)).radiansBetween(new Direction(theta2))>0){
				open = new Direction(theta2 + beta);
				close = new Direction(theta1 - beta);
			}else{
				close = new Direction(theta2 - beta);
				open = new Direction(theta1 + beta);
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
	
	public Slice[] evadeBullets_efficient(){
		Slice[] unavailable = null;
		
		BulletInfo[] bi = rc.senseNearbyBullets();
		
		MapLocation origin = new MapLocation(0,0);
		MapLocation point = new MapLocation(stride,body);
		
		float beta = origin.directionTo(point).radians+berth;
		
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
	
	public static void hypotenuse(float a, float b){
		MapLocation origin = new MapLocation(0,0);
		MapLocation point = new MapLocation(a,b);
		System.out.println(origin.distanceTo(point));
		System.out.println(origin.directionTo(point).radians);
	}
	
	public float acos(float adjacent, float hypotenuse){
		float opposite = (float)Math.sqrt(hypotenuse*hypotenuse - adjacent*adjacent);
		Direction alpha = new MapLocation(0, 0).directionTo(new MapLocation(adjacent, opposite));
		return alpha.radians;
	}
	
	public float asin(float opposite, float hypotenuse){
		float adjacent = (float)Math.sqrt(hypotenuse*hypotenuse - opposite*opposite);
		Direction alpha = new MapLocation(0, 0).directionTo(new MapLocation(adjacent, opposite));
		return alpha.radians;
	}
}
