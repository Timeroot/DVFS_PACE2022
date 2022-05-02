package JNA_SCIP;

import java.util.HashMap;

import com.sun.jna.DefaultTypeMapper;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.TypeMapper;
import com.sun.jna.platform.EnumConverter;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.PointerByReference;

import static JNA_SCIP.SCIP_RETCODE.*;

public interface JSCIP extends Library {
	static TypeMapper scip_TypeMapper = new DefaultTypeMapper() {
        {
        	addTypeConverter(SCIP_LINCONSTYPE.class, new EnumConverter<SCIP_LINCONSTYPE>(SCIP_LINCONSTYPE.class));
            addTypeConverter(SCIP_PARAMEMPHASIS.class, new EnumConverter<SCIP_PARAMEMPHASIS>(SCIP_PARAMEMPHASIS.class));
            addTypeConverter(SCIP_PARAMSETTING.class, new EnumConverter<SCIP_PARAMSETTING>(SCIP_PARAMSETTING.class));
            addTypeConverter(SCIP_RESULT.class, new EnumConverter<SCIP_RESULT>(SCIP_RESULT.class));
            addTypeConverter(SCIP_STATUS.class, new EnumConverter<SCIP_STATUS>(SCIP_STATUS.class));
            addTypeConverter(SCIP_VARTYPE.class, new EnumConverter<SCIP_VARTYPE>(SCIP_VARTYPE.class));
            addTypeConverter(SCIP_VERBLEVEL.class, new EnumConverter<SCIP_VERBLEVEL>(SCIP_VERBLEVEL.class));
            //weird one needs a special converter
            addTypeConverter(SCIP_RETCODE.class, SCIP_RETCODE.RETCODE_Converter.inst);
        }
    };
	static JSCIP LIB = (JSCIP)Native.load("scip", JSCIP.class,
		new HashMap<String, Object>() {
			private static final long serialVersionUID = -8766823852974891689L;
		{
		    put(Library.OPTION_TYPE_MAPPER, scip_TypeMapper);
		}
    });
	
	static SCIP scip = new SCIP();
	
	/* cons.h */
	String SCIPconsGetName(SCIP_CONS cons);
	static String consGetName(SCIP_CONS cons){ return LIB.SCIPconsGetName(cons); }

	int SCIPconsGetPos(SCIP_CONS cons);
	static int consGetPos(SCIP_CONS cons){ return LIB.SCIPconsGetPos(cons); }
	
	SCIP_CONSHDLR SCIPconsGetHdlr(SCIP_CONS cons);
	static SCIP_CONSHDLR consGetHdlr(SCIP_CONS cons){ return LIB.SCIPconsGetHdlr(cons); }
	
	Pointer SCIPconsGetData(SCIP_CONS cons);
	static Pointer consGetData(SCIP_CONS cons){ return LIB.SCIPconsGetData(cons); }
	
	int SCIPconsGetNUses(SCIP_CONS cons);
	static int consGetNUses(SCIP_CONS cons){ return LIB.SCIPconsGetNUses(cons); }
	
	boolean SCIPconsIsDeleted(SCIP_CONS cons);
	static boolean consIsDeleted(SCIP_CONS cons){ return LIB.SCIPconsIsDeleted(cons); }
	
	boolean SCIPconsIsEnabled(SCIP_CONS cons);
	static boolean consIsEnabled(SCIP_CONS cons){ return LIB.SCIPconsIsEnabled(cons); }
	
	boolean SCIPconsIsAdded(SCIP_CONS cons);
	static boolean consIsAdded(SCIP_CONS cons){ return LIB.SCIPconsIsAdded(cons); }
	
	boolean SCIPconsIsObsolete(SCIP_CONS cons);
	static boolean consIsObsolete(SCIP_CONS cons){ return LIB.SCIPconsIsObsolete(cons); }
	
	boolean SCIPconsIsConflict(SCIP_CONS cons);
	static boolean consIsConflict(SCIP_CONS cons){ return LIB.SCIPconsIsConflict(cons); }
	
