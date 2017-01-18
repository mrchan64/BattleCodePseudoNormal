package playeralpha;

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
}
