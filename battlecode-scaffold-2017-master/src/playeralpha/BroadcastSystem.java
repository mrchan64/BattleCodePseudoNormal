package playeralpha;

import java.util.Arrays;

import battlecode.common.*;

public class BroadcastSystem {
	public static final int ENEMY_LOCATION_X = 0;
	public static final int ENEMY_LOCATION_Y = 1;
	public static final int ENEMY_THREAT_LEVEL = 2;
	public static final int ENEMY_ID = 3;
	public static final int ENEMY_SENSED = 4;
	public static final int SCOUT_FORMATION = 5;
	public static final int IN_ENEMY_RANGE = 6;
	public static final int HEAD_SWITCHER = 7;
	public static final int SCOUTS_CODE = 8;
	public static final int SCOUTS_TOTAL = 9;
	public static final int GARDENER_DEST_X = 10;
	public static final int GARDENER_DEST_Y = 11;
	public static final int GARDENER_DEST_DISTANCE = 12;
	public static final int GARDENER_ALIGN_ID = 13;
	public static final int RELATIVE_SAFETY_X = 14;
	public static final int RELATIVE_SAFETY_Y = 15;
	public static final int INVALID_LOCATION_X = 16;
	public static final int INVALID_LOCATION_Y = 17;
	public static final int INVALID_LOCATION_SET = 18;
	public static final int GARDENER_MIGRATING = 19;
	public static final int MIGRATED_FARMERS = 20;
	public static final int ON_HOLD_FARMERS = 21;
	public static final int BUILDING_GARDENERS = 22;
	public static final int WEST_WALL = 23;
	public static final int EAST_WALL = 24;
	public static final int NORTH_WALL = 25;
	public static final int SOUTH_WALL = 26;
	public static final int ALLY_LOCATION_X = 27;
	public static final int ALLY_LOCATION_Y = 28;
	
	public static int previousHead = 1;
	public static int scoutNum = -1;
	
	public static void sendEnemyLocation(RobotController rc, MapLocation ml, int threat, int id){
		try {
			rc.broadcast(ENEMY_LOCATION_X, (int)ml.x);
			rc.broadcast(ENEMY_LOCATION_Y, (int)ml.y);
			rc.broadcast(ENEMY_THREAT_LEVEL, threat);
			rc.broadcast(ENEMY_ID, id);
			rc.broadcast(ENEMY_SENSED, 1);
			rc.broadcast(SCOUT_FORMATION, 0);
			rc.broadcast(IN_ENEMY_RANGE, 1);
		} catch (GameActionException e) {
			System.out.println("[ERROR] Enemy Location Broadcast Failed");
		}
	}
	
	public static int[] readEnemyLocation(RobotController rc){
		int[] res = new int[4];
		try{
			res[0] = rc.readBroadcast(ENEMY_LOCATION_X);
			res[1] = rc.readBroadcast(ENEMY_LOCATION_Y);
			res[2] = rc.readBroadcast(ENEMY_THREAT_LEVEL);
			res[3] = rc.readBroadcast(ENEMY_ID);
		}catch(Exception e){
			System.out.println("[ERROR] Enemy Location Read Broadcast Failed");
			Arrays.fill(res, -1);
		}
		return res;
	}
	
	public static void sendInRange(RobotController rc){
		try{
			rc.broadcast(IN_ENEMY_RANGE, 1);
		}catch(Exception e){
			System.out.println("[ERROR] In Range Broadcast Failed");
		}
	}
	
	public static void resetSensed(RobotController rc){
		try{
			rc.broadcast(ENEMY_SENSED, 0);
			rc.broadcast(IN_ENEMY_RANGE, 0);
		}catch(Exception e){
			System.out.println("[ERROR] Sense Reset Failed");
		}
	}
	
	public static void resetScoutsCode(RobotController rc){
		try{
			int n = rc.readBroadcast(SCOUTS_CODE);
			rc.broadcast(SCOUTS_TOTAL, n);
			rc.broadcast(SCOUTS_CODE, 0);
		}catch(Exception e){
			System.out.println("[ERROR] Scouts Code Reset Failed");
		}
	}
	
	public static boolean readSensed(RobotController rc){
		int sensed = 1;
		try{
			if(rc.readBroadcast(SCOUT_FORMATION)==0){
				if(rc.readBroadcast(IN_ENEMY_RANGE)>0){
					sensed = rc.readBroadcast(ENEMY_SENSED);
				}
			}
		}catch(Exception e){
			System.out.println("[ERROR] Read Sense Failed");
		}
		return sensed>0;
	}
	
	public static void setScoutFormation(RobotController rc){
		try{
			int formation = rc.readBroadcast(SCOUT_FORMATION);
			if(formation != 1){
				rc.broadcast(ENEMY_THREAT_LEVEL, 0);
				rc.broadcast(SCOUT_FORMATION, 1);
			}
		} catch (GameActionException e) {
			System.out.println("[ERROR] Scout Formation Message Failed");
		}
	}
	
