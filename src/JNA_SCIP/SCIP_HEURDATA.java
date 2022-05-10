package JNA_SCIP;

import com.sun.jna.Pointer;
import com.sun.jna.PointerType;

public class SCIP_HEURDATA extends PointerType {
	public SCIP_HEURDATA() {}
	public SCIP_HEURDATA(Pointer p) {
		super(p);
	}

}
