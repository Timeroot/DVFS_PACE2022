package JNA_SCIP;

import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

public class SCIP extends PointerType {
	public SCIP() {}
	public SCIP(Pointer p) {
		super(p);
	}
}

//@FieldOrder({ "set", "interrupt", "dialoghdlr", "messagehdlr", "totaltime", "state" })
//public class SCIP extends Structure {
//	
//	public SCIP() {
//		super();
//	}
//	public SCIP(Pointer pointer) {
//		super(pointer);
//	}
	
//	public static class ByReference extends SCIP implements Structure.ByReference {}

//	public SCIP_MEM.ByReference mem;
//	public Pointer set;
//	public Pointer interrupt;
//	public Pointer dialoghdlr;
//	public Pointer messagehdlr;
//	public Pointer totaltime;
//	public Pointer state;
	// etc.
//}