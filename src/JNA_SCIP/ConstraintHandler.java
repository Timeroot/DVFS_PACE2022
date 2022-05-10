package JNA_SCIP;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;

import com.sun.jna.Pointer;
import com.sun.jna.Memory;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

public abstract class ConstraintHandler<Data extends ConstraintData<Data>> {

	public SCIP_CONSHDLR conshdlr;
	HashMap<SCIP_CONS, Data> mapping;
	Class<Data> d_class;
	
	//We manage the data associated to the constraints on the Java side (in `mapping`) but
	//consdelete isn't called -- and we'll never know when a constraint gets deleted --
	//unless we pass a nonnull value for consdata. We keep this single byte allocated and
	//all constraints share it as their consdata. When we 'delete' it, we just stop pointing
	//to it.
	static Pointer dummyConsdata = new Memory(1);
	
	//We need to keep hard references here to keep the callbacks from being garbage collected
	private SCIP_DECL_CONSENFOLP consenfolp = this::consenfolp;
	private SCIP_DECL_CONSENFOPS consenfops = this::consenfops;
	private SCIP_DECL_CONSCHECK  conscheck = this::conscheck;
	private SCIP_DECL_CONSLOCK   conslock = this::conslock;
	private SCIP_DECL_CONSDELETE consdelete = this::consdelete;
	private SCIP_DECL_CONSTRANS  constrans = this::constrans;
	private SCIP_DECL_CONSPROP   consprop = this::consprop;

	/* Define a constraint handler. Needs:
	 * @param builder	Constructor for constraints
	 * @param copier	Copy-constructor for transformed problems
	 * @param t_clazz	A reference to the Cons (T) type class
	 * @param name		Name of the constraint handler
	 * @param desc		Description
	 * @param enfopriority	Enforcement priority
	 * @param chckpriority	Check priority
	 * @param eagerfreq		Frequency of eager checks
	 * @param needscons		Does this constraint handler need constraints to run?
	 */
	public ConstraintHandler(Class<Data> d_class, String name, String desc,
			int enfopriority, int chckpriority, int eagerfreq, boolean needscons) {
		this.d_class = d_class;
		mapping = new HashMap<>();
		
		conshdlr = JSCIP.CALL_SCIPincludeConshdlrBasic(JSCIP.scip, name, desc,
				enfopriority, chckpriority, eagerfreq, needscons,
				consenfolp, consenfops, conscheck, conslock, null);
		JSCIP.setConshdlrDelete(conshdlr, consdelete);
		JSCIP.setConshdlrTrans(conshdlr, constrans);
		
	}
	
	/* Make a constraint with given name and data, and given flags. */
	public SCIP_CONS instantiate(String name, boolean initial, boolean separate,
			boolean enforce, boolean check, boolean propagate, boolean local, boolean modifiable,
			boolean dynamic, boolean removable, boolean stickingatnode, Data data) {

		SCIP_CONS cons = JSCIP.createCons(name, conshdlr, dummyConsdata, initial, separate, enforce, check,
				propagate, local, modifiable, dynamic, removable, stickingatnode);
		
		mapping.put(cons, data);

		JSCIP.addCons(cons);
		JSCIP.releaseCons(cons);
		return cons;
	}
	
	/* Make a constraint with given name and data, and default flags. */
	public SCIP_CONS instantiate(String name, Data con_args) {
		return instantiate(name, false, true, true, true, true, false, false, false, true, true, con_args);
	}
	
	/* Enable optional methods, like consprop */
	public void enableConsprop(int propfreq, boolean delayprop, SCIP_PROPTIMING proptiming) {
		JSCIP.setConshdlrProp(conshdlr, consprop, propfreq, delayprop, proptiming);
	}
	public void disableConsprop() {
		//propfreq == -1 means "disable".
		JSCIP.setConshdlrProp(conshdlr, null, -1, true, SCIP_PROPTIMING.SCIP_PROPTIMING_NEVER);
	}
	
	/* Define a series of handler that simplify types and do sanity checking */
	
	//CONSCHECK
	public abstract SCIP_RESULT conscheck(Data[] conss, SCIP_SOL sol, boolean checkintegrality,
			boolean checklprows, boolean printreason, boolean completely);
	
