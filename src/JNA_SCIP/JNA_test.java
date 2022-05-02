package JNA_SCIP;

import com.sun.jna.Library;
import com.sun.jna.Native;

public class JNA_test {
	static CLibrary INSTANCE;
	public static void main(String[] args) {
		System.out.println("Going to load library...");
		INSTANCE = (CLibrary)Native.loadLibrary("c", CLibrary.class);
		System.out.println("Loaded "+INSTANCE);
		int val = INSTANCE.atol("51") + 10;
		System.out.println("Computed 51+10==" + val);
	}
	
	public interface CLibrary extends Library {
	    int atol(String s);
	}
}
