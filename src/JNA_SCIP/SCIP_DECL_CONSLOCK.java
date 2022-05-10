package JNA_SCIP;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;
import com.sun.jna.TypeMapper;
import com.sun.jna.ptr.IntByReference;

public interface SCIP_DECL_CONSLOCK extends Callback {
	public final static TypeMapper TYPE_MAPPER = JSCIP.TYPE_MAPPER;
	//returns SCIP_RETCODE
    SCIP_RETCODE conslock(SCIP scip, SCIP_CONSHDLR conshdlr, SCIP_CONS cons,
    		SCIP_LOCKTYPE locktype, int nlockspos, int nlocksneg);
}