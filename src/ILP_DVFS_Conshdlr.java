import java.util.ArrayDeque;
import java.util.ArrayList;

import com.sun.jna.ptr.IntByReference;

import JNA_SCIP.*;

import static JNA_SCIP.SCIP_LOCKTYPE.MODEL;
import static JNA_SCIP.SCIP_PARAMEMPHASIS.*;
import static JNA_SCIP.SCIP_PARAMSETTING.*;
import static JNA_SCIP.SCIP_RETCODE.*;
import static JNA_SCIP.SCIP_RESULT.*;
import static JNA_SCIP.SCIP_VARTYPE.*;

//Solves the DVFS problem using the ILP formulation. Since there can be exponentially
//many cycles, this implements a constraint handler (conshdlr) that is periodically given
//candidate solutions, and then adds cuts as needed. Also adds heuristics.
public class ILP_DVFS_Conshdlr {

	int N;
	SCIP_VAR[] vars;
	Graph g;
	double inf;
	SCIP_HEUR scip_heur;

	static final boolean ECHO = false;//print progress + versioning to STDOUT
	static final boolean ECHO_SETTINGS = true;//print configurations to STDOUT
	
	//How strong to presolve?
	static final SCIP_PARAMSETTING presolve_emph = AGGRESSIVE;
	static final SCIP_PARAMEMPHASIS solve_emph = OPTIMALITY;
	
	//Which types of constraints to add for initial set (K2's and K3's)
	static final boolean INIT_USE_LINEAR = true;
	static final boolean INIT_USE_SETPPC = true;
	static final boolean INIT_USE_LOGICOR = false;

	//Which types of constraints to add in the constraint callback
	static final boolean CALLBACK_ROW = false;
	static final boolean CALLBACK_GLOBALCUT = true;
	static final boolean CALLBACK_LINCONS = false;
	static final boolean CALLBACK_SETPPCCONS = false;
	static final boolean CALLBACK_LOGICORCONS = false;
	
	//Should new constraints be added in the CONSCHECK, where it's not strictly needed?
	static final boolean CONSCHECK_ADDCONS = true;
	
	//How frequently/early should the constraint handler be called?
	static final int CONSHDLR_ENFOPRIORITY = -1;
	static final int CONSHDLR_CHCKPRIORITY = -1000000;
	
	//How frequently/early to call the heuristic
	static final int HEUR_PRIORITY = 1000;
	static final int HEUR_FREQ = 0;
	static final int HEUR_FREQOFS = 0;
	static final int HEUR_MAXDEPTH = -1;
	static final SCIP_HEURTIMING HEUR_TIMING = SCIP_HEURTIMING.BEFORENODE;

	@SuppressWarnings("unused")
	static void checkOptions(){
		if(CALLBACK_ROW && CALLBACK_GLOBALCUT)
			throw new RuntimeException("Should only have one of CALLBACK_ROW and CALLBACK_GLOBALCUT true");
		if(!(CALLBACK_ROW || CALLBACK_GLOBALCUT || CALLBACK_LOGICORCONS || CALLBACK_SETPPCCONS || CALLBACK_LINCONS))
			throw new RuntimeException("Must have one of the CALLBACK_* options on, otherwise can't improve");
		if(CONSHDLR_ENFOPRIORITY >= 0 || CONSHDLR_CHCKPRIORITY >= 0)
			throw new RuntimeException("CONSHDLR_ENFOPRIORITY and CONSHDLR_CHCKPRIORITY must be negative in order to handle only integral solutions");
	}
	static { checkOptions(); }//run at class load time
	
