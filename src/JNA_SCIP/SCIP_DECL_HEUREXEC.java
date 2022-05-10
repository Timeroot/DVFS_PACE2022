package JNA_SCIP;

import com.sun.jna.Callback;
import com.sun.jna.TypeMapper;
import com.sun.jna.ptr.IntByReference;

public interface SCIP_DECL_HEUREXEC extends Callback {
	public final static TypeMapper TYPE_MAPPER = JSCIP.TYPE_MAPPER;
	/* Allowed return values: SCIP_FOUNDSOL, SCIP_DIDNOTFIND, SCIP_DIDNOTRUN, SCIP_DELAYED */
    SCIP_RETCODE heurexec(SCIP scip, SCIP_HEUR heur, SCIP_HEURTIMING heurtiming, boolean nodeinfeasible, IntByReference scip_result);
}