	boolean SCIPconsIsInProb(SCIP_CONS cons);
	static boolean consIsInProb(SCIP_CONS cons){ return LIB.SCIPconsIsInProb(cons); }
	
	String SCIPconshdlrGetName(SCIP_CONSHDLR conshdlr);
	static String conshdlrGetName(SCIP_CONSHDLR conshdlr){ return LIB.SCIPconshdlrGetName(conshdlr); } 
	
	String SCIPconshdlrGetDesc(SCIP_CONSHDLR conshdlr);
	static String conshdlrGetDesc(SCIP_CONSHDLR conshdlr){ return LIB.SCIPconshdlrGetDesc(conshdlr); }
	
	Pointer SCIPconshdlrGetData(SCIP_CONSHDLR cons);
	static Pointer conshdlrGetData(SCIP_CONSHDLR cons){ return LIB.SCIPconshdlrGetData(cons); }
	
	/* cons_linear.h */
	SCIP_RETCODE SCIPcreateConsBasicLinear (SCIP scip, PointerByReference cons, String name,
			int nvars, SCIP_VAR[] vars, double[] vals, double lhs, double rhs);
	static void CALL_SCIPcreateConsBasicLinear(SCIP scip, SCIP_CONS cons, String name,
			int nvars, SCIP_VAR[] vars, double[] vals, double lhs, double rhs) {
		PointerByReference pref = new PointerByReference();
		pref.setValue(cons.getPointer());
		SCIP_RETCODE ret = LIB.SCIPcreateConsBasicLinear(scip, pref, name, nvars, vars, vals, lhs, rhs);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
		cons.setPointer(pref.getValue());
	}
	static SCIP_CONS createConsBasicLinear(String name,
			int nvars, SCIP_VAR[] vars, double[] vals, double lhs, double rhs) {
		SCIP_CONS cons = new SCIP_CONS();
		CALL_SCIPcreateConsBasicLinear(scip, cons, name, nvars, vars, vals, lhs, rhs);
		return cons;
	}
	
	SCIP_RETCODE SCIPaddCoefLinear(SCIP scip, SCIP_CONS cons, SCIP_VAR var, double val);
	static void CALL_SCIPaddCoefLinear(SCIP scip, SCIP_CONS cons, SCIP_VAR var, double val) {
		SCIP_RETCODE ret = LIB.SCIPaddCoefLinear(scip, cons, var, val);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
	}
	static void addCoefLinear(SCIP_CONS cons, SCIP_VAR var, double val) {
		CALL_SCIPaddCoefLinear(scip, cons, var, val);
	}
	
