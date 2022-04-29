package JNA_SCIP;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

public interface JSCIP extends Library {
	static JSCIP LIB = (JSCIP)Native.load("scip", JSCIP.class);
	static PointerByReference pref = new PointerByReference();
	static SCIP scip = new SCIP();
	
	/* cons_linear.h */
	int SCIPcreateConsBasicLinear (SCIP scip, PointerByReference cons, String name,
			int nvars, SCIP_VAR[] vars, double[] vals, double lhs, double rhs);
	static void CALL_SCIPcreateConsBasicLinear(SCIP scip, SCIP_CONS cons, String name,
			int nvars, SCIP_VAR[] vars, double[] vals, double lhs, double rhs) {
		int retcode = LIB.SCIPcreateConsBasicLinear(scip, pref, name, nvars, vars, vals, lhs, rhs);
		if(retcode != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+retcode);
		cons.setPointer(pref.getValue());
	}
	static SCIP_CONS createConsBasicLinear(String name,
			int nvars, SCIP_VAR[] vars, double[] vals, double lhs, double rhs) {
		SCIP_CONS cons = new SCIP_CONS();
		CALL_SCIPcreateConsBasicLinear(scip, cons, name, nvars, vars, vals, lhs, rhs);
		return cons;
	}
	
	int SCIPaddCoefLinear(SCIP scip, SCIP_CONS cons, SCIP_VAR var, double val);
	static void CALL_SCIPaddCoefLinear(SCIP scip, SCIP_CONS cons, SCIP_VAR var, double val) {
		int retcode = LIB.SCIPaddCoefLinear(scip, cons, var, val);
		if(retcode != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+retcode);
	}
	static void addCoefLinear(SCIP_CONS cons, SCIP_VAR var, double val) {
		CALL_SCIPaddCoefLinear(scip, cons, var, val);
	}
	
	/* expr_var.h */
	int SCIPcreateExprVar(SCIP scip, PointerByReference expr, SCIP_VAR var,
			Pointer ownercreate, Pointer ownercreatedata);
	static void CALL_SCIPcreateExprVar(SCIP scip, SCIP_EXPR expr, SCIP_VAR var,
			Pointer ownercreate, Pointer ownercreatedata) {
		int retcode = LIB.SCIPcreateExprVar(scip, pref, var, ownercreate, ownercreatedata);
		if(retcode != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+retcode);
		expr.setPointer(pref.getValue());
	}
	static SCIP_EXPR createExprVar(SCIP_VAR var, Pointer ownercreate, Pointer ownercreatedata) {
		SCIP_EXPR expr = new SCIP_EXPR();
		CALL_SCIPcreateExprVar(scip, expr, var, ownercreate, ownercreatedata);
		return expr;
	}
	
	/* pub_message.h */
	void SCIPmessagePrintError(String fmt, Object... vals);
	
	/* scipdefplugins.h */
	int SCIPincludeDefaultPlugins(SCIP scip);
	static void CALL_SCIPincludeDefaultPlugins(SCIP scip) {
		int retcode = LIB.SCIPincludeDefaultPlugins(scip);
		if(retcode != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+retcode);
	}
	static void includeDefaultPlugins() { CALL_SCIPincludeDefaultPlugins(scip); }
	
	/* scip_cons.h */
	int SCIPreleaseCons(SCIP scip, PointerByReference cons);
	static void CALL_SCIPreleaseCons(SCIP scip, SCIP_CONS cons) {
		pref.setValue(cons.getPointer());
		int retcode = LIB.SCIPreleaseCons(scip, pref);
		if(retcode != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+retcode);
		cons.setPointer(pref.getValue());
	}
	static void releaseCons(SCIP_CONS cons) { CALL_SCIPreleaseCons(scip, cons); }
	
	/* scip_dialog.h */
	int SCIPstartInteraction(SCIP scip);
	static void CALL_SCIPstartInteraction(SCIP scip) {
		int retcode = LIB.SCIPstartInteraction(scip);
		if(retcode != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+retcode);
	};
	static void startInteraction() { CALL_SCIPstartInteraction(scip); }
	
