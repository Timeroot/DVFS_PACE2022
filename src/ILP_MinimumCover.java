import JNA_SCIP.*;

import static JNA_SCIP.SCIP_PARAMEMPHASIS.*;
import static JNA_SCIP.SCIP_PARAMSETTING.*;
import static JNA_SCIP.SCIP_VARTYPE.*;

//Solves the problem via a minimum cover problem. Assumes there's no chunks left.
//Uses SCIP for the ILP, and provides a heuristic.
public class ILP_MinimumCover {

	int N;
	SCIP_VAR[] vars;
	double inf;

	static final boolean ECHO = true && Main_Load.TESTING;//print progress + versioning to STDOUT
	static final boolean ECHO_SETTINGS = false;//print configurations to STDOUT
	
	//if true, uses the negation of each variable -- might be better for some heuristics?
	static final boolean NEGATE = false;
	
	//How strong to presolve?
	static final SCIP_PARAMSETTING presolve_emph = AGGRESSIVE;
	
	//We only care about finding the optimum ASAP, because when we do, we're done.
	static final SCIP_PARAMEMPHASIS solve_emph = OPTIMALITY;
	
	@SuppressWarnings("unused")
	public boolean[] solve(MinimumCoverInfo mci) {
		if(Main_Load.TESTING) {
			System.out.println("Solving with ILP_MinimumCover.");
			System.out.println(mci.pairList.size()+" K2s, "+mci.bigCycleList.size()+" cycles");
		}
		
		if(mci.chunks.size() > 0)
			throw new RuntimeException("ILP_MinimumCover doesn't support chunks");
		
		N = mci.N;
		
		//SCIP initialization
		SCIP scip = SCIP.create();
		inf = scip.infinity();
		if(ECHO_SETTINGS && ECHO)
			scip.printVersion(null);
		scip.includeDefaultPlugins();
		scip.createProbBasic("prob");
		
		scip.setIntParam("display/verblevel", 4);
		if(!ECHO)
			scip.setIntParam("display/verblevel", 0);
		
		scip.setPresolving(presolve_emph, !(ECHO_SETTINGS && ECHO));
		scip.setEmphasis(solve_emph, !(ECHO_SETTINGS && ECHO));

		
		//Allocate variables
		vars = new SCIP_VAR[N];
		for(int i=0; i<N; i++) {
			//Each variable has objective weight of 1.0
			vars[i] = scip.createVarBasic("v"+i, 0, 1, NEGATE?-1.0:1.0, BINARY);
			scip.addVar(vars[i]);
		}
		
		for(int[] pair : mci.pairList) {
			addLinearCons(scip, pair);
		}
		
		for(int[] cycle : mci.bigCycleList) {
			addLinearCons(scip, cycle);
		}

		/*
		SCIP_DECL_HEUREXEC heurexec =  this::heurexec;
		scip_heur = scip.includeHeurBasic("javaheur", "java-side heuristic", (byte)'j',
				HEUR_PRIORITY, HEUR_FREQ, HEUR_FREQOFS, HEUR_MAXDEPTH, HEUR_TIMING,
				false, heurexec, null);
		
		ArrayList<Integer> heurSol = ExactSolver_Heuristic.solve(heurSolGraph);
		System.out.println("Got a solution with N="+heurSol.size()+", tC="+trueCount+", fC="+falseCount);
		*/
		
		scip.solve();
		
		SCIP_STATUS scip_status = scip.getStatus();
		if(Main_Load.TESTING)
			System.out.println("Final status "+scip_status);
		
		//Save the variables
		SCIP_SOL sol = scip.getBestSol();
		boolean[] res = extractSol(scip, sol);
		
		//SCIP cleanup
		for(int i=0; i<N; i++) {
			scip.releaseVar(vars[i]);
		}
		scip.free();
		
		return res;
	}
	
	boolean[] extractSol(SCIP scip, SCIP_SOL sol){
		boolean[] res = new boolean[N];
		for(int i=0; i<N; i++) {
			double val = scip.getSolVal(sol, vars[i]);
			if((val > 0.5) ^ NEGATE) {
				res[i] = true;
			}
		}
		return res;
	}

	void addLinearCons(SCIP scip, int[] cycle) {
		SCIP_CONS cons = NEGATE?
				scip.createConsBasicLinear("linCyc"+cycle.length, null, null, -inf, cycle.length-1):
				scip.createConsBasicLinear("linCyc"+cycle.length, null, null, 1, inf);
		for(int v : cycle) {
			scip.addCoefLinear(cons, vars[v], 1.0);
		}
		scip.addCons(cons);
		scip.releaseCons(cons);
	}
	
	void addSetppcCons(SCIP scip, int[] cycle) {
		SCIP_VAR[] con_vars = new SCIP_VAR[cycle.length];
		for(int i=0; i<cycle.length; i++)
			con_vars[i] = vars[i]; 
		SCIP_CONS cons = scip.createConsBasicSetcover("ppcCyc"+cycle.length, con_vars);
		scip.addCons(cons);
		scip.releaseCons(cons);
	}
	
	void addLogicorCons(SCIP scip, int[] cycle) {
		SCIP_VAR[] con_vars = new SCIP_VAR[cycle.length];
		for(int i=0; i<cycle.length; i++)
			con_vars[i] = vars[i]; 
		SCIP_CONS cons = scip.createConsBasicLogicor("lorCyc"+cycle.length, con_vars);
		scip.addCons(cons);
		scip.releaseCons(cons);
	}
}
