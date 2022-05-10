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
	public static final TypeMapper TYPE_MAPPER = new DefaultTypeMapper() {
        {
        	addTypeConverter(SCIP_BOUNDCHGTYPE.class, new EnumConverter<SCIP_BOUNDCHGTYPE>(SCIP_BOUNDCHGTYPE.class));
        	addTypeConverter(SCIP_DOMCHGTYPE.class, new EnumConverter<SCIP_DOMCHGTYPE>(SCIP_DOMCHGTYPE.class));
        	addTypeConverter(SCIP_LINCONSTYPE.class, new EnumConverter<SCIP_LINCONSTYPE>(SCIP_LINCONSTYPE.class));
        	addTypeConverter(SCIP_LOCKTYPE.class, new EnumConverter<SCIP_LOCKTYPE>(SCIP_LOCKTYPE.class));
            addTypeConverter(SCIP_PARAMEMPHASIS.class, new EnumConverter<SCIP_PARAMEMPHASIS>(SCIP_PARAMEMPHASIS.class));
            addTypeConverter(SCIP_PARAMSETTING.class, new EnumConverter<SCIP_PARAMSETTING>(SCIP_PARAMSETTING.class));
            addTypeConverter(SCIP_PROPTIMING.class, new EnumConverter<SCIP_PROPTIMING>(SCIP_PROPTIMING.class));
            addTypeConverter(SCIP_RESULT.class, new EnumConverter<SCIP_RESULT>(SCIP_RESULT.class));
            addTypeConverter(SCIP_STATUS.class, new EnumConverter<SCIP_STATUS>(SCIP_STATUS.class));
            addTypeConverter(SCIP_VARSTATUS.class, new EnumConverter<SCIP_VARSTATUS>(SCIP_VARSTATUS.class));
            addTypeConverter(SCIP_VARTYPE.class, new EnumConverter<SCIP_VARTYPE>(SCIP_VARTYPE.class));
            addTypeConverter(SCIP_VERBLEVEL.class, new EnumConverter<SCIP_VERBLEVEL>(SCIP_VERBLEVEL.class));
            //weird ones that need a special converter
            addTypeConverter(SCIP_RETCODE.class, SCIP_RETCODE.RETCODE_Converter.inst);
            addTypeConverter(SCIP_HEURTIMING.class, SCIP_HEURTIMING.HEURTIMING_Converter.inst);
        }
    };
    
	static JSCIP LIB = Native.load("scip", JSCIP.class,
		new HashMap<String, Object>() {
			private static final long serialVersionUID = -8766823852974891689L;
		{
		    put(Library.OPTION_TYPE_MAPPER, TYPE_MAPPER);
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
	
	boolean SCIPconsIsInitial(SCIP_CONS cons);
	static boolean consIsInitial(SCIP_CONS cons){ return LIB.SCIPconsIsInitial(cons); }
	
	boolean SCIPconsIsSeparated(SCIP_CONS cons);
	static boolean consIsSeparated(SCIP_CONS cons){ return LIB.SCIPconsIsSeparated(cons); }
	
	boolean SCIPconsIsEnforced(SCIP_CONS cons);
	static boolean consIsEnforced(SCIP_CONS cons){ return LIB.SCIPconsIsEnforced(cons); }
	
	boolean SCIPconsIsChecked(SCIP_CONS cons);
	static boolean consIsChecked(SCIP_CONS cons){ return LIB.SCIPconsIsChecked(cons); }
	
	boolean SCIPconsIsPropagated(SCIP_CONS cons);
	static boolean consIsPropagated(SCIP_CONS cons){ return LIB.SCIPconsIsPropagated(cons); }
	
	boolean SCIPconsIsLocal(SCIP_CONS cons);
	static boolean consIsLocal(SCIP_CONS cons){ return LIB.SCIPconsIsLocal(cons); }
	
	boolean SCIPconsIsModifiable(SCIP_CONS cons);
	static boolean consIsModifiable(SCIP_CONS cons){ return LIB.SCIPconsIsModifiable(cons); }
	
	boolean SCIPconsIsDynamic(SCIP_CONS cons);
	static boolean consIsDynamic(SCIP_CONS cons){ return LIB.SCIPconsIsDynamic(cons); }
	
	boolean SCIPconsIsRemovable(SCIP_CONS cons);
	static boolean consIsRemovable(SCIP_CONS cons){ return LIB.SCIPconsIsRemovable(cons); }
	
	boolean SCIPconsIsStickingAtNode(SCIP_CONS cons);
	static boolean consIsStickingAtNode(SCIP_CONS cons){ return LIB.SCIPconsIsStickingAtNode(cons); }
	
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
	
	double SCIPconshdlrGetSetupTime(SCIP_CONSHDLR conshdlr);
	static double conshdlrGetSetupTime(SCIP_CONSHDLR conshdlr){ return LIB.SCIPconshdlrGetSetupTime(conshdlr); }
	
	double SCIPconshdlrGetPresolTime(SCIP_CONSHDLR conshdlr);
	static double conshdlrGetPresolTime(SCIP_CONSHDLR conshdlr){ return LIB.SCIPconshdlrGetPresolTime(conshdlr); }
	
	double SCIPconshdlrGetSepaTime(SCIP_CONSHDLR conshdlr);
	static double conshdlrGetSepaTime(SCIP_CONSHDLR conshdlr){ return LIB.SCIPconshdlrGetSepaTime(conshdlr); }
	
	double SCIPconshdlrGetEnfoLPTime(SCIP_CONSHDLR conshdlr);
	static double conshdlrGetEnfoLPTime(SCIP_CONSHDLR conshdlr){ return LIB.SCIPconshdlrGetEnfoLPTime(conshdlr); }
	
	double SCIPconshdlrGetEnfoPSTime(SCIP_CONSHDLR conshdlr);
	static double conshdlrGetEnfoPSTime(SCIP_CONSHDLR conshdlr){ return LIB.SCIPconshdlrGetEnfoPSTime(conshdlr); }
	
	double SCIPconshdlrGetEnfoRelaxTime(SCIP_CONSHDLR conshdlr);
	static double conshdlrGetEnfoRelaxTime(SCIP_CONSHDLR conshdlr){ return LIB.SCIPconshdlrGetEnfoRelaxTime(conshdlr); }
	
	double SCIPconshdlrGetPropTime(SCIP_CONSHDLR conshdlr);
	static double conshdlrGetPropTime(SCIP_CONSHDLR conshdlr){ return LIB.SCIPconshdlrGetPropTime(conshdlr); }
	
	double SCIPconshdlrGetCheckTime(SCIP_CONSHDLR conshdlr);
	static double conshdlrGetCheckTime(SCIP_CONSHDLR conshdlr){ return LIB.SCIPconshdlrGetCheckTime(conshdlr); }
	
	double SCIPconshdlrGetRespropTime(SCIP_CONSHDLR conshdlr);
	static double conshdlrGetRespropTime(SCIP_CONSHDLR conshdlr){ return LIB.SCIPconshdlrGetRespropTime(conshdlr); }
	
	/* cons_linear.h */
	SCIP_RETCODE SCIPcreateConsBasicLinear (SCIP scip, PointerByReference cons, String name,
			int nvars, SCIP_VAR[] vars, double[] vals, double lhs, double rhs);
	static void CALL_SCIPcreateConsBasicLinear(SCIP scip, SCIP_CONS cons, String name,
			SCIP_VAR[] vars, double[] vals, double lhs, double rhs) {
		PointerByReference pref = new PointerByReference();
		pref.setValue(cons.getPointer());
		SCIP_RETCODE ret = LIB.SCIPcreateConsBasicLinear(scip, pref, name, vars==null?0:vars.length, vars, vals, lhs, rhs);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
		cons.setPointer(pref.getValue());
	}
	static SCIP_CONS createConsBasicLinear(String name, SCIP_VAR[] vars, double[] vals, double lhs, double rhs) {
		SCIP_CONS cons = new SCIP_CONS();
		CALL_SCIPcreateConsBasicLinear(scip, cons, name, vars, vals, lhs, rhs);
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
	
	/* cons_logicor.h */
	SCIP_RETCODE SCIPcreateConsBasicLogicor(SCIP scip, PointerByReference cons,
			String name, int nvars, SCIP_VAR[] vars);
	static void CALL_SCIPcreateConsBasicLogicor(SCIP scip, SCIP_CONS cons, String name, SCIP_VAR[] vars) {
		PointerByReference pref = new PointerByReference();
		pref.setValue(cons.getPointer());
		SCIP_RETCODE ret = LIB.SCIPcreateConsBasicLogicor(scip, pref, name, vars==null?0:vars.length, vars);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
		cons.setPointer(pref.getValue());
	}
	static SCIP_CONS createConsBasicLogicor(String name, SCIP_VAR[] vars) {
		SCIP_CONS cons = new SCIP_CONS();
		CALL_SCIPcreateConsBasicLogicor(scip, cons, name, vars);
		return cons;
	}
	
	/* cons_setppc.h */
	SCIP_RETCODE SCIPcreateConsBasicSetcover(SCIP scip, PointerByReference cons,
			String name, int nvars, SCIP_VAR[] vars);
	static void CALL_SCIPcreateConsBasicSetcover(SCIP scip, SCIP_CONS cons, String name,
			SCIP_VAR[] vars) {
		PointerByReference pref = new PointerByReference();
		pref.setValue(cons.getPointer());
		SCIP_RETCODE ret = LIB.SCIPcreateConsBasicSetcover(scip, pref, name, vars==null?0:vars.length, vars);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
		cons.setPointer(pref.getValue());
	}
	static SCIP_CONS createConsBasicSetcover(String name, SCIP_VAR[] vars) {
		SCIP_CONS cons = new SCIP_CONS();
		CALL_SCIPcreateConsBasicSetcover(scip, cons, name, vars);
		return cons;
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
	
	/* heur.h */
	double SCIPheurGetSetupTime(SCIP_HEUR scip_heur);
	static double heurGetSetupTime(SCIP_HEUR scip_heur) { return LIB.SCIPheurGetSetupTime(scip_heur); }
	
	double SCIPheurGetTime(SCIP_HEUR scip_heur);
	static double heurGetTime(SCIP_HEUR scip_heur) { return LIB.SCIPheurGetTime(scip_heur); }
	
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
	static SCIP_CONSHDLR includeConshdlrBasic(
			String name, String desc,
			int enfopriority, int chckpriority, int eagerfreq, boolean needscons,
			SCIP_DECL_CONSENFOLP consenfolp,
			SCIP_DECL_CONSENFOPS consenfops,
			SCIP_DECL_CONSCHECK conscheck,
			SCIP_DECL_CONSLOCK conslock,
			Pointer conshdlrdata//SCIP_CONSHDLRDATA 
		) {
		return CALL_SCIPincludeConshdlrBasic(JSCIP.scip, name, desc,
				enfopriority, chckpriority, eagerfreq, needscons, consenfolp, consenfops,
				conscheck, conslock, conshdlrdata);
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
	
	SCIP_RETCODE SCIPsetConshdlrTrans(SCIP scip, SCIP_CONSHDLR conshdlr, SCIP_DECL_CONSTRANS constrans);
	static void CALL_SCIPsetConshdlrTrans(SCIP scip, SCIP_CONSHDLR conshdlr, SCIP_DECL_CONSTRANS constrans) {
		SCIP_RETCODE ret = LIB.SCIPsetConshdlrTrans(scip, conshdlr, constrans);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
	}
	static void setConshdlrTrans(SCIP_CONSHDLR conshdlr, SCIP_DECL_CONSTRANS constrans) {
		CALL_SCIPsetConshdlrTrans(scip, conshdlr, constrans);
	}
	
	SCIP_RETCODE SCIPsetConshdlrProp(SCIP scip, SCIP_CONSHDLR conshdlr, SCIP_DECL_CONSPROP consprop,
			int propfreq, boolean delayprop, SCIP_PROPTIMING proptiming);
	static void CALL_SCIPsetConshdlrProp(SCIP scip, SCIP_CONSHDLR conshdlr, SCIP_DECL_CONSPROP consprop,
			int propfreq, boolean delayprop, SCIP_PROPTIMING proptiming) {
		SCIP_RETCODE ret = LIB.SCIPsetConshdlrProp(scip, conshdlr, consprop, propfreq, delayprop, proptiming);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
	}
	static void setConshdlrProp(SCIP_CONSHDLR conshdlr, SCIP_DECL_CONSPROP consprop,
			int propfreq, boolean delayprop, SCIP_PROPTIMING proptiming) {
		CALL_SCIPsetConshdlrProp(scip, conshdlr, consprop, propfreq, delayprop, proptiming);
	}
	
	/* scip_cut.h */
	SCIP_RETCODE SCIPaddRow(SCIP scip, SCIP_ROW row, boolean forcecut, ByteByReference infeasible);
	//Returns true if the row rendered problem infeasible 
	static boolean CALL_SCIPaddRow(SCIP scip, SCIP_ROW row, boolean forcecut) {
		ByteByReference bref = new ByteByReference();
		SCIP_RETCODE ret = LIB.SCIPaddRow(scip, row, forcecut, bref);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
		return bref.getValue() != 0;
	}
	static boolean addRow(SCIP_ROW row, boolean forcecut) { return CALL_SCIPaddRow(scip, row, forcecut); }
	
	SCIP_RETCODE SCIPaddPoolCut(SCIP scip, SCIP_ROW row);
	static void CALL_SCIPaddPoolCut(SCIP scip, SCIP_ROW row) {
		SCIP_RETCODE ret = LIB.SCIPaddPoolCut(scip, row);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
		return;
	}
	static void addPoolCut(SCIP_ROW row) {  CALL_SCIPaddPoolCut(scip, row); }
	
	boolean SCIPisCutEfficacious(SCIP scip, SCIP_SOL sol, SCIP_ROW cut);
	static boolean isCutEfficacious(SCIP_SOL sol, SCIP_ROW cut) {
		return JSCIP.LIB.SCIPisCutEfficacious(scip, sol, cut);
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
    static void free() { CALL_SCIPfree(scip); scip.setPointer(null); }
    
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
	
	SCIP_RETCODE SCIPaddVarToRow(SCIP scip, SCIP_ROW row, SCIP_VAR var, double val);
	static void CALL_SCIPaddVarToRow(SCIP scip, SCIP_ROW row, SCIP_VAR var, double val) {
		SCIP_RETCODE ret = LIB.SCIPaddVarToRow(scip, row, var, val);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
	}
	static void addVarToRow(SCIP_ROW row, SCIP_VAR var, double val) { CALL_SCIPaddVarToRow(scip, row, var, val); }
	
	SCIP_RETCODE SCIPreleaseRow(SCIP scip, PointerByReference row);
	static void CALL_SCIPreleaseRow(SCIP scip, SCIP_ROW row) {
		PointerByReference pref = new PointerByReference();
		pref.setValue(row.getPointer());
		SCIP_RETCODE ret = LIB.SCIPreleaseRow(scip, pref);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
//		row.setPointer(pref.getValue());//always returns null
	}
	static void releaseRow(SCIP_ROW row) { CALL_SCIPreleaseRow(scip, row); }
	
	SCIP_RETCODE SCIPcacheRowExtensions(SCIP scip, SCIP_ROW row);
	static void CALL_SCIPcacheRowExtensions(SCIP scip, SCIP_ROW row) {
		SCIP_RETCODE ret = LIB.SCIPcacheRowExtensions(scip, row);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
	}
	static void cacheRowExtensions(SCIP_ROW row) { CALL_SCIPcacheRowExtensions(scip, row); }
	
	SCIP_RETCODE SCIPflushRowExtensions(SCIP scip, SCIP_ROW row);
	static void CALL_SCIPflushRowExtensions(SCIP scip, SCIP_ROW row) {
		SCIP_RETCODE ret = LIB.SCIPflushRowExtensions(scip, row);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
	}
	static void flushRowExtensions(SCIP_ROW row) { CALL_SCIPflushRowExtensions(scip, row); }
	
	/* scip_heur.h */
	SCIP_RETCODE SCIPincludeHeurBasic(SCIP scip, PointerByReference scip_heur, String name, String desc,
			byte dispchar, int priority, int freq, int freqofs, int maxdepth, SCIP_HEURTIMING timingmask,
			boolean usessubscip, SCIP_DECL_HEUREXEC heurexec, SCIP_HEURDATA heurdata);
	static void CALL_SCIPincludeHeurBasic(SCIP scip, SCIP_HEUR scip_heur, String name, String desc,
			byte dispchar, int priority, int freq, int freqofs, int maxdepth, SCIP_HEURTIMING timingmask,
			boolean usessubscip, SCIP_DECL_HEUREXEC heurexec, SCIP_HEURDATA heurdata) {
		PointerByReference pref = new PointerByReference();
		pref.setValue(scip_heur.getPointer());
		SCIP_RETCODE ret = LIB.SCIPincludeHeurBasic(scip, pref, name, desc, dispchar, priority, freq,
				freqofs, maxdepth, timingmask, usessubscip, heurexec, heurdata);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
		scip_heur.setPointer(pref.getValue());
	}
	static SCIP_HEUR includeHeurBasic(String name, String desc, byte dispchar, int priority, int freq,
			int freqofs, int maxdepth, SCIP_HEURTIMING timingmask, boolean usessubscip,
			SCIP_DECL_HEUREXEC heurexec, SCIP_HEURDATA heurdata) {
		SCIP_HEUR scip_heur = new SCIP_HEUR();
		CALL_SCIPincludeHeurBasic(scip, scip_heur, name, desc, dispchar, priority, freq, freqofs, maxdepth,
				timingmask, usessubscip, heurexec, heurdata);
		return scip_heur;
	}
	
	SCIP_RETCODE SCIPsetHeurCopy(SCIP scip, SCIP_HEUR heur, SCIP_DECL_HEURCOPY heurcopy);
	SCIP_RETCODE SCIPsetHeurFree(SCIP scip, SCIP_HEUR heur, SCIP_DECL_HEURFREE heurfree);
	SCIP_RETCODE SCIPsetHeurInit(SCIP scip, SCIP_HEUR heur, SCIP_DECL_HEURINIT heurinit);
	SCIP_RETCODE SCIPsetHeurExit(SCIP scip, SCIP_HEUR heur, SCIP_DECL_HEUREXIT heurexit);
	SCIP_RETCODE SCIPsetHeurInitsol(SCIP scip, SCIP_HEUR heur, SCIP_DECL_HEURINITSOL heurinitsol);
	SCIP_RETCODE SCIPsetHeurExitsol(SCIP scip, SCIP_HEUR heur, SCIP_DECL_HEUREXITSOL heurexitsol);
	
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
	
	SCIP_RETCODE SCIPcreateSol(SCIP scip, PointerByReference sol, SCIP_HEUR heur);
	static void CALL_SCIPcreateSol(SCIP scip, SCIP_SOL sol, SCIP_HEUR heur) {
		PointerByReference pref = new PointerByReference();
		pref.setValue(sol.getPointer());
		SCIP_RETCODE ret = LIB.SCIPcreateSol(scip, pref, heur);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
		sol.setPointer(pref.getValue());
	}
	static SCIP_SOL createSol(SCIP_HEUR heur) {
		SCIP_SOL scip_sol = new SCIP_SOL();
		CALL_SCIPcreateSol(scip, scip_sol, heur);
		return scip_sol;
	}

	SCIP_RETCODE SCIPfreeSol(SCIP scip, PointerByReference sol);
	static void CALL_SCIPfreeSol(SCIP scip, SCIP_SOL sol) {
		PointerByReference pref = new PointerByReference();
		pref.setValue(sol.getPointer());
		SCIP_RETCODE ret = LIB.SCIPfreeSol(scip, pref);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
//		sol.setPointer(pref.getValue());//always returns null
	}
	static void freeSol(SCIP_SOL sol) {
		CALL_SCIPfreeSol(scip, sol);
	}
	
	SCIP_RETCODE SCIPsetSolVal(SCIP scip, SCIP_SOL sol, SCIP_VAR var, double val);
	static void CALL_SCIPsetSolVal(SCIP scip, SCIP_SOL sol, SCIP_VAR var, double val) {
		SCIP_RETCODE ret = LIB.SCIPsetSolVal(scip, sol, var, val);
		if(ret != SCIP_OKAY)
			throw new RuntimeException("Error, retcode "+ret);
	}
	static void setSolVal(SCIP_SOL sol, SCIP_VAR var, double val) {
		CALL_SCIPsetSolVal(scip, sol, var, val);	
	}

	SCIP_RETCODE SCIPtrySol(SCIP scip, SCIP_SOL sol, boolean printreason, boolean completely,
			boolean checkbounds, boolean checkintegrality, boolean checklprows,
			ByteByReference stored);
	//Returns true if solution was stored
	static boolean CALL_SCIPtrySol(SCIP scip, SCIP_SOL sol, boolean printreason, boolean completely,
			boolean checkbounds, boolean checkintegrality, boolean checklprows) {
		ByteByReference bref = new ByteByReference();
		SCIP_RETCODE ret = LIB.SCIPtrySol(scip, sol, printreason, completely, checkbounds,
				checkintegrality, checklprows, bref);
		return bref.getValue() != 0;
	}
	static boolean trySol(SCIP_SOL sol, boolean printreason, boolean completely,
			boolean checkbounds, boolean checkintegrality, boolean checklprows) {
		return CALL_SCIPtrySol(scip, sol, printreason, completely, checkbounds,
				checkintegrality, checklprows);
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
	
	SCIP_VARSTATUS SCIPvarGetStatus(SCIP_VAR var);
	static SCIP_VARSTATUS varGetStatus(SCIP_VAR var) { return LIB.SCIPvarGetStatus(var); }
	
	double SCIPvarGetObj(SCIP_VAR var);
	static double varGetObj(SCIP_VAR var) { return LIB.SCIPvarGetObj(var); }
	
	double SCIPvarGetLbLocal(SCIP_VAR var);
	static double varGetLbLocal(SCIP_VAR var) { return LIB.SCIPvarGetLbLocal(var); }
	
	double SCIPvarGetUbLocal(SCIP_VAR var);
	static double varGetUbLocal(SCIP_VAR var) { return LIB.SCIPvarGetUbLocal(var); }
	
	double SCIPvarGetLbGlobal(SCIP_VAR var);
	static double varGetLbGlobal(SCIP_VAR var) { return LIB.SCIPvarGetLbGlobal(var); }
	
	double SCIPvarGetUbGlobal(SCIP_VAR var);
	static double varGetUbGlobal(SCIP_VAR var) { return LIB.SCIPvarGetUbGlobal(var); }
	
	double SCIPvarGetLbOriginal(SCIP_VAR var);
	static double varGetLbOriginal(SCIP_VAR var) { return LIB.SCIPvarGetLbOriginal(var); }
	
	double SCIPvarGetUbOriginal(SCIP_VAR var);
	static double varGetUbOriginal(SCIP_VAR var) { return LIB.SCIPvarGetUbOriginal(var); }
	
	double SCIPvarGetLbLazy(SCIP_VAR var);
	static double varGetLbLazy(SCIP_VAR var) { return LIB.SCIPvarGetLbLazy(var); }
	
	double SCIPvarGetUbLazy(SCIP_VAR var);
	static double varGetUbLazy(SCIP_VAR var) { return LIB.SCIPvarGetUbLazy(var); }
	
	//Our own sketchy methods. This gets the SCIP_SET* field
	static Pointer SCIPset(SCIP scip) {
		return scip.getPointer().getPointer(8);
	}
}