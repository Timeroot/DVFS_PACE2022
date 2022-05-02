package JNA_SCIP;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;
import com.sun.jna.TypeMapper;
import com.sun.jna.ptr.IntByReference;

public interface SCIP_DECL_CONSCHECK extends Callback {
	public final static TypeMapper TYPE_MAPPER = JSCIP.scip_TypeMapper;
	//returns SCIP_RETCODE
    SCIP_RETCODE invoke (SCIP scip, SCIP_CONSHDLR conshdlr, Pointer conss,//SCIP_CONS**
    	int nconss,
    	SCIP_SOL sol, boolean checkintegrality, boolean checklprows, boolean printreason, boolean completely,
    	IntByReference scip_result//SCIP_RESULT*
    );
}