	/* expr_var.h */
	SCIP_RETCODE SCIPcreateExprVar(SCIP scip, PointerByReference expr, SCIP_VAR var,
			Pointer ownercreate, Pointer ownercreatedata);
	static void CALL_SCIPcreateExprVar(SCIP scip, SCIP_EXPR expr, SCIP_VAR var,
			Pointer ownercreate, Pointer ownercreatedata) {
		PointerByReference pref = new PointerByReference();
		pref.setValue(expr.getPointer());
		SCIP_RETCODE ret = LIB.SCIPcreateExprVar(scip, pref, var, ownercreate, ownercreatedata);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
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
	SCIP_RETCODE SCIPincludeDefaultPlugins(SCIP scip);
	static void CALL_SCIPincludeDefaultPlugins(SCIP scip) {
		SCIP_RETCODE ret = LIB.SCIPincludeDefaultPlugins(scip);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
	}
	static void includeDefaultPlugins() { CALL_SCIPincludeDefaultPlugins(scip); }
	
	/* scip_cons.h */
	SCIP_RETCODE SCIPreleaseCons(SCIP scip, PointerByReference cons);
	static void CALL_SCIPreleaseCons(SCIP scip, SCIP_CONS cons) {
		PointerByReference pref = new PointerByReference();
		pref.setValue(cons.getPointer());
		SCIP_RETCODE ret = LIB.SCIPreleaseCons(scip, pref);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
		//cons.setPointer(pref.getValue()); //always returns null
	}
	static void releaseCons(SCIP_CONS cons) { CALL_SCIPreleaseCons(scip, cons); }
	
	SCIP_RETCODE SCIPsetConshdlrEnforelax(SCIP scip,	SCIP_CONSHDLR conshdlr,
			Pointer consenforelax//SCIP_DECL_CONSENFORELAX  	 
		);//TODO
	
	SCIP_RETCODE SCIPincludeConshdlrBasic(SCIP scip,
			PointerByReference conshdlrptr, String name, String desc,
			int enfopriority, int chckpriority, int eagerfreq, boolean needscons,
			SCIP_DECL_CONSENFOLP consenfolp,
			SCIP_DECL_CONSENFOPS consenfops,
			SCIP_DECL_CONSCHECK conscheck,
			SCIP_DECL_CONSLOCK conslock,
			Pointer conshdlrdata//SCIP_CONSHDLRDATA 
		);
	static SCIP_CONSHDLR CALL_SCIPincludeConshdlrBasic(SCIP scip,
			String name, String desc,
			int enfopriority, int chckpriority, int eagerfreq, boolean needscons,
			SCIP_DECL_CONSENFOLP consenfolp,
			SCIP_DECL_CONSENFOPS consenfops,
			SCIP_DECL_CONSCHECK conscheck,
			SCIP_DECL_CONSLOCK conslock,
			Pointer conshdlrdata//SCIP_CONSHDLRDATA 
		) {
		PointerByReference pref = new PointerByReference();
		SCIP_RETCODE ret = LIB.SCIPincludeConshdlrBasic(scip, pref, name, desc,
				enfopriority, chckpriority, eagerfreq, needscons, consenfolp, consenfops,
				conscheck, conslock, conshdlrdata);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
		return new SCIP_CONSHDLR(pref.getValue());
	}
	
	SCIP_RETCODE SCIPsetConshdlrDelete(SCIP scip, SCIP_CONSHDLR conshdlr, SCIP_DECL_CONSDELETE consDelete);
	static void CALL_SCIPsetConshdlrDelete(SCIP scip, SCIP_CONSHDLR conshdlr, SCIP_DECL_CONSDELETE consDelete) {
		SCIP_RETCODE ret = LIB.SCIPsetConshdlrDelete(scip, conshdlr, consDelete);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
	}
	static void setConshdlrDelete(SCIP_CONSHDLR conshdlr, SCIP_DECL_CONSDELETE consDelete) {
		CALL_SCIPsetConshdlrDelete(scip, conshdlr, consDelete);
	}
	
	SCIP_RETCODE SCIPcreateCons(SCIP scip, PointerByReference cons, String name, SCIP_CONSHDLR conshdlr,
			Pointer consdata, boolean initial, boolean separate, boolean enforce, boolean check,
			boolean propagate, boolean local, boolean modifiable, boolean dynamic, boolean removable,
			boolean stickingatnode);
	static void CALL_SCIPcreateCons(SCIP scip, SCIP_CONS cons, String name, SCIP_CONSHDLR conshdlr,
			Pointer consdata, boolean initial, boolean separate, boolean enforce, boolean check,
			boolean propagate, boolean local, boolean modifiable, boolean dynamic, boolean removable,
			boolean stickingatnode) {
		PointerByReference pref = new PointerByReference();
		pref.setValue(cons.getPointer());
		SCIP_RETCODE ret = LIB.SCIPcreateCons(scip, pref, name, conshdlr, consdata, initial, separate,
				enforce, check, propagate, local, modifiable, dynamic, removable, stickingatnode);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
		cons.setPointer(pref.getValue());
	}
	static SCIP_CONS createCons(String name, SCIP_CONSHDLR conshdlr,
			Pointer consdata, boolean initial, boolean separate, boolean enforce, boolean check,
			boolean propagate, boolean local, boolean modifiable, boolean dynamic, boolean removable,
			boolean stickingatnode) {
		SCIP_CONS cons = new SCIP_CONS();
		CALL_SCIPcreateCons(scip, cons, name, conshdlr, consdata, initial, separate, enforce, check,
				propagate, local, modifiable, dynamic, removable, stickingatnode);
		return cons;
	}
	
	/* scip_dialog.h */
	SCIP_RETCODE SCIPstartInteraction(SCIP scip);
	static void CALL_SCIPstartInteraction(SCIP scip) {
		SCIP_RETCODE ret = LIB.SCIPstartInteraction(scip);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
	};
	static void startInteraction() { CALL_SCIPstartInteraction(scip); }
	
	/* scip_general.h */
	SCIP_RETCODE SCIPcreate(PointerByReference scip);//SCIP_RETCODE
    static void CALL_SCIPcreate(SCIP scip) {
		PointerByReference pref = new PointerByReference();
    	pref.setValue(scip.getPointer());
		SCIP_RETCODE ret = LIB.SCIPcreate(pref);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
		scip.setPointer(pref.getValue());
    }
    static void create() {
    	if(scip.getPointer() != null)
    		throw new RuntimeException("Called create() when scip was already nonnull, call free() first.");
    	CALL_SCIPcreate(scip);
    }
    
    SCIP_RETCODE SCIPfree(PointerByReference scip);//SCIP_RETCODE
    static void CALL_SCIPfree(SCIP scip) {
		PointerByReference pref = new PointerByReference();
    	pref.setValue(scip.getPointer());
		SCIP_RETCODE ret = LIB.SCIPfree(pref);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
//		scip.setPointer(pref.getValue()); //always returns null
    }
    static void free() { CALL_SCIPfree(scip); }
    
	void SCIPprintVersion(SCIP scip, Pointer file);
	static void printVersion(Pointer file) { LIB.SCIPprintVersion(scip, file); }
	
	SCIP_STATUS SCIPgetStatus(SCIP scip);
	static SCIP_STATUS getStatus() { return LIB.SCIPgetStatus(scip); }
	
	/* scip_lp.h */
	SCIP_RETCODE SCIPcreateEmptyRowConshdlr (SCIP scip, PointerByReference row, SCIP_CONSHDLR conshdlr,
			String name, double lhs, double rhs, boolean local, boolean modifiable,
			boolean removable);
	static void CALL_SCIPcreateEmptyRowConshdlr (SCIP scip, SCIP_ROW row, SCIP_CONSHDLR conshdlr,
			String name, double lhs, double rhs, boolean local, boolean modifiable,
			boolean removable) {
		PointerByReference pref = new PointerByReference();
		SCIP_RETCODE ret = LIB.SCIPcreateEmptyRowConshdlr(scip, pref, conshdlr, name, lhs, rhs, local, modifiable, removable);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
		row.setPointer(pref.getValue());
	}
	static SCIP_ROW createEmptyRowConshdlr(SCIP_CONSHDLR conshdlr, String name, double lhs,
			double rhs, boolean local, boolean modifiable, boolean removable) {
		SCIP_ROW row = new SCIP_ROW();
		CALL_SCIPcreateEmptyRowConshdlr(scip, row, conshdlr, name, lhs, rhs, local, modifiable, removable);
		return row;
	}
	
	int SCIPaddVarToRow(SCIP scip, SCIP_ROW row, SCIP_VAR var, double val);
	int SCIPaddRow(SCIP scip, SCIP_ROW row, boolean forcecut, ByteByReference infeasible);
	int SCIPreleaseRow(SCIP scip, PointerByReference row);//TODO
	
	int SCIPcacheRowExtensions(SCIP scip, SCIP_ROW row);
	int SCIPflushRowExtensions(SCIP scip, SCIP_ROW row);
	
	/* scip_mem.h */
	Pointer SCIPblkmem(SCIP scip);
	
	/* scip_message.h */
	void SCIPinfoMessage(SCIP sc, Pointer file, String formatstr, Object... vals);
	static void infoMessage(Pointer file, String formatstr, Object... vals) {
		LIB.SCIPinfoMessage(scip, file, formatstr, vals);
	}

	SCIP_VERBLEVEL SCIPgetVerbLevel(SCIP scip);//SCIP_VERBLEVEL
	static SCIP_VERBLEVEL getVerbLevel() { return LIB.SCIPgetVerbLevel(scip); };
	
	/* scip_numerics.h */
	double SCIPinfinity(SCIP scip);
	static double infinity() { return LIB.SCIPinfinity(scip); }
	
	/* scip_param.h */
	SCIP_RETCODE SCIPsetRealParam(SCIP scip, String name, double value);
	static void CALL_SCIPsetRealParam(SCIP scip, String name, double value) {
		SCIP_RETCODE ret = LIB.SCIPsetRealParam(scip, name, value);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
	};
	static void setRealParam(String name, double value) { CALL_SCIPsetRealParam(scip, name, value); }
	
	SCIP_RETCODE SCIPsetCharParam(SCIP scip, String name, byte value);
	static void CALL_SCIPsetCharParam(SCIP scip, String name, byte value) {
		SCIP_RETCODE ret = LIB.SCIPsetCharParam(scip, name, value);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
	};
	static void setCharParam(String name, byte value) { CALL_SCIPsetCharParam(scip, name, value); }

	SCIP_RETCODE SCIPsetIntParam(SCIP scip, String name, int value);
	static void CALL_SCIPsetIntParam(SCIP scip, String name, int value) {
		SCIP_RETCODE ret = LIB.SCIPsetIntParam(scip, name, value);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
	};
	static void setIntParam(String name, int value) { CALL_SCIPsetIntParam(scip, name, value); }

	SCIP_RETCODE SCIPsetLongintParam(SCIP scip, String name, long value);
	static void CALL_SCIPsetLongintParam(SCIP scip, String name, long value) {
		SCIP_RETCODE ret = LIB.SCIPsetLongintParam(scip, name, value);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
	};
	static void setLongintParam(String name, long value) { CALL_SCIPsetLongintParam(scip, name, value); }
	
	SCIP_RETCODE SCIPsetStringParam(SCIP scip, String name, String value);
	static void CALL_SCIPsetStringParam(SCIP scip, String name, String value) {
		SCIP_RETCODE ret = LIB.SCIPsetStringParam(scip, name, value);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
	};
	static void setStringParam(String name, String value) { CALL_SCIPsetStringParam(scip, name, value); }
	
	SCIP_RETCODE SCIPsetEmphasis(SCIP scip, SCIP_PARAMEMPHASIS emph, boolean quiet);
	static void CALL_SCIPsetEmphasis(SCIP scip, SCIP_PARAMEMPHASIS emph, boolean quiet) {
		SCIP_RETCODE ret = LIB.SCIPsetEmphasis(scip, emph, quiet);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
	};
	static void setEmphasis(SCIP_PARAMEMPHASIS emph, boolean quiet) { CALL_SCIPsetEmphasis(scip, emph, quiet); }
	
	SCIP_RETCODE SCIPsetPresolving(SCIP scip, SCIP_PARAMSETTING emph, boolean quiet);
	static void CALL_SCIPsetPresolving(SCIP scip, SCIP_PARAMSETTING emph, boolean quiet) {
		SCIP_RETCODE ret = LIB.SCIPsetPresolving(scip, emph, quiet);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
	};
	static void setPresolving(SCIP_PARAMSETTING emph, boolean quiet) { CALL_SCIPsetPresolving(scip, emph, quiet); }
	
	
	/* scip_prob.h */
	SCIP_RETCODE SCIPreadProb(SCIP scip, String filename, String ext);
	static void CALL_SCIPreadProb(SCIP scip, String filename, String ext) {
		SCIP_RETCODE ret = LIB.SCIPreadProb(scip, filename, ext);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
	}
	static void readProb(String filename, String ext) { CALL_SCIPreadProb(scip, filename, ext); }
	
	SCIP_RETCODE SCIPcreateProbBasic(SCIP scip, String name);
	static void CALL_SCIPcreateProbBasic(SCIP scip, String name) {
		SCIP_RETCODE ret = LIB.SCIPcreateProbBasic(scip, name);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
	};
	static void createProbBasic(String name) { CALL_SCIPcreateProbBasic(scip, name); }
	
	SCIP_RETCODE SCIPfreeProb(SCIP scip);
	static void CALL_SCIPfreeProb(SCIP scip) {
		SCIP_RETCODE ret = LIB.SCIPfreeProb(scip);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
	};
	static void freeProb() { CALL_SCIPfreeProb(scip); }
	
	SCIP_RETCODE SCIPaddVar(SCIP scip, SCIP_VAR var);
	static void CALL_SCIPaddVar(SCIP scip, SCIP_VAR var) {
		SCIP_RETCODE ret = LIB.SCIPaddVar(scip, var);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
	}
	static void addVar(SCIP_VAR var) { CALL_SCIPaddVar(scip, var); }
	
	SCIP_RETCODE SCIPaddCons(SCIP scip, SCIP_CONS cons);
	static void CALL_SCIPaddCons(SCIP scip, SCIP_CONS cons) {
		SCIP_RETCODE ret = LIB.SCIPaddCons(scip, cons);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
	}
	static void addCons(SCIP_CONS cons) { CALL_SCIPaddCons(scip, cons); }
	
	/* scip_sol.h */
	SCIP_SOL SCIPgetBestSol(SCIP scip);
	static SCIP_SOL getBestSol() { return LIB.SCIPgetBestSol(scip); }
	
	SCIP_RETCODE SCIPprintSol(SCIP scip, SCIP_SOL sol, Pointer file, boolean printzeros);
	static void CALL_SCIPprintSol(SCIP scip, SCIP_SOL sol, Pointer file, boolean printzeros) {
		SCIP_RETCODE ret = LIB.SCIPprintSol(scip, sol, file, printzeros);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
	}
	static void printSol(SCIP_SOL sol, Pointer file, boolean printzeros) {
		CALL_SCIPprintSol(scip, sol, file, printzeros);
	}
	
	SCIP_RETCODE SCIPprintBestSol(SCIP scip, Pointer file, boolean printzeros);
	static void CALL_SCIPprintBestSol(SCIP scip, Pointer file, boolean printzeros) {
		SCIP_RETCODE ret = LIB.SCIPprintBestSol(scip, file, printzeros);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
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
	SCIP_RETCODE SCIPpresolve(SCIP scip);
	static void CALL_SCIPpresolve(SCIP scip) {
		SCIP_RETCODE ret = LIB.SCIPpresolve(scip);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
	}
	static void presolve() { CALL_SCIPpresolve(scip); }
	
	SCIP_RETCODE SCIPsolve(SCIP scip);
	static void CALL_SCIPsolve(SCIP scip) {
		SCIP_RETCODE ret = LIB.SCIPsolve(scip);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
	}
	static void solve() { CALL_SCIPsolve(scip); }
	
	/* scip_solvingstats.h */
	SCIP_RETCODE SCIPprintOrigProblem(SCIP scip, Pointer file, String extension, boolean genericnames);
	static void CALL_SCIPprintOrigProblem(SCIP scip, Pointer file, String extension, boolean genericnames) {
		SCIP_RETCODE ret = LIB.SCIPprintOrigProblem(scip, file, extension, genericnames);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
	}
	static void printOrigProblem(Pointer file, String extension, boolean genericnames) {
		CALL_SCIPprintOrigProblem(scip, file, extension, genericnames);
	}
	
	SCIP_RETCODE SCIPprintStatistics(SCIP scip, Pointer file);
	static void CALL_SCIPprintStatistics(SCIP scip, Pointer file) {
		SCIP_RETCODE ret = LIB.SCIPprintStatistics(scip, file);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
	}
	static void printStatistics(Pointer file) { CALL_SCIPprintStatistics(scip, file); }
	
	/* scip_var.h */
	SCIP_RETCODE SCIPcreateVarBasic(SCIP scip, PointerByReference var, String name, 
			double lb, double ub, double obj, SCIP_VARTYPE vartype);
	static void CALL_SCIPcreateVarBasic(SCIP scip, SCIP_VAR var,
			String name, double lb, double ub, double obj, SCIP_VARTYPE vartype) {
		PointerByReference pref = new PointerByReference();
		pref.setValue(var.getPointer());
		SCIP_RETCODE ret = LIB.SCIPcreateVarBasic(scip, pref, name, lb, ub, obj, vartype);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
		var.setPointer(pref.getValue());
	}
	static SCIP_VAR createVarBasic(String name, double lb, double ub, double obj, SCIP_VARTYPE vartype) {
		SCIP_VAR var = new SCIP_VAR();
		CALL_SCIPcreateVarBasic(scip, var, name, lb, ub, obj, vartype);
		return var; 
	}
	
	SCIP_RETCODE SCIPreleaseVar(SCIP scip, PointerByReference var);
	static void CALL_SCIPreleaseVar(SCIP scip, SCIP_VAR var) {
		PointerByReference pref = new PointerByReference();
		pref.setValue(var.getPointer());
		SCIP_RETCODE ret = LIB.SCIPreleaseVar(scip, pref);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
//		var.setPointer(pref.getValue()); //always return null
	}
	static void releaseVar(SCIP_VAR var) { CALL_SCIPreleaseVar(scip, var); }

	SCIP_RETCODE SCIPcaptureVar(SCIP scip, SCIP_VAR var);
	static void CALL_SCIPcaptureVar(SCIP scip, SCIP_VAR var) {
		SCIP_RETCODE ret = LIB.SCIPcaptureVar(scip, var);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
	}
	static void captureVar(SCIP_VAR var) { CALL_SCIPcaptureVar(scip, var); }
	
	SCIP_RETCODE SCIPaddVarLocks(SCIP scip, SCIP_VAR var, int nlocksdown, int nlocksup);
	static void CALL_SCIPaddVarLocks(SCIP scip, SCIP_VAR var, int nlocksdown, int nlocksup) {
		SCIP_RETCODE ret = LIB.SCIPaddVarLocks(scip, var, nlocksdown, nlocksup);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
	}
	static void addVarLocks(SCIP_VAR var, int nlocksdown, int nlocksup) { CALL_SCIPaddVarLocks(scip, var, nlocksdown, nlocksup); } 

	SCIP_RETCODE SCIPaddVarLocksType(SCIP scip, SCIP_VAR var, SCIP_LOCKTYPE type, int nlocksdown, int nlocksup);
	static void CALL_SCIPaddVarLocksType(SCIP scip, SCIP_VAR var, SCIP_LOCKTYPE type, int nlocksdown, int nlocksup) {
		SCIP_RETCODE ret = LIB.SCIPaddVarLocksType(scip, var, null, nlocksdown, nlocksup);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
	}
	static void addVarLocksType(SCIP_VAR var, SCIP_LOCKTYPE type, int nlocksdown, int nlocksup) { CALL_SCIPaddVarLocksType(scip, var, type, nlocksdown, nlocksup); }
	
	SCIP_RETCODE SCIPprintVar(SCIP scip, SCIP_VAR var, Pointer file);
	static void CALL_SCIPprintVar(SCIP scip, SCIP_VAR var, Pointer file) {
		SCIP_RETCODE ret = LIB.SCIPprintVar(scip, var, file);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
	}
	static void printVar(SCIP_VAR var, Pointer file) { CALL_SCIPprintVar(scip, var, file); }

	/* var.c */
	String SCIPvarGetName(SCIP_VAR var);
	static String varGetName(SCIP_VAR var) { return LIB.SCIPvarGetName(var); }
	
	int SCIPvarGetNUses(SCIP_VAR var);
	static int varGetNUses(SCIP_VAR var) { return LIB.SCIPvarGetNUses(var); }
	
	SCIP_VARTYPE SCIPvarGetType(SCIP_VAR var);
	static SCIP_VARTYPE varGetType(SCIP_VAR var) { return LIB.SCIPvarGetType(var); }
	
	double SCIPvarGetObj(SCIP_VAR var);
	static double varGetObj(SCIP_VAR var) { return LIB.SCIPvarGetObj(var); }
	
	//Our own sketchy methods. This gets the SCIP_SET* field
	static Pointer SCIPset(SCIP scip) {
		return scip.getPointer().getPointer(8);
	}
}