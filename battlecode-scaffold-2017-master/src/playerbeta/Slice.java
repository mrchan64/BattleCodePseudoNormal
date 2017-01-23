package playerbeta;

import battlecode.common.*;

public class Slice {
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
	
	/**
	 * Returns the closest edge of the slice.  This method won't work properly if you don't
	 * check for slice.contains() beforehand.
	 * 
	 * @param dir Direction in between open and close of slice
	 * @return Closest edge of slice
	 */
	public Direction round(Direction dir){
		if(Math.abs(open.radiansBetween(dir))<Math.abs(close.radiansBetween(dir))){
			return open;
		}else{
			return close;
		}
	}
	
	public static Slice[] combine(Slice[] a, Slice[] b){
		Slice[] ret = new Slice[a.length+b.length];
		for(int i = 0; i<a.length; i++){
			ret[i] = a[i];
		}
		int mark = a.length;
		for(int i = 0; i<b.length; i++){
			ret[mark+i] = b[i];
		}
		ret = simplify(ret);
		return ret;
	}
	
	public static Slice[] simplify(Slice[] avoid){
		for(int i = 0; i<avoid.length; i++){
			for(int j = i+1; j<avoid.length; j++){
				if(avoid[i].add(avoid[j])){
					avoid = remove(avoid, j);
					j = i;
				}
			}
		}
		return avoid;
	}
	
	private static Slice[] remove(Slice[] arg, int index){
		Slice[] newArg = new Slice[arg.length - 1];
		for(int i = 0; i<index; i++){
			newArg[i] = arg[i];
		}
		for(int i = index; i<newArg.length; i++){
			newArg[i] = arg[i+1];
		}
		return newArg;
	}
}
