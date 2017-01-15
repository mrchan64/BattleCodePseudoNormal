package playeralpha;

import battlecode.common.*;

public class RobotPlayer {
	public static RobotController rc;
	
	@SuppressWarnings("unused")
	public static void run(RobotController rc) throws GameActionException {
		RobotPlayer.rc = rc;
		switch (rc.getType()) {
        case ARCHON:
        	runArchon();
        	break;
        case GARDENER:
        	runGardener();
        	break;
        case SCOUT:
        	Scout scout = new Scout(rc);
        	scout.go();
		}
	}
	
	public static void runArchon() throws GameActionException{
		float dir = (float)Math.PI*.5F;
		float dir2 = (float)(-1*Math.PI*.5F);
		float dir3 = 0;
		Direction gardDir = new Direction(dir);
		Direction gardDir2 = new Direction(dir2);
		Direction gardDir3 = new Direction(dir3);
		BroadcastSystem.setScoutFormation(rc);
		while(true){
			try{
				boolean isHead = BroadcastSystem.checkHead(rc);
				if(isHead){
					if(!BroadcastSystem.readSensed(rc)){
						BroadcastSystem.setScoutFormation(rc);
					}
					BroadcastSystem.resetScoutsCode(rc);
					BroadcastSystem.resetSensed(rc);
				}
				if(rc.canBuildRobot(RobotType.GARDENER, gardDir)){
					System.out.println("Building Gardener dir 1");
					rc.buildRobot(RobotType.GARDENER, gardDir);
				}else if(rc.canBuildRobot(RobotType.GARDENER, gardDir2)){
					System.out.println("Building Gardener dir 2");
					rc.buildRobot(RobotType.GARDENER, gardDir2);
				}else if(rc.canBuildRobot(RobotType.GARDENER, gardDir3)){
					System.out.println("Building Gardener dir 3");
					rc.buildRobot(RobotType.GARDENER, gardDir3);
				}
				Clock.yield();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	public static void runGardener(){

		float dir = (float)Math.PI*.5F;
		float dir2 = (float)(-1*Math.PI*.5F);
		float dir3 = 0;
		float dir4 = (float)Math.PI;
		Direction treeDir = new Direction(dir);
		Direction treeDir2 = new Direction(dir2);
		Direction treeDir3 = new Direction(dir3);
		Direction scoutDir = new Direction(dir4);
		while(true){
			try{
				if(rc.canPlantTree(treeDir)){
					rc.plantTree(treeDir);
				}else if(rc.canPlantTree(treeDir2)){
					rc.plantTree(treeDir2);
				}else if(rc.canPlantTree(treeDir3)){
					rc.plantTree(treeDir3);
					
				}
				if(rc.getRoundNum()>200 && rc.canBuildRobot(RobotType.SCOUT, scoutDir)){
					System.out.println("Building Scout");
					rc.buildRobot(RobotType.SCOUT, scoutDir);
				}
				TreeInfo[] trees = rc.senseNearbyTrees();
				for(int i = 0; i<trees.length; i++){
					if(trees[i].maxHealth-trees[i].health >= GameConstants.WATER_HEALTH_REGEN_RATE){
						if(rc.canWater(trees[i].ID)){
							rc.water(trees[i].ID);
						}
					}
				}
				Clock.yield();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
    static boolean tryMove(Direction dir) throws GameActionException {
        return tryMove(dir,20,3);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles direction in the path.
     *
     * @param dir The intended direction of movement
     * @param degreeOffset Spacing between checked directions (degrees)
     * @param checksPerSide Number of extra directions checked on each side, if intended direction was unavailable
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {

        // First, try intended direction
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        }

        // Now try a bunch of similar angles
        boolean moved = false;
        int currentCheck = 1;

        while(currentCheck<=checksPerSide) {
            // Try the offset of the left side
            if(rc.canMove(dir.rotateLeftDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateLeftDegrees(degreeOffset*currentCheck));
                return true;
            }
            // Try the offset on the right side
            if(rc.canMove(dir.rotateRightDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateRightDegrees(degreeOffset*currentCheck));
                return true;
            }
            // No move performed, try slightly further
            currentCheck++;
        }

        // A move never happened, so return false.
        return false;
    }
	
}
