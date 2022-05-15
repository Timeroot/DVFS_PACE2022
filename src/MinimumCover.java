import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;

import com.sun.jna.ptr.IntByReference;

import JNA_SCIP.*;

import static JNA_SCIP.SCIP_LOCKTYPE.SCIP_LOCKTYPE_MODEL;
import static JNA_SCIP.SCIP_PARAMEMPHASIS.*;
import static JNA_SCIP.SCIP_PARAMSETTING.*;
import static JNA_SCIP.SCIP_RETCODE.*;
import static JNA_SCIP.SCIP_STATUS.*;
import static JNA_SCIP.SCIP_RESULT.*;
import static JNA_SCIP.SCIP_VARTYPE.*;

public class MinimumCover {

	//Solves the problem via a minimum cover problem.
	//Uses SCIP, gives a good heuristic solution, and doesn't require callbacks.

	int N;
	SCIP_VAR[] vars;
	double inf;
	
	static final boolean ECHO = true;
	static final boolean ECHO_SETTINGS = false;
	
	//if true, uses the negation of each variable -- might be better for some heuristics?
	static final boolean NEGATE = true;
	
	//How strong to presolve?
	static final SCIP_PARAMSETTING presolve_emph = SCIP_PARAMSETTING_AGGRESSIVE;
	static final SCIP_PARAMEMPHASIS solve_emph = SCIP_PARAMEMPHASIS_OPTIMALITY;
	
	public ArrayList<Integer> solve(Graph orig, ArrayList<int[]> pairList, ArrayList<int[]> bigCycleList) {
		if(Main_Load.TESTING)
			System.out.println("Solving with MinimumCover");
		
		N = orig.N;
		
		//SCIP initialization
		JSCIP.create();
		inf = JSCIP.infinity();
		if(ECHO_SETTINGS && ECHO)
			JSCIP.printVersion(null);
		JSCIP.includeDefaultPlugins();
		JSCIP.createProbBasic("prob");
		if(!ECHO)
			JSCIP.setIntParam("display/verblevel", 0);
		
		JSCIP.setPresolving(presolve_emph, !(ECHO_SETTINGS && ECHO));
		JSCIP.setEmphasis(solve_emph, !(ECHO_SETTINGS && ECHO));
		
		
		//Allocate variables
		vars = new SCIP_VAR[N];
		for(int i=0; i<N; i++) {
			//Each variable has objective weight of 1.0
			vars[i] = JSCIP.createVarBasic("v"+i, 0, 1, NEGATE?-1.0:1.0, SCIP_VARTYPE_BINARY);
			JSCIP.addVar(vars[i]);
		}
		
		for(int[] pair : pairList) {
			addLinearCons(pair);
		}
		
		for(int[] cycle : bigCycleList) {
			addLinearCons(cycle);
		}

		/*
		SCIP_DECL_HEUREXEC heurexec =  this::heurexec;
		scip_heur = JSCIP.includeHeurBasic("javaheur", "java-side heuristic", (byte)'j',
				HEUR_PRIORITY, HEUR_FREQ, HEUR_FREQOFS, HEUR_MAXDEPTH, HEUR_TIMING,
				false, heurexec, null);
		
		ArrayList<Integer> heurSol = ExactSolver_Heuristic.solve(heurSolGraph);
		System.out.println("Got a solution with N="+heurSol.size()+", tC="+trueCount+", fC="+falseCount);
		*/
		
		JSCIP.solve();
		
		SCIP_STATUS scip_status = JSCIP.getStatus();
		System.out.println("Final status "+scip_status);
		
		//Save the variables
		SCIP_SOL sol = JSCIP.getBestSol();
		ArrayList<Integer> res = extractSol(sol);
		
		//SCIP cleanup
		for(int i=0; i<N; i++) {
			JSCIP.releaseVar(vars[i]);
		}
		JSCIP.free();
		
		return res;
	}
	
	ArrayList<Integer> extractSol(SCIP_SOL sol){
		ArrayList<Integer> res = new ArrayList<>();
		res.clear();		
		for(int i=0; i<N; i++) {
			double val = JSCIP.getSolVal(sol, vars[i]);
			if((val > 0.5) ^ NEGATE) {
				res.add(i);
			}
		}
		return res;
	}

	void addLinearCons(int[] cycle) {
		SCIP_CONS cons = NEGATE?
				JSCIP.createConsBasicLinear("linCyc"+cycle.length, null, null, -inf, cycle.length-1):
				JSCIP.createConsBasicLinear("linCyc"+cycle.length, null, null, 1, inf);
		for(int v : cycle) {
			JSCIP.addCoefLinear(cons, vars[v], 1.0);
		}
		JSCIP.addCons(cons);
		JSCIP.releaseCons(cons);
	}
	
	void addSetppcCons(int[] cycle) {
		SCIP_VAR[] con_vars = new SCIP_VAR[cycle.length];
		for(int i=0; i<cycle.length; i++)
			con_vars[i] = vars[i]; 
		SCIP_CONS cons = JSCIP.createConsBasicSetcover("ppcCyc"+cycle.length, con_vars);
		JSCIP.addCons(cons);
		JSCIP.releaseCons(cons);
	}
	
	void addLogicorCons(int[] cycle) {
		SCIP_VAR[] con_vars = new SCIP_VAR[cycle.length];
		for(int i=0; i<cycle.length; i++)
			con_vars[i] = vars[i]; 
		SCIP_CONS cons = JSCIP.createConsBasicLogicor("lorCyc"+cycle.length, con_vars);
		JSCIP.addCons(cons);
		JSCIP.releaseCons(cons);
	}
}