	public static boolean checkScoutFormation(RobotController rc){
		int sensed = 0;
		try{
			sensed = rc.readBroadcast(SCOUT_FORMATION);
		}catch(Exception e){
			System.out.println("[ERROR] Read Sense Failed");
		}
		return sensed>0;
	}
	
	public static void sendEnemyGone(RobotController rc){
		try{
			rc.broadcast(IN_ENEMY_RANGE, 1);
		} catch (GameActionException e) {
			System.out.println("[ERROR] Enemy Gone Message Failed");
		}
	}
	
	public static boolean checkHead(RobotController rc){
		boolean isHead = true;
		try{
			int currentHead = rc.readBroadcast(HEAD_SWITCHER);
			if(currentHead!=previousHead){
				previousHead = currentHead;
				isHead = false;
			}else{
				previousHead = 1-currentHead;
				rc.broadcast(HEAD_SWITCHER, 1-currentHead);
			}
		} catch (GameActionException e) {
			System.out.println("[ERROR] Head Claim Failed");
		}
		return isHead;
	}
	
	public static float scoutingMission(RobotController rc){
		try{
			int state = rc.readBroadcast(SCOUT_FORMATION);
			scoutNum = rc.readBroadcast(SCOUTS_CODE);
			rc.broadcast(SCOUTS_CODE, ++scoutNum);
			if(state==0){
				return -1;
			}else{
				int total = rc.readBroadcast(SCOUTS_TOTAL);
				float angle = (float) (((float)scoutNum)/((float)total)*Math.PI*2+Math.PI/4);
				return angle;
			}
			
		}catch(Exception e){
			System.out.println("[ERROR] Scouting Mission Transaction Failed");
		}
		return -1;
	}
	
	public static MapLocation getInvalidLocation(RobotController rc){
		try{
			if(rc.readBroadcast(INVALID_LOCATION_SET) == 1){
				return new MapLocation(((float)rc.readBroadcast(INVALID_LOCATION_X))/2, ((float)rc.readBroadcast(INVALID_LOCATION_Y))/2);
			}
		}catch(Exception e){
			System.out.println("[ERROR] Get Invalid Dest Failed");
		}
		return null;
	}
	
	public static void setInvalidLocation(RobotController rc, MapLocation loc){
		try{
			rc.broadcast(INVALID_LOCATION_X, (int) (loc.x*2));
			rc.broadcast(INVALID_LOCATION_Y, (int) (loc.y*2));
			rc.broadcast(INVALID_LOCATION_SET, 1);
		}catch(Exception e){
			System.out.println("[ERROR] Set Invalid Dest Failed");
		}
	}
	
	public static MapLocation getGardenerDest(RobotController rc){
		try{
			if(rc.readBroadcast(GARDENER_DEST_DISTANCE)==Integer.MAX_VALUE){
				return null;
			}
			return new MapLocation(((float)rc.readBroadcast(GARDENER_DEST_X))/2,((float)rc.readBroadcast(GARDENER_DEST_Y))/2);
		}catch(Exception e){
			System.out.println("[ERROR] Get Gardener Destination Failed");
		}
		return null;
	}
	
	public static void setGardenerDest(RobotController rc, MapLocation loc, float distance){
		try{
			if(distance*2 < rc.readBroadcast(GARDENER_DEST_DISTANCE)){
				rc.broadcast(GARDENER_DEST_X, (int) (loc.x*2)); 
				rc.broadcast(GARDENER_DEST_Y, (int) (loc.y*2)); 
				rc.broadcast(GARDENER_DEST_DISTANCE, (int)(distance*2)); 
				rc.broadcast(GARDENER_ALIGN_ID, rc.getID());
			}
		}catch(Exception e){
			System.out.println("[ERROR] Set Gardener Destination Failed");
		}
	}
	
	public static int getAlignID(RobotController rc){
		try{
			return rc.readBroadcast(GARDENER_ALIGN_ID);
		}catch(Exception e){
			System.out.println("[ERROR] Get Gardener Destination Failed");
		}
		return 0;
	}
	
	public static void resetGardenerDest(RobotController rc){
		try{ 
			rc.broadcast(GARDENER_DEST_DISTANCE, Integer.MAX_VALUE); 
			rc.broadcast(GARDENER_MIGRATING, 0);
		}catch(Exception e){
			System.out.println("[ERROR] Reset Gardener Destination Failed");
		}
	}
	
	public static MapLocation getRelativeSafety(RobotController rc){
		try{
			return new MapLocation(rc.readBroadcast(RELATIVE_SAFETY_X),rc.readBroadcast(RELATIVE_SAFETY_Y));
		}catch(Exception e){
			System.out.println("[ERROR] Get Relative Safety Failed");
		}
		return null;
	}
	
	public static void setRelativeSafety(RobotController rc, MapLocation loc){
		try{
			rc.broadcast(RELATIVE_SAFETY_X, (int) loc.x);
			rc.broadcast(RELATIVE_SAFETY_Y, (int) loc.y);
		}catch(Exception e){
			System.out.println("[ERROR] Set Relative Safety Failed");
		}
	}
	