	public SCIP_RETCODE conscheck(SCIP scip, SCIP_CONSHDLR conshdlr, Pointer conss,//SCIP_CONS**
	    	int nconss,
	    	SCIP_SOL sol, boolean checkintegrality, boolean checklprows, boolean printreason, boolean completely,
	    	IntByReference scip_result//SCIP_RESULT*
    ) {
		//Sanity check on conshdlr
		if(!conshdlr.equals(this.conshdlr)) {
			System.err.println("Unexpected conshdlr "+conshdlr+" != "+this.conshdlr);
			return SCIP_RETCODE.SCIP_INVALIDDATA;
		}
		//Call implementation with try{} wrapping
		try {
			@SuppressWarnings("unchecked")
			Data[] t_arr = (Data[])Array.newInstance(d_class, nconss);
			for(int i=0; i<nconss; i++) {
				SCIP_CONS scip_cons = new SCIP_CONS(conss.getPointer(8 * i));
				t_arr[i] = mapping.get(scip_cons);
			}
			SCIP_RESULT res = conscheck(t_arr, sol, checkintegrality, checklprows, printreason, completely);
			scip_result.setValue(res.ordinal());
			
		} catch (RuntimeException e) {
			e.printStackTrace();
			return SCIP_RETCODE.SCIP_ERROR;
		}
		return SCIP_RETCODE.SCIP_OKAY;
	}
	
	//CONSLOCK
	public abstract SCIP_RETCODE conslock(Data cons, SCIP_LOCKTYPE locktype, int nlockspos, int nlocksneg);

	public SCIP_RETCODE conslock(SCIP scip, SCIP_CONSHDLR conshdlr, SCIP_CONS scip_cons, SCIP_LOCKTYPE locktype,
			int nlockspos, int nlocksneg) {
		//Sanity check on conshdlr
		if(!conshdlr.equals(this.conshdlr)) {
			System.err.println("Unexpected conshdlr "+conshdlr+" != "+this.conshdlr);
			return SCIP_RETCODE.SCIP_INVALIDDATA;
		}
		//Call implementation
		return conslock(mapping.get(scip_cons), locktype, nlockspos, nlocksneg);
	}

	//CONSENFOPS
	public abstract SCIP_RESULT consenfops(Data[] conss, int nusefulconss, boolean solinfeasible,
			boolean objinfeasible);

	public SCIP_RETCODE consenfops(SCIP scip, SCIP_CONSHDLR conshdlr, Pointer conss, int nconss, int nusefulconss,
			boolean solinfeasible, boolean objinfeasible, IntByReference scip_result) {
		//Sanity check on conshdlr
		if(!conshdlr.equals(this.conshdlr)) {
			System.err.println("Unexpected conshdlr "+conshdlr+" != "+this.conshdlr);
			return SCIP_RETCODE.SCIP_INVALIDDATA;
		}
		//Call implementation with try{} wrapping
		try {	
			@SuppressWarnings("unchecked")
			Data[] t_arr = (Data[])Array.newInstance(d_class, nconss);
			for(int i=0; i<nconss; i++) {
				SCIP_CONS scip_cons = new SCIP_CONS(conss.getPointer(8 * i));
				t_arr[i] = mapping.get(scip_cons);
			}
			SCIP_RESULT res = consenfops(t_arr, nusefulconss, solinfeasible, objinfeasible);
			scip_result.setValue(res.ordinal());
		} catch (RuntimeException e) {
			e.printStackTrace();
			return SCIP_RETCODE.SCIP_ERROR;
		}
		return SCIP_RETCODE.SCIP_OKAY;
	}
	
	//CONSENFOLP
	public abstract SCIP_RESULT consenfolp(Data[] conss, int nusefulconss, boolean solinfeasible);

	public SCIP_RETCODE consenfolp(SCIP scip, SCIP_CONSHDLR conshdlr, Pointer conss, int nconss, int nusefulconss,
			boolean solinfeasible, IntByReference scip_result) {
		//Sanity check on conshdlr
		if(!conshdlr.equals(this.conshdlr)) {
			System.err.println("Unexpected conshdlr "+conshdlr+" != "+this.conshdlr);
			return SCIP_RETCODE.SCIP_INVALIDDATA;
		}
		//Call implementation with try{} wrapping
		try {	
			@SuppressWarnings("unchecked")
			Data[] t_arr = (Data[])Array.newInstance(d_class, nconss);
			for(int i=0; i<nconss; i++) {
				SCIP_CONS scip_cons = new SCIP_CONS(conss.getPointer(8 * i));
				t_arr[i] = mapping.get(scip_cons);
			}
			SCIP_RESULT res = consenfolp(t_arr, nusefulconss, solinfeasible);
			scip_result.setValue(res.ordinal());
		} catch (RuntimeException e) {
			e.printStackTrace();
			return SCIP_RETCODE.SCIP_ERROR;
		}
		return SCIP_RETCODE.SCIP_OKAY;
	}
	
