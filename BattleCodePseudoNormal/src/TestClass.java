import java.util.Arrays;


public class TestClass {

	public static void main(String[] args) {
		System.out.println(Arrays.toString(args));
		
	}
	
	public static void remove(int[] arg){
		arg = new int[arg.length-1];
		System.out.println(arg.length);
	}
	
	public static Object[] remove(Object[] arg, int index){
		Object[] newArg = new Object[arg.length - 1];
		for(int i = 0; i<index; i++){
			newArg[i] = arg[i];
		}
		for(int i = index; i<newArg.length; i++){
			newArg[i] = arg[i+1];
		}
		return newArg;
	}

}
