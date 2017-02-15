import java.util.Arrays;


public class TestClass {

	public static void main(String[] args) {
		for(int i = -100; i<100; i++){
			for(int j = -100; j<100; j++){
				for(int k = -100; k<100; k++){
					int a = i;
					int b = j;
					int c = k;
					int value1 = not(a) | not(b++) & c;
					int value2 = ((a - 1) | (b/2)) & (c*=2);
					int value3 = (a-- | --b) & (c+=2);
					int value4 = a | not(b & --c);
					if(value1 == 0 && value2 == 0 && value3 == 0 && value4 == 0){
						System.out.println(i+" "+j+" "+k);
					}
				}
			}
		}
		Float.par
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
	
	public static int not(int input){
		if(input == 0){
			return 1;
		}else{
			return 0;
		}
	}

}
