package JNA_SCIP;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;
import com.sun.jna.TypeMapper;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

public interface SCIP_DECL_CONSDELETE extends Callback {
	public final static TypeMapper TYPE_MAPPER = JSCIP.scip_TypeMapper;
	//returns SCIP_RETCODE
    SCIP_RETCODE invoke (SCIP scip, SCIP_CONSHDLR conshdlr, SCIP_CONS cons,
    	PointerByReference consdata//SCIP_CONSDATA**
    );
}
