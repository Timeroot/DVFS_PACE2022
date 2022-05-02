package JNA_SCIP;

import com.sun.jna.Pointer;
import com.sun.jna.PointerType;

public class SCIP_CONSHDLR extends PointerType {
	public SCIP_CONSHDLR() {}
	public SCIP_CONSHDLR(Pointer p) {
		super(p);
	}
	
//    public long nsepacalls;
//    public long nenfolpcalls;  
    //etc.
}
