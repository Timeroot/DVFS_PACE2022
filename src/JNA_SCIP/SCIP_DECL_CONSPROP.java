package JNA_SCIP;

import com.sun.jna.Callback;
import com.sun.jna.TypeMapper;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

public interface SCIP_DECL_CONSPROP  extends Callback {
	public final static TypeMapper TYPE_MAPPER = JSCIP.TYPE_MAPPER;
	SCIP_RETCODE consprop(SCIP scip, SCIP_CONSHDLR conshdlr, Pointer conss, int nconss, int nusefulconss,
		      int nmarkedconss, SCIP_PROPTIMING proptiming, IntByReference result);
}
