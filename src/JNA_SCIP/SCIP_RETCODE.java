package JNA_SCIP;

import com.sun.jna.FromNativeContext;
import com.sun.jna.ToNativeContext;
import com.sun.jna.TypeConverter;

public enum SCIP_RETCODE {
   SCIP_OKAY,
   SCIP_ERROR,
   SCIP_NOMEMORY,
   SCIP_READERROR,
   SCIP_WRITEERROR,
   SCIP_NOFILE,
   SCIP_FILECREATEERROR,
   SCIP_LPERROR,
   SCIP_NOPROBLEM,
   SCIP_INVALIDCALL,
   SCIP_INVALIDDATA,
   SCIP_INVALIDRESULT,
   SCIP_PLUGINNOTFOUND,
   SCIP_PARAMETERUNKNOWN,
   SCIP_PARAMETERWRONGTYPE,
   SCIP_PARAMETERWRONGVAL,
   SCIP_KEYALREADYEXISTING,
   SCIP_MAXDEPTHLEVEL,
   SCIP_BRANCHERROR,
   SCIP_NOTIMPLEMENTED;
	// These do NOT obey normal numbering. SCIP_OKAY = +1, SCIP_ERROR = 0, and
	// so on down to SCIP_NOTIMPLEMENTED = -18. As such, JNA needs a custom
	// "converter" to understand these.
	
	public int toNative() {
		return 1 - this.ordinal();
	}
	
	static SCIP_RETCODE fromNative(int i) {
		return SCIP_RETCODE.values()[1 - i];
	}

	public static class RETCODE_Converter implements TypeConverter {
		// Singleton
		public static final RETCODE_Converter inst = new RETCODE_Converter();

		private RETCODE_Converter() {}

		@Override
		public SCIP_RETCODE fromNative(Object input, FromNativeContext context) {
			return SCIP_RETCODE.fromNative((Integer)input);
		}

		@Override
		public Integer toNative(Object input, ToNativeContext context) {
			return ((SCIP_RETCODE) input).toNative();
		}

		@Override
		public Class<Integer> nativeType() {
			return int.class;
		}
	}
}
