package JNA_SCIP;

import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.Structure;

public class SCIP_ROW extends PointerType {

	public SCIP_ROW() {}
	public SCIP_ROW(Pointer p) {
		super(p);
	}
	
}
