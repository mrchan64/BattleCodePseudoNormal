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
		Direction gardDir = new Direction(dir);
		try{
			if(rc.canBuildRobot(RobotType.GARDENER, gardDir)){
				System.out.println("Building Gardener");
				rc.buildRobot(RobotType.GARDENER, gardDir);
			}
			Clock.yield();
		}catch(Exception e){
			e.printStackTrace();
		}
		while(true){
			try{
				
				Clock.yield();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	public static void runGardener(){

		float dir = (float)Math.PI;
		Direction treeDir = new Direction(0);
		Direction scoutDir = new Direction(dir);
		while(true){
			try{
				if(rc.canPlantTree(treeDir)){
					System.out.println("Planting Tree");
					rc.plantTree(treeDir);
				}
				if(rc.canBuildRobot(RobotType.SCOUT, scoutDir)){
					System.out.println("Building Scout");
					rc.buildRobot(RobotType.SCOUT, scoutDir);
				}
				TreeInfo[] trees = rc.senseNearbyTrees();
				for(int i = 0; i<trees.length; i++){
					if(rc.canWater()){
						System.out.println("Watering Tree");
						rc.water(trees[i].ID);
					}
				}
				System.out.println(rc.getTeamBullets());
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
