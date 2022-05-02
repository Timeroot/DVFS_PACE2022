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

/*
public class SCIP extends Structure implements Structure.ByReference {
public SCIP() {}
public SCIP(Pointer p) {
	super(p);
}

@Override
protected List<String> getFieldOrder() {
	//Be lazy and assume the compiler gives it in the right order...
	return Stream.of(SCIP.class.getDeclaredFields()).filter(
		x -> (x.getModifiers() & (Modifier.PUBLIC | Modifier.STATIC)) == Modifier.PUBLIC
		).map(Field::getName).collect(Collectors.toList());
}

public Pointer mem;//SCIP_MEM.ByReference
public Pointer set;
public Pointer interrupt;
public Pointer dialoghdlr;
public Pointer messagehdlr;
public Pointer totaltime;
public Pointer stat;
// etc.
}
*/