	public ArrayList<Integer> solve(Graph g) {
		if(Main_Load.TESTING)
			System.out.println("Solving with ILP_DVFS_Conshdlr");
		
		this.g = g;
		this.N = g.N;
		
		//SCIP initialization
		SCIP scip = SCIP.create();
		this.inf = scip.infinity();
		if(ECHO)
			scip.printVersion(null);
		scip.includeDefaultPlugins();
		scip.createProbBasic("prob");
		if(!ECHO)
			scip.setIntParam("display/verblevel", 0);

		scip.setPresolving(presolve_emph, !(ECHO_SETTINGS && ECHO));
		scip.setEmphasis(solve_emph, !(ECHO_SETTINGS && ECHO));

		scip.setBoolParam("misc/improvingsols", true);
		
		//Allocate variables
		vars = new SCIP_VAR[N];
		for(int i=0; i<N; i++) {
			//Each variable has objective weight of 1.0
			vars[i] = scip.createVarBasic("v"+i, 0, 1, 1.0, BINARY);
			scip.addVar(vars[i]);
		}
		
		ArrayList<int[]> cycleList = findTriangles(g);
		for(int[] cycle : cycleList) {
			if(INIT_USE_LINEAR)
				addLinearCons(scip, cycle);
			if(INIT_USE_SETPPC)
				addSetppcCons(scip, cycle);
			if(INIT_USE_LOGICOR)
				addLogicorCons(scip, cycle);
		}

		AcycConshdlr conshdlr = new AcycConshdlr(scip);

		SCIP_DECL_HEUREXEC heurexec =  this::heurexec;
		scip_heur = scip.includeHeurBasic("javaheur", "java-side heuristic", (byte)'j',
				HEUR_PRIORITY, HEUR_FREQ, HEUR_FREQOFS, HEUR_MAXDEPTH, HEUR_TIMING,
				false, heurexec, null);
		
		scip.presolve();
		scip.solve();
		
		SCIP_STATUS scip_status = scip.getStatus();
		System.out.println("Final status "+scip_status);
		
		//Save the variables
		SCIP_SOL sol = scip.getBestSol();
		ArrayList<Integer> res = extractSol(scip, sol);
		
		System.out.println(" -- Conshdlr times -- ");
		System.out.println("Prop time:   "+JSCIP.conshdlrGetPropTime(conshdlr.conshdlr));
		System.out.println("Check time:  "+JSCIP.conshdlrGetCheckTime(conshdlr.conshdlr));
		System.out.println("EnfoLP time: "+JSCIP.conshdlrGetEnfoLPTime(conshdlr.conshdlr));
		System.out.println("EnfoPS time: "+JSCIP.conshdlrGetEnfoPSTime(conshdlr.conshdlr));
		System.out.println(" -- Heuristic times --");
		System.out.println("Setup time:  "+JSCIP.heurGetSetupTime(scip_heur));
		System.out.println("Run time:    "+JSCIP.heurGetTime(scip_heur));

		
		//SCIP cleanup
		for(int i=0; i<N; i++) {
			scip.releaseVar(vars[i]);
		}
		scip.free();
		
		return res;
	}
	
	ArrayList<Integer> extractSol(SCIP scip, SCIP_SOL sol){
		ArrayList<Integer> res = new ArrayList<>();
		res.clear();		
		for(int i=0; i<N; i++) {
			double val = scip.getSolVal(sol, vars[i]);
			if(val > 0.5) {
				res.add(i);
			}
		}
		return res;
	}

	//Store no constraint data
	class VoidData extends ConstraintData<VoidData,AcycConshdlr> {
		public VoidData copy() {return new VoidData();}
		public void delete() {}
		public VoidData transform(SCIP scip) {return new VoidData();}
		public void delete(SCIP scip) {}
		public void exit(SCIP scip) {}
	}
	
	class AcycConshdlr extends ConstraintHandler<VoidData,AcycConshdlr> {

		public AcycConshdlr(SCIP scip) {
			//"false" in the last argument to trigger constraint checking even with no associated constraints
			super(scip, VoidData.class, "AcycConshdlr", "acyclicity handler",
					CONSHDLR_ENFOPRIORITY, CONSHDLR_CHCKPRIORITY, 0, false);
			//Eagerfreq = 0 because we don't use actual constraints
		}

		public SCIP_RESULT addCycles(SCIP scip, SCIP_SOL sol, ArrayList<int[]> cycleList) {
			boolean is_infeasible = false;
			
			System.out.println("Adding "+cycleList.size()+" cycles");
			for(int[] cycle : cycleList) {
				
				if(CALLBACK_ROW) {
					SCIP_ROW row = scip.createEmptyRowConshdlr(this.conshdlr, "cycRow"+cycle.length, 1, inf, false, false, true);
					scip.cacheRowExtensions(row);
					for(int v : cycle) {
						scip.addVarToRow(row, vars[v], 1);
					}
					scip.flushRowExtensions(row);
					
					is_infeasible |= scip.addRow(row, false);
					System.out.println(" is_infeas: "+is_infeasible);
					System.out.println(" is_cut_effic: "+scip.isCutEfficacious(sol, row));
					scip.releaseRow(row);
				}
				if(CALLBACK_GLOBALCUT) {
					SCIP_ROW row = scip.createEmptyRowConshdlr(this.conshdlr, "cycRow"+cycle.length, 1, inf, false, false, true);
					scip.cacheRowExtensions(row);
					for(int v : cycle) {
						scip.addVarToRow(row, vars[v], 1);
					}
					scip.flushRowExtensions(row);
					scip.addPoolCut(row);
					System.out.println(" is_cut_effic: "+scip.isCutEfficacious(sol, row));
					scip.releaseRow(row);
				}
				if(CALLBACK_LINCONS)
					addLinearCons(scip, cycle);
				if(CALLBACK_SETPPCCONS)
					addSetppcCons(scip, cycle);
				if(CALLBACK_LOGICORCONS)
					addLogicorCons(scip, cycle);
			}

			if(is_infeasible) {
				return SCIP_RESULT.SCIP_CUTOFF;
			} else {
				return SCIP_RESULT.SCIP_SEPARATED;
			}
		}

