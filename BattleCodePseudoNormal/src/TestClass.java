
public class TestClass {

	public static void main(String[] args) {
		String[] test = new String[4];
		System.out.println(test.length);
		test = (String[]) remove(test, 2);
		System.out.println(test.length);
		System.out.println(test instanceof String[]);
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