	/* scip_general.h */
    int SCIPcreate(PointerByReference scip);//SCIP_RETCODE
    static void CALL_SCIPcreate(SCIP scip) {
		int retcode = LIB.SCIPcreate(pref);
		if(retcode != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+retcode);
		scip.setPointer(pref.getValue());
    }
    static void create() {
    	if(scip.getPointer() != null)
    		throw new RuntimeException("Called create() when scip was already nonnull, call free() first.");
    	CALL_SCIPcreate(scip);
    }
    
    int SCIPfree(PointerByReference scip);//SCIP_RETCODE
    static void CALL_SCIPfree(SCIP scip) {
    	pref.setValue(scip.getPointer());
		int retcode = LIB.SCIPfree(pref);
		if(retcode != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+retcode);
		scip.setPointer(pref.getValue());
    }
    static void free() { CALL_SCIPfree(scip); }
    
	int SCIPprintVersion(SCIP scip, Pointer file);
	static void CALL_SCIPprintVersion(SCIP scip, Pointer file) {
		int retcode = LIB.SCIPprintVersion(scip, file);
		if(retcode != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+retcode);
	}
	static void printVersion(Pointer file) { CALL_SCIPprintVersion(scip, file); }
	
	int SCIPgetStatus(SCIP scip);
	static SCIP_STATUS CALL_SCIPgetStatus(SCIP scip) {
		int status = LIB.SCIPgetStatus(scip);
		return SCIP_STATUS.values()[status];
	}
	static SCIP_STATUS getStatus() { return CALL_SCIPgetStatus(scip); }
	
	/* scip_message.h */
	void SCIPinfoMessage(SCIP sc, Pointer file, String formatstr, Object... vals);
	static void infoMessage(Pointer file, String formatstr, Object... vals) {
		LIB.SCIPinfoMessage(scip, file, formatstr, vals);
	}

	int SCIPgetVerbLevel(SCIP scip);//SCIP_VERBLEVEL
	static SCIP_VERBLEVEL CALL_SCIPgetVerbLevel(SCIP scip) {
		int verblevel = LIB.SCIPgetVerbLevel(scip);
		return SCIP_VERBLEVEL.values()[verblevel];
	};
	static SCIP_VERBLEVEL getVerbLevel() { return CALL_SCIPgetVerbLevel(scip); };
	
	/* scip_numerics.h */
	double SCIPinfinity(SCIP scip);
	static double infinity() { return LIB.SCIPinfinity(scip); }
	
	/* scip_param.h */
	int SCIPsetRealParam(SCIP scip, String name, double value);
	static void CALL_SCIPsetRealParam(SCIP scip, String name, double value) {
		int retcode = LIB.SCIPsetRealParam(scip, name, value);
		if(retcode != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+retcode);
	};
	static void setRealParam(String name, double value) { CALL_SCIPsetRealParam(scip, name, value); }
	
	int SCIPsetCharParam(SCIP scip, String name, byte value);
	static void CALL_SCIPsetCharParam(SCIP scip, String name, byte value) {
		int retcode = LIB.SCIPsetCharParam(scip, name, value);
		if(retcode != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+retcode);
	};
	static void setCharParam(String name, byte value) { CALL_SCIPsetCharParam(scip, name, value); }

	int SCIPsetIntParam(SCIP scip, String name, int value);
	static void CALL_SCIPsetIntParam(SCIP scip, String name, int value) {
		int retcode = LIB.SCIPsetIntParam(scip, name, value);
		if(retcode != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+retcode);
	};
	static void setIntParam(String name, int value) { CALL_SCIPsetIntParam(scip, name, value); }

	int SCIPsetLongintParam(SCIP scip, String name, long value);
	static void CALL_SCIPsetLongintParam(SCIP scip, String name, long value) {
		int retcode = LIB.SCIPsetLongintParam(scip, name, value);
		if(retcode != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+retcode);
	};
	static void setLongintParam(String name, long value) { CALL_SCIPsetLongintParam(scip, name, value); }
	
	int SCIPsetStringParam(SCIP scip, String name, String value);
	static void CALL_SCIPsetStringParam(SCIP scip, String name, String value) {
		int retcode = LIB.SCIPsetStringParam(scip, name, value);
		if(retcode != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+retcode);
	};
	static void setStringParam(String name, String value) { CALL_SCIPsetStringParam(scip, name, value); }
	