		@Override
		public SCIP_RESULT conscheck(SCIP scip, VoidData[] conss, SCIP_SOL sol, boolean checkintegrality,
				boolean checklprows, boolean printreason, boolean completely) {
			if(conss.length > 0)
				throw new RuntimeException(); //where did you get a constraint from?
			
			ArrayList<Integer> trySol = extractSol(scip, sol);
			ArrayList<int[]> cycleList = null;
			boolean acyc;
			
			if(CONSCHECK_ADDCONS) {
				cycleList = digForCycles(trySol, GenCyclesMode.CHECK_ACYC);
				acyc = cycleList.size()==0;
			} else {
				acyc = !hasCycle(trySol);
			}

			System.out.println("CONSCHECK with n="+trySol.size()+", acyc?="+acyc+" Opts:"+(checkintegrality?'T':'F')+(checklprows?'T':'F')+(printreason?'T':'F')+(completely?'T':'F'));
			
			if(acyc)
				return SCIP_FEASIBLE;
			else {
				if(CONSCHECK_ADDCONS) {
					addCycles(scip, null, cycleList);
				}
				return SCIP_INFEASIBLE;
			}
		}

		@Override
		public SCIP_RETCODE conslock(SCIP scip, VoidData cons, SCIP_LOCKTYPE locktype, int nlockspos,
				int nlocksneg) {
			if(cons != null)
				return SCIP_ERROR; //where did you get a constraint from, what?
			
			//Lock everything from below
			for(SCIP_VAR var : vars)
				scip.addVarLocksType(var, MODEL, nlockspos, nlocksneg);
			
			return SCIP_OKAY;
		}

		@Override
		public SCIP_RESULT consenfops(SCIP scip, VoidData[] conss, int nusefulconss, boolean solinfeasible,
				boolean objinfeasible) {
			System.out.println("CONSENFOPS?");
			if(conss.length > 0)
				throw new RuntimeException(); //where did you get a constraint from, what?

			return consenfolp(scip, conss, nusefulconss, solinfeasible);
		}

		@Override
		public SCIP_RESULT consenfolp(SCIP scip, VoidData[] conss, int nusefulconss, boolean solinfeasible) {
			if(conss.length > 0)
				throw new RuntimeException(); //where did you get a constraint from, what?

			ArrayList<Integer> trySol = extractSol(scip, null);
			ArrayList<int[]> cycleList = digForCycles(trySol, GenCyclesMode.EDGE_DFS_GENEROUS);
			System.out.println("CONSENFOLP with n="+trySol.size()+", cyc="+cycleList.size());
			
			if(cycleList.size() == 0)
				return SCIP_FEASIBLE;
			else {
				return addCycles(scip, null, cycleList);
			}
		}

		@Override
		public SCIP_RETCODE consdelete(SCIP scip, VoidData cons) {
			throw new RuntimeException("Consdelete on .. what? "+cons);//this should never be called
		}
	}
	
	//Adds in all the triangles first (and pairs, in case those are in there)
	ArrayList<int[]> findTriangles(Graph g) {
		ArrayList<int[]> cycleList = new ArrayList<int[]>();
		int pairsAdded = 0;
		int trisAdded = 0;
		
		long startT = System.currentTimeMillis();
		
		for(int i=0; i<g.N; i++) {
			for(int j : g.eList[i]) {
				if(j < i)//skip
					continue;
				
				//check for a pair
				if(g.eList[j].contains(i)) {
					cycleList.add(new int[]{i,j});
					pairsAdded++;
					continue;//if we got a pair don't need to worry about triangles.
				}
				
				//check for a triangle
				for(int k : g.eList[j]) {
					if(k < i)//skip
						continue;
					
					if(g.eList[k].contains(i)) {
						cycleList.add(new int[]{i,j,k});
						trisAdded++;
					}
				}
			}
		}
		
		long time = System.currentTimeMillis() - startT;
		if(Main_Load.TESTING) System.out.println("FindTri took "+(time*0.001)+"sec, found "+pairsAdded+" K2 and "+trisAdded+" K3");
		
		return cycleList;
	}