	public static boolean amMigrating(RobotController rc){
		try{
			if(rc.readBroadcast(GARDENER_MIGRATING)==0){
				rc.broadcast(GARDENER_MIGRATING, 1);
				return true;
			}
		}catch(Exception e){
			System.out.println("[ERROR] Migration Activation Failed");
		}
		return false;
	}
	
	public static int checkNumFarmers(RobotController rc, boolean migrated){
		try{
			int count = rc.readBroadcast(MIGRATED_FARMERS);
			if(migrated){
				rc.broadcast(MIGRATED_FARMERS, count+1);
			}else{
				int num = rc.readBroadcast(ON_HOLD_FARMERS);
				rc.broadcast(ON_HOLD_FARMERS, num+1);
			}
			return count;
		}catch(Exception e){
			System.out.println("[ERROR] Farmer Count Failed");
		}
		return -1;
	}
	
	public static void resetNumFarmers(RobotController rc){
		try{
			rc.broadcast(MIGRATED_FARMERS, 0);
			rc.broadcast(ON_HOLD_FARMERS, 0);
		}catch(Exception e){
			System.out.println("[ERROR] Farmer Count Reset Failed");
		}
	}
	
	public static int[] checkFarmers(RobotController rc){
		try{
			int[] ret = {rc.readBroadcast(MIGRATED_FARMERS), rc.readBroadcast(ON_HOLD_FARMERS)};
			return ret;
		}catch(Exception e){
			System.out.println("[ERROR] Farmer Check Failed");
		}
		return new int[]{0,0};
	}
	
	public static void countBuilders(RobotController rc){
		try{
			int num = rc.readBroadcast(BUILDING_GARDENERS);
			rc.broadcast(BUILDING_GARDENERS, num+1);
		}catch(Exception e){
			System.out.println("[ERROR] Builder Count Failed");
		}
	}
	
	public static int numBuilders(RobotController rc){
		try{
			int num = rc.readBroadcast(BUILDING_GARDENERS);
			rc.broadcast(BUILDING_GARDENERS, 0);
			return num;
		}catch(Exception e){
			System.out.println("[ERROR] Builder Reset Failed");
		}
		return 0;
	}
	
	public static void initWalls(RobotController rc){
		try{
			rc.broadcast(EAST_WALL, Integer.MAX_VALUE);
			rc.broadcast(WEST_WALL, Integer.MIN_VALUE);
			rc.broadcast(NORTH_WALL, Integer.MAX_VALUE);
			rc.broadcast(SOUTH_WALL, Integer.MIN_VALUE);
		}catch(Exception e){
			System.out.println("[ERROR] Wall Init Failed");
		}
	}
	
	public static int[] getWalls(RobotController rc){
		try{
			int[] walls = {
					rc.readBroadcast(EAST_WALL),
					rc.readBroadcast(WEST_WALL),
					rc.readBroadcast(NORTH_WALL),
					rc.readBroadcast(SOUTH_WALL),
			};
			return walls;
		}catch(Exception e){
			System.out.println("[ERROR] Wall Init Failed");
		}
		return new int[0];
	}
	
	public static void setWall(RobotController rc, float pos, String dir){
		try{
			switch(dir){
			case "EAST":
				if(rc.readBroadcast(EAST_WALL)>(int) pos+1){
					rc.broadcast(EAST_WALL, (int) pos+1);
				}
				break;
			case "WEST":
				if(rc.readBroadcast(WEST_WALL)<(int) pos){
					rc.broadcast(WEST_WALL, (int) pos);
				}
				break;
			case "NORTH":
				if(rc.readBroadcast(NORTH_WALL)>(int) pos+1){
					rc.broadcast(NORTH_WALL, (int) pos+1);
				}
				break;
			case "SOUTH":
				if(rc.readBroadcast(SOUTH_WALL)<(int) pos+1){
					rc.broadcast(SOUTH_WALL, (int) pos);
				}
				break;
			default:
				System.out.println("[ERROR] Wall Dir Invalid");
			}
		}catch(Exception e){
			System.out.println("[ERROR] Wall Init Failed");
		}
	}
	
	public static void setAllyLocation(RobotController rc, MapLocation ml){
		try{
			rc.broadcast(ALLY_LOCATION_X, (int) ml.x); 
			rc.broadcast(ALLY_LOCATION_Y, (int) ml.y); 
		}catch(Exception e){
			System.out.println("[ERROR] Ally Location Set Failed");
		}
	}
	
	public static MapLocation getAllyLocation(RobotController rc){
		try{
			return new MapLocation(rc.readBroadcast(ALLY_LOCATION_X), rc.readBroadcast(ALLY_LOCATION_Y)); 
		}catch(Exception e){
			System.out.println("[ERROR] Ally Location Set Failed");
		}
		return null;
	}
}
