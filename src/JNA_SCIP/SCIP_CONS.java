package JNA_SCIP;

import com.sun.jna.Pointer;
import com.sun.jna.PointerType;

public class SCIP_CONS extends PointerType {
	public SCIP_CONS() {}
	public SCIP_CONS(Pointer p) {
		super(p);
	}
//	static public SCIP_CONS[] load_arr(Pointer p/* SCIP_CONS** */, int ncons){
//		SCIP_CONS[] arr = new  SCIP_CONS[ncons];
//		for(int i=0; i<ncons; i++)
//			arr[i] = new SCIP_CONS(p.getPointer(8 * i));
//		return arr;
//	}
	
//    public double age;
//    public String name;
    //etc.
}
