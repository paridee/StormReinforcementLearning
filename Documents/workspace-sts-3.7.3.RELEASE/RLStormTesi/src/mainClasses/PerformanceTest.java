package mainClasses;
/**
 * Performance test for CPU benchmarking
 * @author Paride
 *
 */

@Deprecated
public class PerformanceTest {
    public static long fibonacci(long i) {
	/* F(i) non e` definito per interi i negativi! */
    	if (i == 0) return 0;
		else if (i == 1) return 1;
		else return fibonacci(i-1) + fibonacci(i-2);
    }
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		for(int i=35;i<45;i++){
			long total	=	0;
			for(int j=0;j<20;j++){
				long start	=	System.currentTimeMillis();
				long res	=	fibonacci(i);
				long end	=	System.currentTimeMillis();
				total	=total+end-start;
			}
			System.out.println(i+" res: "+(double)total/20);
			
		}
	}

}