	int SCIPsetEmphasis(SCIP scip, int SCIP_PARAMEMPHASIS, boolean quiet);
	static void CALL_SCIPsetEmphasis(SCIP scip, SCIP_PARAMEMPHASIS emph, boolean quiet) {
		int retcode = LIB.SCIPsetEmphasis(scip, emph.ordinal(), quiet);
		if(retcode != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+retcode);
	};
	static void setEmphasis(SCIP_PARAMEMPHASIS emph, boolean quiet) { CALL_SCIPsetEmphasis(scip, emph, quiet); }
	
	int SCIPsetPresolving(SCIP scip, int SCIP_PARAMSETTING, boolean quiet);
	static void CALL_SCIPsetPresolving(SCIP scip, SCIP_PARAMSETTING emph, boolean quiet) {
		int retcode = LIB.SCIPsetPresolving(scip, emph.ordinal(), quiet);
		if(retcode != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+retcode);
	};
	static void setPresolving(SCIP_PARAMSETTING emph, boolean quiet) { CALL_SCIPsetPresolving(scip, emph, quiet); }
	
	
	/* scip_prob.h */
	int SCIPreadProb(SCIP scip, String filename, String ext);
	static void CALL_SCIPreadProb(SCIP scip, String filename, String ext) {
		int retcode = LIB.SCIPreadProb(scip, filename, ext);
		if(retcode != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+retcode);
	}
	static void readProb(String filename, String ext) { CALL_SCIPreadProb(scip, filename, ext); }
	
	int SCIPcreateProbBasic(SCIP scip, String name);
	static void CALL_SCIPcreateProbBasic(SCIP scip, String name) {
		int retcode = LIB.SCIPcreateProbBasic(scip, name);
		if(retcode != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+retcode);
	};
	static void createProbBasic(String name) { CALL_SCIPcreateProbBasic(scip, name); }
	
	int SCIPaddVar(SCIP scip, SCIP_VAR var);
	static void CALL_SCIPaddVar(SCIP scip, SCIP_VAR var) {
		int retcode = LIB.SCIPaddVar(scip, var);
		if(retcode != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+retcode);
	}
	static void addVar(SCIP_VAR var) { CALL_SCIPaddVar(scip, var); }
	
	int SCIPaddCons(SCIP scip, SCIP_CONS cons);
	static void CALL_SCIPaddCons(SCIP scip, SCIP_CONS cons) {
		int retcode = LIB.SCIPaddCons(scip, cons);
		if(retcode != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+retcode);
	}
	static void addCons(SCIP_CONS cons) { CALL_SCIPaddCons(scip, cons); }
	
	/* scip_sol.h */
	SCIP_SOL SCIPgetBestSol(SCIP scip);
	static SCIP_SOL getBestSol() { return LIB.SCIPgetBestSol(scip); }
	
	int SCIPprintSol(SCIP scip, SCIP_SOL sol, Pointer file, boolean printzeros);
	static void CALL_SCIPprintSol(SCIP scip, SCIP_SOL sol, Pointer file, boolean printzeros) {
		int retcode = LIB.SCIPprintSol(scip, sol, file, printzeros);
		if(retcode != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+retcode);
	}
	static void printSol(SCIP_SOL sol, Pointer file, boolean printzeros) {
		CALL_SCIPprintSol(scip, sol, file, printzeros);
	}
	
	int SCIPprintBestSol(SCIP scip, Pointer file, boolean printzeros);
	static void CALL_SCIPprintBestSol(SCIP scip, Pointer file, boolean printzeros) {
		int retcode = LIB.SCIPprintBestSol(scip, file, printzeros);
		if(retcode != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+retcode);
	}
	static void printBestSol(Pointer file, boolean printzeros) {
		CALL_SCIPprintBestSol(scip, file, printzeros);
	}
	
	double SCIPgetSolVal(SCIP scip, SCIP_SOL sol, SCIP_VAR var);
	static double getSolVal(SCIP_SOL sol, SCIP_VAR var) {
		return LIB.SCIPgetSolVal(scip, sol, var);
	}
	
	double SCIPgetSolOrigObj(SCIP scip, SCIP_SOL sol);
	static double getSolOrigObj(SCIP_SOL sol) {
		return LIB.SCIPgetSolOrigObj(scip, sol);
	}
	
	/* scip_solve.h */
	int SCIPpresolve(SCIP scip);
	static void CALL_SCIPpresolve(SCIP scip) {
		int retcode = LIB.SCIPpresolve(scip);
		if(retcode != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+retcode);
	}
	static void presolve() { CALL_SCIPpresolve(scip); }
	
