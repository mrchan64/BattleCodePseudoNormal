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
			System.out.println("Total" +n);
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
}
