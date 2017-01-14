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
	
	public static int previousHead = 1;
	
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
	
	public static void resetSensed(RobotController rc){
		try{
			rc.broadcast(ENEMY_SENSED, 0);
			rc.broadcast(IN_ENEMY_RANGE, 0);
		}catch(Exception e){
			System.out.println("[ERROR] Sense Reset Failed");
		}
	}
	
	public static boolean readSensed(RobotController rc){
		int sensed = 0;
		try{
			if(rc.readBroadcast(IN_ENEMY_RANGE)>0){
				sensed = rc.readBroadcast(ENEMY_SENSED);
			}
		}catch(Exception e){
			System.out.println("[ERROR] Read Sense Failed");
		}
		return sensed>0;
	}
	
	public static void setScoutFormation(RobotController rc){
		try{
			rc.broadcast(ENEMY_THREAT_LEVEL, 0);
			rc.broadcast(SCOUT_FORMATION, 1);
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
				isHead = false;
			}else{
				rc.broadcast(HEAD_SWITCHER, 1-currentHead);
			}
			previousHead = currentHead;
		} catch (GameActionException e) {
			System.out.println("[ERROR] Head Claim Failed");
		}
		return isHead;
	}
}