	int SCIPsolve(SCIP scip);
	static void CALL_SCIPsolve(SCIP scip) {
		int retcode = LIB.SCIPsolve(scip);
		if(retcode != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+retcode);
	}
	static void solve() { CALL_SCIPsolve(scip); }
	
	/* scip_solvingstats.h */
	int SCIPprintOrigProblem(SCIP scip, Pointer file, String extension, boolean genericnames);
	static void CALL_SCIPprintOrigProblem(SCIP scip, Pointer file, String extension, boolean genericnames) {
		int retcode = LIB.SCIPprintOrigProblem(scip, file, extension, genericnames);
		if(retcode != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+retcode);
	}
	static void printOrigProblem(Pointer file, String extension, boolean genericnames) {
		CALL_SCIPprintOrigProblem(scip, file, extension, genericnames);
	}
	
	int SCIPprintStatistics(SCIP scip, Pointer file);
	static void CALL_SCIPprintStatistics(SCIP scip, Pointer file) {
		int retcode = LIB.SCIPprintStatistics(scip, file);
		if(retcode != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+retcode);
	}
	static void printStatistics(Pointer file) { CALL_SCIPprintStatistics(scip, file); }
	
	/* scip_var.h */
	int SCIPcreateVarBasic(SCIP scip, PointerByReference var, String name, 
			double lb, double ub, double obj, int vartype);
	static void CALL_SCIPcreateVarBasic(SCIP scip, SCIP_VAR var,
			String name, double lb, double ub, double obj, SCIP_VARTYPE vartype) {
		int retcode = LIB.SCIPcreateVarBasic(scip, pref, name, lb, ub, obj, vartype.ordinal());
		if(retcode != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+retcode);
		var.setPointer(pref.getValue());
	}
	static SCIP_VAR createVarBasic(String name, double lb, double ub, double obj, SCIP_VARTYPE vartype) {
		SCIP_VAR var = new SCIP_VAR();
		CALL_SCIPcreateVarBasic(scip, var, name, lb, ub, obj, vartype);
		return var;
	}
	
	int SCIPreleaseVar(SCIP scip, PointerByReference var);
	static void CALL_SCIPreleaseVar(SCIP scip, SCIP_VAR var) {
		pref.setValue(var.getPointer());
		int retcode = LIB.SCIPreleaseVar(scip, pref);
		if(retcode != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+retcode);
		var.setPointer(pref.getValue());
	}
	static void releaseVar(SCIP_VAR var) { CALL_SCIPreleaseVar(scip, var); }
	
    //SCIP_RETCODE
    public static final int SCIP_OKAY               =  +1,       /**< normal termination */
	   SCIP_ERROR              =   0,       /**< unspecified error */
	   SCIP_NOMEMORY           =  -1,       /**< insufficient memory error */
	   SCIP_READERROR          =  -2,       /**< read error */
	   SCIP_WRITEERROR         =  -3,       /**< write error */
	   SCIP_NOFILE             =  -4,       /**< file not found error */
	   SCIP_FILECREATEERROR    =  -5,       /**< cannot create file */
	   SCIP_LPERROR            =  -6,       /**< error in LP solver */
	   SCIP_NOPROBLEM          =  -7,       /**< no problem exists */
	   SCIP_INVALIDCALL        =  -8,       /**< method cannot be called at this time in solution process */
	   SCIP_INVALIDDATA        =  -9,       /**< error in input data */
	   SCIP_INVALIDRESULT      = -10,       /**< method returned an invalid result code */
	   SCIP_PLUGINNOTFOUND     = -11,       /**< a required plugin was not found */
	   SCIP_PARAMETERUNKNOWN   = -12,       /**< the parameter with the given name was not found */
	   SCIP_PARAMETERWRONGTYPE = -13,       /**< the parameter is not of the expected type */
	   SCIP_PARAMETERWRONGVAL  = -14,       /**< the value is invalid for the given parameter */
	   SCIP_KEYALREADYEXISTING = -15,       /**< the given key is already existing in table */
	   SCIP_MAXDEPTHLEVEL      = -16,       /**< maximal branching depth level exceeded */
	   SCIP_BRANCHERROR        = -17,       /**< no branching could be created */
	   SCIP_NOTIMPLEMENTED     = -18;       /**< function not implemented */
    
