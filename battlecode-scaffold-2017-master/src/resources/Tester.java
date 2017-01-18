package resources;
import battlecode.common.*;

public class Tester {

	public static void main(String[] args) {
		MapLocation ml1 = new MapLocation(62.221867F, 505.617188F);
		MapLocation ml2 = new MapLocation(61.084831F, 501.669647F);
		Direction close = ml2.directionTo(ml1);
		float dist = ml2.distanceTo(ml1);
		float dir = (float) Math.atan((ml1.y-ml2.y)/(ml1.x-ml2.x));
		MapLocation ml3 = ml2.add(dir, dist);
		System.out.println(ml3);

	}

}
