package JNA_SCIP;

import com.sun.jna.Pointer;
import com.sun.jna.PointerType;

public class SCIP_MEM extends PointerType {

	public SCIP_MEM() {}
	public SCIP_MEM(Pointer p) {
		super(p);
	}

//	public Pointer setmem;
//	public Pointer probmem;
//	public Pointer buffer;
//	public Pointer cleanbuffer;
}