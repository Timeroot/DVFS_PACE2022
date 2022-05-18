import JNA_SCIP.*;

import static JNA_SCIP.SCIP_PARAMEMPHASIS.*;
import static JNA_SCIP.SCIP_PARAMSETTING.*;
import static JNA_SCIP.SCIP_RETCODE.*;
import static JNA_SCIP.SCIP_STATUS.*;
import static JNA_SCIP.SCIP_RESULT.*;
import static JNA_SCIP.SCIP_VARTYPE.*;

//Solves the problem via a minimum cover problem. Assumes there's no chunks left.
//Uses SCIP for the ILP, and provides a heuristic.
public class ILP_MinimumCover {

	int N;
	SCIP_VAR[] vars;
	double inf;

	static final boolean ECHO = false;//print progress + versioning to STDOUT
	static final boolean ECHO_SETTINGS = false;//print configurations to STDOUT
	
	//if true, uses the negation of each variable -- might be better for some heuristics?
	static final boolean NEGATE = false;
	
	//How strong to presolve?
	static final SCIP_PARAMSETTING presolve_emph = SCIP_PARAMSETTING_AGGRESSIVE;
	
	//We only care about finding the optimum ASAP, because when we do, we're done.
	static final SCIP_PARAMEMPHASIS solve_emph = SCIP_PARAMEMPHASIS_OPTIMALITY;
	
	@SuppressWarnings("unused")
	public boolean[] solve(MinimumCoverInfo mci) {
		if(Main_Load.TESTING)
			System.out.println("Solving with ILP_MinimumCover");
		
		if(mci.chunks.size() > 0)
			throw new RuntimeException("ILP_MinimumCover doesn't support chunks");
		
		N = mci.N;
		
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
		
		for(int[] pair : mci.pairList) {
			addLinearCons(pair);
		}
		
		for(int[] cycle : mci.bigCycleList) {
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
		if(Main_Load.TESTING)
			System.out.println("Final status "+scip_status);
		
		//Save the variables
		SCIP_SOL sol = JSCIP.getBestSol();
		boolean[] res = extractSol(sol);
		
		//SCIP cleanup
		for(int i=0; i<N; i++) {
			JSCIP.releaseVar(vars[i]);
		}
		JSCIP.free();
		
		return res;
	}
	
	boolean[] extractSol(SCIP_SOL sol){
		boolean[] res = new boolean[N];
		for(int i=0; i<N; i++) {
			double val = JSCIP.getSolVal(sol, vars[i]);
			if((val > 0.5) ^ NEGATE) {
				res[i] = true;
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
