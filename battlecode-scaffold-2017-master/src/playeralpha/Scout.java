package playeralpha;

import battlecode.common.*;

public class Scout {
	RobotController rc;
	RobotType type = RobotType.SCOUT;
	final float berth = .06141592654F;
	float stride, body;
	MapLocation enemylocation;
	MapLocation here;
	Direction shooting;
	
	public Scout(RobotController rc){
		this.rc = rc;
		enemylocation = new MapLocation(22, 520);
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
			shooting = new Direction((float) (Math.PI / 6 * 5));
			moveTowards(enemylocation);
			if(rc.canFireSingleShot()){
				rc.fireSingleShot(shooting);
			}
			Clock.yield();
		}catch(Exception e){
			System.out.println("Turn could not happen");
			e.printStackTrace();
		}
	}
	
	public void moveTowards(MapLocation target){
		Direction general = here.directionTo(target);
		Slice[] avoid = combine(evadeBullets(), evadeObstacles());
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
			}
		}
		try{
			if(rc.canMove(general)){
				rc.move(general);
			}
		}catch(Exception e){
			System.out.println("Tried to move");
		}
	}
	
	public Slice[] combine(Slice[] a, Slice[] b){
		Slice[] ret = new Slice[a.length+b.length];
		for(int i = 0; i<a.length; i++){
			ret[i] = a[i];
		}
		int mark = a.length;
		for(int i = 0; i<b.length; i++){
			ret[mark+i] = b[i];
		}
		return ret;
	}
	
	public Slice[] evadeObstacles(){
		RobotInfo[] ri = rc.senseNearbyRobots();
		Slice[] unavailable = null;
		int targThreat = 0;
		for(int i = 0; i<ri.length; i++){
			if(ri[i].team != rc.getTeam()){
				switch(ri[i].getType()){
				case TANK:
					if(targThreat<1){
						targThreat = 1;
						shooting = here.directionTo(ri[i].location);
					}
				case LUMBERJACK:
					if(targThreat<2){
						targThreat = 2;
						shooting = here.directionTo(ri[i].location);
					}
				case SOLDIER:
					if(targThreat<3){
						targThreat = 3;
						shooting = here.directionTo(ri[i].location);
					}
				case SCOUT:
					if(targThreat<4){
						targThreat = 4;
						shooting = here.directionTo(ri[i].location);
					}
				case GARDENER:
					if(targThreat<5){
						targThreat = 5;
						shooting = here.directionTo(ri[i].location);
						enemylocation = ri[i].location;
					}
				case ARCHON:
					if(targThreat<6){
						targThreat = 6;
						shooting = here.directionTo(ri[i].location);
						enemylocation = ri[i].location;
					}
				}
			}
			if(here.distanceTo(ri[i].location) > ri[i].getRadius()){
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
					theta1 = altitudeAngle.radians + (float) Math.acos((double)altitudeLength/oa);
				}else{
					theta1 = altitudeAngle.radians - (float) Math.acos((double)altitudeLength/oa);
				}
			}else{
				float hypotenuse = here.distanceTo(endpoint1);
				if(altitudeAngle.radiansBetween(here.directionTo(endpoint1))>0){
					theta1 = altitudeAngle.radians + (float) Math.acos((double)altitudeLength/hypotenuse);
				}else{
					theta1 = altitudeAngle.radians - (float) Math.acos((double)altitudeLength/hypotenuse);
				}
			}
			
			if(here.distanceTo(endpoint2) > oa){
				if(altitudeAngle.radiansBetween(here.directionTo(endpoint2))>0){
					theta2 = altitudeAngle.radians + (float) Math.acos((double)altitudeLength/oa);
				}else{
					theta2 = altitudeAngle.radians - (float) Math.acos((double)altitudeLength/oa);
				}
			}else{
				float hypotenuse = here.distanceTo(endpoint2);
				if(altitudeAngle.radiansBetween(here.directionTo(endpoint2))>0){
					theta2 = altitudeAngle.radians + (float) Math.acos((double)altitudeLength/hypotenuse);
				}else{
					theta2 = altitudeAngle.radians - (float) Math.acos((double)altitudeLength/hypotenuse);
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
					j = i+1;
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
	
	public class Line{
		
		public float x;
		public float y;
		public float m;
		
		public Line(MapLocation point, Direction slope){
			x=point.x;
			y=point.y;
			m=(float) Math.tan(slope.radians);
		}
		
		public MapLocation intersect(Line line2){
			float pointX = (line2.m * line2.x - m * x + y - line2.y)/(line2.m - m);
			float pointY = m * (pointX - x) + y;
			return new MapLocation(pointX, pointY);
		}
	}
	
	public class Slice{
		
		public Direction open;
		public Direction close;
		public boolean concave = false;
		public boolean complete = false;
		
		public Slice(Direction open, Direction close){
			this.open = open;
			this.close = close;
			checkConcave();
		}
		
		public boolean add(Direction o, Direction c){
			boolean success = false;
			if(complete)return true;
			if(contains(o)){
				if(contains(c)){
					if(new Slice(o,c).contains(open)){
						close = open;
						complete = true;
						return true;
					}
					success = true;
				}else{
					close = c;
					success = true;
				}
			}else{
				if(contains(c)){
					open = o;
					success = true;
				}else{
					Slice other = new Slice(o,c);
					if(other.contains(open) && other.contains(close)){
						success = true;
						open = other.open;
						close = other.close;
						concave = other.concave;
						complete = other.complete;
						if(complete)return true;
					}
				}
			}

			checkConcave();
			return success;
		}
		
		public boolean add(Slice slice){
			if(slice.concave){
				Direction mid1 = new Direction(slice.open.radians-(float)Math.PI);
				Direction mid2 = new Direction(slice.close.radians+(float)Math.PI);
				if(add(slice.open, mid1)){
					add(mid2,slice.close);
				}else{
					if(add(mid2,slice.close)){
						add(slice.open, mid1);
					}else{
						return false;
					}
				}
			}else{
				return add(slice.open, slice.close);
			}
			return true;
		}
		
		public void checkConcave(){
			concave = close.radiansBetween(open)<0;
		}
		
		public boolean contains(Direction dir){
			boolean inside = false;
			if(!concave){
				if(close.radiansBetween(dir)>=0 && dir.radiansBetween(open)>=0){
					inside = true;
				}
			}else{
				inside = true;
				if(open.radiansBetween(dir)>0 && dir.radiansBetween(close)>0){
					inside = false;;
				}
			}
			return inside;
		}
		
	}
	
}
