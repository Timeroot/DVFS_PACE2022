package JNA_SCIP.Examples;

import static JNA_SCIP.SCIP_VARTYPE.*;

import static JNA_SCIP.SCIP_RETCODE.*;
import static JNA_SCIP.SCIP_LOCKTYPE.*;

import JNA_SCIP.*;

public class Conshdlr_JSCIP {

	//Toy example of using a custom constraint handler using JSCIP's convenience methods.
	//The problem we solve is:
	// Maximize x
	// 0 <= x <= 100
	// Integer x
	// with a custom constraint enforcing x <= 67.5.
	//This constraint handler is a bit smarter, so that we don't have search blindly:
	//when given an infeasible solution, it will provide a cutting plane on the nearest
	//multiple of 10. For instance, it will refute the solution 83 by a cutting plane x<=80.
	
	public static void main(String[] args) {
		
		JSCIP.create();
		JSCIP.includeDefaultPlugins();

		JSCIP.createProbBasic("conshdlr_example");
		
		SCIP_VAR x = JSCIP.createVarBasic("x", 0, 100, -1, SCIP_VARTYPE_INTEGER);
		JSCIP.addVar(x);

		MyConshdlr conshdlr = new MyConshdlr();
		
		conshdlr.consprop(null, conshdlr.conshdlr, null, 0, 0, 0, null, null);

		MyCons cons_data = new MyCons(x);
		SCIP_CONS scip_cons = conshdlr.instantiate("mycons", cons_data);
		System.out.println("Made scip_cons "+scip_cons);
		
		//Release the variable now that we're done with it
		JSCIP.releaseVar(x);
		
		JSCIP.solve();
		JSCIP.printBestSol(null, true);

		JSCIP.free();
	}
	
	static class MyCons extends ConstraintData<MyCons> {
		
		private SCIP_VAR var;
		
		public MyCons(SCIP_VAR var) {
			this.var = var;
			JSCIP.captureVar(var);
		}
		
		public SCIP_VAR var() { return var; }
		
		public void delete() {
			System.out.println(this+"  deleted");
			System.out.println("Must now release "+var());
			JSCIP.releaseVar(var());
		}
		
		public String toString() {
			return "MyCons{var="+JSCIP.varGetName(var)+"}";
		}
		
		@Override
		public MyCons copy() {
			System.out.println("CONSTRANS is copying "+this);
			//in this case, our constraint data (var) is never modified, so we can just keep
			//the same ConstraintData.
			return this;
			//If it was something we needed to change later, we could use a copy like this:
			//return new MyCons(var);
			//make sure to NOT capture on copy, as the copy won't have delete().
		}
	}
	
	static class MyConshdlr extends ConstraintHandler<MyCons> {
		public MyConshdlr() {
			super(MyCons.class, "MyConshdlr", "my custom handler", -1, -1, 1, true);
		}
		
		//Tries to find a cut. SCIP_CUTOFF if it rendered problem infeasible,
		//SCIP_SEPARATED if it found an efficacious cut. null if no cut.
		//(This isn't SCIP API, just our own method internal to this callback.)
		public SCIP_RESULT findCut(SCIP_VAR var, double x_val) {
			double cut_val = Math.floor((x_val-0.1)/10)*10;
			if(cut_val <= 67.5)
				return null;
			
			SCIP_ROW row = JSCIP.createEmptyRowConshdlr(this.conshdlr, "x10row", -1000, cut_val, false, false, true);
			JSCIP.cacheRowExtensions(row);
			JSCIP.addVarToRow(row, var, 1);
			JSCIP.flushRowExtensions(row);
			System.out.println("Cut with "+cut_val);
			
			boolean is_infeasible = JSCIP.addRow(row, false);
			JSCIP.releaseRow(row);
			
			if(is_infeasible) {
				return SCIP_RESULT.SCIP_CUTOFF;
			} else {
				return SCIP_RESULT.SCIP_SEPARATED;
			}
		}

		@Override
		public SCIP_RESULT conscheck(MyCons[] conss, SCIP_SOL sol, boolean checkintegrality, boolean checklprows,
				boolean printreason, boolean completely) {
			
			for(MyCons cons : conss) {
				double x_val = JSCIP.getSolVal(sol, cons.var());
				System.out.println("CONSCHECK, x = "+x_val);

				boolean is_ok = x_val <= 67.5;
				if(!is_ok)
					return SCIP_RESULT.SCIP_INFEASIBLE;
			}
			return SCIP_RESULT.SCIP_FEASIBLE;
		}

		@Override
		public SCIP_RETCODE conslock(MyCons cons, SCIP_LOCKTYPE locktype, int nlockspos, int nlocksneg) {
			System.out.println("CONSLOCK called on "+cons);
			JSCIP.addVarLocksType(cons.var(), SCIP_LOCKTYPE_MODEL, nlocksneg, nlockspos);
			return SCIP_OKAY;
		}

		@Override
		public SCIP_RESULT consenfops(MyCons[] conss, int nusefulconss, boolean solinfeasible, boolean objinfeasible) {
			System.out.println("CONSENFOPS called");
			//This is often implemented similarly to consenfolp, but on this simple problem it's never actually
			//called -- we never hit a pseudosolution (PS). But if we did, we could just forward it to our consenfolp
			//implementation.
			return consenfolp(conss, nusefulconss, solinfeasible);
		}

		@Override
		public SCIP_RESULT consenfolp(MyCons[] conss, int nusefulconss, boolean solinfeasible) {
			
			for(MyCons cons : conss) {
				double x_val = JSCIP.getSolVal(null, cons.var());//no sol provided, use "null" here
				System.out.println("CONSENFOLP, x = "+x_val);

				boolean is_ok = x_val <= 67.5;
				if(!is_ok) {
					SCIP_RESULT cut_res = findCut(cons.var(), x_val);
					if(cut_res != null)
						return cut_res;
					else
						return SCIP_RESULT.SCIP_INFEASIBLE;
				}
			}
			return SCIP_RESULT.SCIP_FEASIBLE;
		}

		@Override
		public SCIP_RETCODE consdelete(MyCons cons) {
			System.out.println("CONSDELETE called on "+cons);
			cons.delete();
			return SCIP_OKAY;
		}
		
		@Override
		public SCIP_RESULT consprop(MyCons[] conss, int nusefulconss, int nmarkedconss, SCIP_PROPTIMING proptiming) {
			throw new RuntimeException("Override conspropr implementation fired!");
//			return SCIP_RESULT.SCIP_DIDNOTRUN;
		}
	}
}