	public SCIP_RETCODE heurexec(SCIP scip, SCIP_HEUR heur, SCIP_HEURTIMING heurtiming, boolean nodeinfeasible,
			IntByReference scip_result) {
		System.out.println("HEUREXEC CALLED");
		ReducedGraph heurSolGraph = ReducedGraph.fromGraph(g);
		
		//Read out the current fixings, construct effective graph
		int trueCount = 0, falseCount = 0;
		for(int i=0; i<N; i++) {
			SCIP_VAR var = vars[i];
			double lbL = JSCIP.varGetLbLocal(var);
			double ubL = JSCIP.varGetUbLocal(var);
			boolean fixedTrue = lbL == 1.0;
			boolean fixedFalse = ubL == 0.0;
			if(fixedTrue) {
//				System.out.println("Selfloop on "+i);
				trueCount++;
				heurSolGraph.addEdge(i, i);
			} else if(fixedFalse) {
//				System.out.println("Contract in "+i);
				falseCount++;
				heurSolGraph.contractIn(i);
			}
//			System.out.println("Var "+JSCIP.varGetName(var)+" in ["+lbL+","+ubL+"]. fT="+fixedTrue+", fF="+fixedFalse);
		}
		
		//Solve with a Java-side heuristic 
		ArrayList<Integer> heurSol = Heuristic_DVFS_Fast.solve(heurSolGraph);
		System.out.println("Got a solution with N="+heurSol.size()+", tC="+trueCount+", fC="+falseCount);
//		System.out.println(heurSol);
		
		//Submit solution back to SCIP
		SCIP_SOL scip_sol = scip.createSol(scip_heur);
		for(int i=0; i<N; i++) {
			scip.setSolVal(scip_sol, vars[i], 0.0);
		}
		for(int i : heurSol) {
			scip.setSolVal(scip_sol, vars[i], 1.0);
		}
		
		boolean stored = scip.trySol(scip_sol, true, false, true, false, true);
		System.out.println("Was solsution stored? "+stored);
		
		scip.freeSol(scip_sol);
		
		scip_result.setValue(SCIP_RESULT.SCIP_FOUNDSOL.ordinal());
		return SCIP_RETCODE.SCIP_OKAY;
	}

	//Given a graph and SCIP's output 'solution', check for any cycles. If at least one is found,
	//put it in cycleList and return "false". Otherwise, return true;
	//
	//mode specifies the method for generating edges. *_DFS does a DFS to find one cycle.
	// CHECK_ACYC just tries to find a single cycle.
	// VERTEX_DFS removes all used vertices after, and repeats.
	// VERTEX_DFS_GENEROUS removes only one used vertex.
	// EDGE_DFS removes all but one used edges.
	// EDGE_DFS_GENEROUS removes only one used edge.
	//EDGE_DFS will obviously generate (typically) more cycles.
	enum GenCyclesMode {
		CHECK_ACYC,
		VERTEX_DFS,
		VERTEX_DFS_GENEROUS,
		EDGE_DFS,
		EDGE_DFS_GENEROUS,
	};
	
	boolean hasCycle(ArrayList<Integer> trySol) {
		Graph g = this.g.copy();
		for(int i : trySol) {
			g.clearVertex(i);
		}
		return g.hasCycle();
	}

	ArrayList<int[]> digForCycles(ArrayList<Integer> trySol, GenCyclesMode mode) {
		ArrayList<int[]> cycleList = new ArrayList<int[]>();
		
		//Here's one way to get some cycles.
		//Copy it, remove all the vertices in trySol, then do a DFS to get a cycle.
		Graph g = this.g.copy();
		for(int i : trySol) {
			g.clearVertex(i);
		}
		
		int cyclesAdded = 0;
		
		ArrayDeque<Integer> cycQ;
		while((cycQ = g.findCycleDFS(null)) != null) {
			//put it in the cycle list
			int[] cyc = cycQ.stream().mapToInt(i -> i).toArray();
			cycleList.add(cyc);
			cyclesAdded++;
			//remove that cycle from the graph, hope we get another one.
			if(mode == GenCyclesMode.CHECK_ACYC) {
				break;
			} else if(mode == GenCyclesMode.VERTEX_DFS) {
				for(int i : cyc)
					g.clearVertex(i);
			} else if(mode == GenCyclesMode.VERTEX_DFS_GENEROUS) {
				g.clearVertex(cyc[0]);
			} else if(mode == GenCyclesMode.EDGE_DFS) {
				for(int ii=0; ii<cyc.length - 1; ii++)
					g.clearEdge(cyc[ii], cyc[ii+1]);
			} else if(mode == GenCyclesMode.EDGE_DFS_GENEROUS) {
				g.clearEdge(cyc[0], cyc[1]);
			} 
		}
		
		if(Main_Load.TESTING) System.out.println("Dig gave "+cyclesAdded+" new cycles.");
		return cycleList;
	}
	
	void addLinearCons(SCIP scip, int[] cycle) {
		SCIP_CONS cons = scip.createConsBasicLinear("linCyc"+cycle.length, null, null, 1, inf);
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
