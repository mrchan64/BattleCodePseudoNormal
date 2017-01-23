package playerbeta;

import battlecode.common.*;

public class Line {
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
	
	/*public MapLocation intersect(BodyInfo bi){
		MapLocation circle = bi.getLocation();
		float radius = bi.getRadius();
		float a = 1+m*m;
		float b = -2*(circle.x+m*(circle.y+m*x-y));
		float cpoint5 = m*x-y+circle.y;
		float c = m*m*x*x+cpoint5*cpoint5-radius*radius;
		float det = b*b-4*a*c;
		if(det<=0){
			return null;
		}
		float detsq = (float) Math.sqrt(det);
		float x1 = 
	}*/
}