	//CONSDELETE
	public abstract SCIP_RETCODE consdelete(Data cons);
	
	public SCIP_RETCODE consdelete(SCIP scip, SCIP_CONSHDLR conshdlr, SCIP_CONS scip_cons, PointerByReference consdata) {
		//Sanity check on conshdlr
		if(!conshdlr.equals(this.conshdlr)) {
			System.err.println("Unexpected conshdlr "+conshdlr+" != "+this.conshdlr);
			return SCIP_RETCODE.SCIP_INVALIDDATA;
		}
		consdata.setValue(null);
		//Call implementation with try{} wrapping
		return consdelete(mapping.remove(scip_cons));
	}
	
	//CONSTRANS
	public SCIP_RETCODE constrans(SCIP scip, SCIP_CONSHDLR conshdlr, SCIP_CONS sourcecons, PointerByReference targetcons) {
		if(!conshdlr.equals(this.conshdlr)) {
			System.err.println("Unexpected conshdlr "+conshdlr+" != "+this.conshdlr);
			return SCIP_RETCODE.SCIP_INVALIDDATA;
		}
		//Default implementation: create new constraint with identical flags, and copy the data. 
		String name = JSCIP.consGetName(sourcecons);
		boolean initial = JSCIP.consIsInitial(sourcecons),
				separate = JSCIP.consIsSeparated(sourcecons),
				enforce = JSCIP.consIsEnforced(sourcecons),
				check = JSCIP.consIsChecked(sourcecons),
				propagate = JSCIP.consIsPropagated(sourcecons),
				local = JSCIP.consIsLocal(sourcecons),
				modifiable = JSCIP.consIsModifiable(sourcecons),
				dynamic = JSCIP.consIsDynamic(sourcecons),
				removable = JSCIP.consIsRemovable(sourcecons),
				stickingatnode = JSCIP.consIsStickingAtNode(sourcecons);
				
		SCIP_CONS copy_scip_cons = JSCIP.createCons(
				name, conshdlr, null, initial, separate, enforce, check,
				propagate, local, modifiable, dynamic, removable,
				stickingatnode);
		
		Data orig_cons = mapping.get(sourcecons);
		Data copy_cons = orig_cons.copy(); 
		
		targetcons.setValue(copy_scip_cons.getPointer());
		mapping.put(copy_scip_cons, copy_cons);
		
		return SCIP_RETCODE.SCIP_OKAY;
	}
	
	//CONSPROP
	//default do-nothing implementation. Override if you're going to enable it
	public SCIP_RESULT consprop(Data[] conss, int nusefulconss, int nmarkedconss, SCIP_PROPTIMING proptiming) {
		throw new RuntimeException("Consprop was enabled but you didn't override the default implementation.");
//		return SCIP_RESULT.SCIP_DIDNOTRUN;
	}
	
	public SCIP_RETCODE consprop(SCIP scip, SCIP_CONSHDLR conshdlr, Pointer conss, int nconss, int nusefulconss,
		      int nmarkedconss, SCIP_PROPTIMING proptiming, IntByReference scip_result) {
		//Sanity check on conshdlr
		if(!conshdlr.equals(this.conshdlr)) {
			System.err.println("Unexpected conshdlr "+conshdlr+" != "+this.conshdlr);
			return SCIP_RETCODE.SCIP_INVALIDDATA;
		}
		//Call implementation with try{} wrapping
		try {	
			@SuppressWarnings("unchecked")
			Data[] t_arr = (Data[])Array.newInstance(d_class, nconss);
			for(int i=0; i<nconss; i++) {
				SCIP_CONS scip_cons = new SCIP_CONS(conss.getPointer(8 * i));
				t_arr[i] = mapping.get(scip_cons);
			}
			SCIP_RESULT res = consprop(t_arr, nusefulconss, nmarkedconss, proptiming);
			scip_result.setValue(res.ordinal());
		} catch (RuntimeException e) {
			e.printStackTrace();
			return SCIP_RETCODE.SCIP_ERROR;
		}
		return SCIP_RETCODE.SCIP_OKAY;
	}
}
