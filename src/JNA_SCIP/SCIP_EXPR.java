package JNA_SCIP;

import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.Structure;

public class SCIP_EXPR extends PointerType {

	public SCIP_EXPR() {}
	public SCIP_EXPR(Pointer p) {
		super(p);
	}
	
//	public static class ByRef extends SCIP_EXPR implements ByReference {}
//	
//	public Pointer exprhdlr;
//	public Pointer exprdata;
//	
//	int nchildren;
//    int childrensize;
//    SCIP_EXPR[] children;
//    
//    int nuses;
    //etc.
}
