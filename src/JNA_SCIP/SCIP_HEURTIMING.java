package JNA_SCIP;

import com.sun.jna.FromNativeContext;
import com.sun.jna.ToNativeContext;
import com.sun.jna.TypeConverter;

public class SCIP_HEURTIMING {
	/* This actually an int defined by flags, and there are are 2048 possible values. We could enumerate them all,
	 * or use ints (leaving code largely unreadable). Instead we'll just make a (non-enum) class.
	 */
	static private final SCIP_HEURTIMING[] cache = new SCIP_HEURTIMING[0x800];
	
	public final int flags;
	private SCIP_HEURTIMING(int flags) {
		this.flags = flags;
	}
	public static SCIP_HEURTIMING of(int flags) {
		if(flags < 0 || flags >= 0x800)
			throw new RuntimeException("Bad SCIP_HEURTIMING, should be in the range 0 to 0x7FF (2047), was "+flags);
		if(cache[flags] == null)
			cache[flags] = new SCIP_HEURTIMING(flags);
		return cache[flags];
	}
	public SCIP_HEURTIMING not() {
		return of(~this.flags);
	}
	public SCIP_HEURTIMING or(SCIP_HEURTIMING other) {
		return of(this.flags | other.flags);
	}
	public String toString() {return "HEURTIMING{"+flags+"}";}
	
	public static final SCIP_HEURTIMING BEFORENODE = of(0x1);
	public static final SCIP_HEURTIMING DURINGLPLOOP = of(0x2);
	public static final SCIP_HEURTIMING AFTERLPLOOP = of(0x4);
	public static final SCIP_HEURTIMING AFTERLPNODE = of(0x8);
	public static final SCIP_HEURTIMING AFTERPSEUDONODE = of(0x10);
	public static final SCIP_HEURTIMING AFTERLPPLUNGE = of(0x20);
	public static final SCIP_HEURTIMING AFTERPSEUDOPLUNGE = of(0x40);
	public static final SCIP_HEURTIMING DURINGPRICINGLOOP = of(0x80);
	public static final SCIP_HEURTIMING BEFOREPRESOL = of(0x100);
	public static final SCIP_HEURTIMING DURINGPRESOLLOOP = of(0x200);
	public static final SCIP_HEURTIMING AFTERPROPLOOP = of(0x400);
	
	public static final SCIP_HEURTIMING AFTERNODE = AFTERLPNODE.or(AFTERPSEUDONODE);
	public static final SCIP_HEURTIMING AFTERPLUNGE = AFTERLPPLUNGE.or(AFTERPSEUDOPLUNGE);
	
	public static final SCIP_HEURTIMING NEVER = of(0);
	public static final SCIP_HEURTIMING ALWAYS = of(0x7FF);


	public static class HEURTIMING_Converter implements TypeConverter {
		// Singleton
		public static final HEURTIMING_Converter inst = new HEURTIMING_Converter();

		private HEURTIMING_Converter() {}

		@Override
		public SCIP_HEURTIMING fromNative(Object input, FromNativeContext context) {
			return SCIP_HEURTIMING.of((Integer)input);
		}

		@Override
		public Integer toNative(Object input, ToNativeContext context) {
			return ((SCIP_HEURTIMING) input).flags;
		}

		@Override
		public Class<Integer> nativeType() {
			return int.class;
		}
	}
}
