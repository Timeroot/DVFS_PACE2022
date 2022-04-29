package JNA_SCIP;

import com.sun.jna.Pointer;
import com.sun.jna.PointerType;

public class SCIP_SOL extends PointerType {
	public SCIP_SOL() {}
	public SCIP_SOL(Pointer p) {
		super(p);
	}
}