    public static enum SCIP_VERBLEVEL {
    	SCIP_VERBLEVEL_NONE,          /**< only error and warning messages are displayed */
	    SCIP_VERBLEVEL_DIALOG,          /**< only interactive dialogs, errors, and warnings are displayed */
	    SCIP_VERBLEVEL_MINIMA,          /**< only important messages are displayed */
	    SCIP_VERBLEVEL_NORMAL,          /**< standard messages are displayed */
	    SCIP_VERBLEVEL_HIGH,          /**< a lot of information is displayed */
	    SCIP_VERBLEVEL_FULL
    }
    
    /* type_paramset.h */
    public static enum SCIP_PARAMSETTING
    {
       SCIP_PARAMSETTING_DEFAULT,
       SCIP_PARAMSETTING_AGGRESSIVE,
       SCIP_PARAMSETTING_FAST,
       SCIP_PARAMSETTING_OFF
    };
    
    public static enum SCIP_PARAMEMPHASIS {
    	SCIP_PARAMEMPHASIS_DEFAULT,
    	SCIP_PARAMEMPHASIS_CPSOLVER,
	    SCIP_PARAMEMPHASIS_EASYCIP,
	    SCIP_PARAMEMPHASIS_FEASIBILITY,
	    SCIP_PARAMEMPHASIS_HARDLP,
	    SCIP_PARAMEMPHASIS_OPTIMALITY,
	    SCIP_PARAMEMPHASIS_COUNTER,
	    SCIP_PARAMEMPHASIS_PHASEFEAS,
	    SCIP_PARAMEMPHASIS_PHASEIMPROVE,
	    SCIP_PARAMEMPHASIS_PHASEPROOF,
	    SCIP_PARAMEMPHASIS_NUMERICS,
	    SCIP_PARAMEMPHASIS_BENCHMARK
    }
    
    /* type_cons.h */
	public static enum SCIP_LINCONSTYPE {
	    SCIP_LINCONSTYPE_EMPTY,
	    SCIP_LINCONSTYPE_FREE,
	    SCIP_LINCONSTYPE_SINGLETON,
	    SCIP_LINCONSTYPE_AGGREGATION,
	    SCIP_LINCONSTYPE_PRECEDENCE,
	    SCIP_LINCONSTYPE_VARBOUND,
	    SCIP_LINCONSTYPE_SETPARTITION,
	    SCIP_LINCONSTYPE_SETPACKING,
	    SCIP_LINCONSTYPE_SETCOVERING,
	    SCIP_LINCONSTYPE_CARDINALITY,
	    SCIP_LINCONSTYPE_INVKNAPSACK,
	    SCIP_LINCONSTYPE_EQKNAPSACK,
	    SCIP_LINCONSTYPE_BINPACKING,
	    SCIP_LINCONSTYPE_KNAPSACK,
	    SCIP_LINCONSTYPE_INTKNAPSACK,
	    SCIP_LINCONSTYPE_MIXEDBINARY,
	    SCIP_LINCONSTYPE_GENERAL
	};
	
	/* type_status.h */
	public static enum SCIP_STATUS {
	    SCIP_STATUS_UNKNOWN,
	    SCIP_STATUS_USERINTERRUPT,
	    SCIP_STATUS_NODELIMIT,
	    SCIP_STATUS_TOTALNODELIMIT,
	    SCIP_STATUS_STALLNODELIMIT,
	    SCIP_STATUS_TIMELIMIT,
	    SCIP_STATUS_MEMLIMIT,
	    SCIP_STATUS_GAPLIMIT,
	    SCIP_STATUS_SOLLIMIT,
	    SCIP_STATUS_BESTSOLLIMIT,
	    SCIP_STATUS_RESTARTLIMIT,
	    SCIP_STATUS_OPTIMAL,
	    SCIP_STATUS_INFEASIBLE,
	    SCIP_STATUS_UNBOUNDED,
	    SCIP_STATUS_INFORUNBD,
	    SCIP_STATUS_TERMINATE
	 };
    
    /* type_var.h */
    public static enum SCIP_VARTYPE {
       SCIP_VARTYPE_BINARY,
       SCIP_VARTYPE_INTEGER,
       SCIP_VARTYPE_IMPLINT,
       SCIP_VARTYPE_CONTINUOUS
    };

}