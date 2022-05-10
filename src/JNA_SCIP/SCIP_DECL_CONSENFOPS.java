package JNA_SCIP;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;
import com.sun.jna.TypeMapper;
import com.sun.jna.ptr.IntByReference;

public interface SCIP_DECL_CONSENFOPS extends Callback {
	public final static TypeMapper TYPE_MAPPER = JSCIP.TYPE_MAPPER;
	//returns SCIP_RETCODE
    SCIP_RETCODE consenfops(SCIP scip, SCIP_CONSHDLR conshdlr, Pointer conss,//SCIP_CONS**
    	int nconss,
    	int nusefulconss, boolean solinfeasible, boolean objinfeasible,
    	IntByReference scip_result//SCIP_RESULT*
    );
}
