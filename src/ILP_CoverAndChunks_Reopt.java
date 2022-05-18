import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;

import JNA_SCIP.JSCIP;
import JNA_SCIP.SCIP_CONS;
import JNA_SCIP.SCIP_PARAMEMPHASIS;
import JNA_SCIP.SCIP_PARAMSETTING;
import JNA_SCIP.SCIP_SOL;
import JNA_SCIP.SCIP_STATUS;
import JNA_SCIP.SCIP_VAR;

import static JNA_SCIP.SCIP_PARAMSETTING.*;
import static JNA_SCIP.SCIP_PARAMEMPHASIS.*;
import static JNA_SCIP.SCIP_STATUS.*;
import static JNA_SCIP.SCIP_VARTYPE.*;

//Like ILP_CoverAndChunks_Conshdlr, but instead of doing an in-the-loop conshdlr,
//time, does it in an out-of-the-loop cycle adder.
public class ILP_CoverAndChunks_Reopt {

	ArrayList<int[]> cycleList;
	int N;
	int cycle_i;
	SCIP_VAR[] vars;

	static final boolean ECHO = false;//print progress + versioning to STDOUT
	static final boolean ECHO_SETTINGS = false;//print configurations to STDOUT
	
	//How strong to presolve?
	static final SCIP_PARAMSETTING presolve_emph = SCIP_PARAMSETTING_AGGRESSIVE;
	static final SCIP_PARAMEMPHASIS solve_emph = SCIP_PARAMEMPHASIS_OPTIMALITY;

	GenCyclesMode mode = GenCyclesMode.EDGE_DFS_GENEROUS;
	
	int time_limit = 3000;
	int node_limit = 1000;

	public ArrayList<Integer> solve(MinimumCoverInfo mci) {
		
		if(Main_Load.TESTING) {
			System.out.println("Solving with ILP_CoverAndChunks_Reopt");
//			for(int[] pair : mci.pairList)
//				System.out.println(Arrays.toString(pair));
//			for(int[] cyc : mci.bigCycleList)
//				System.out.println(Arrays.toString(cyc));
//			for(GraphChunk chunk : mci.chunks) {
//				System.out.println(chunk.gInner.dumpS());
//				System.out.println(" mapped with "+Arrays.toString(chunk.mapping));
//			}
		}
		
		this.N = mci.N;
		
		cycleList = new ArrayList<int[]>();
		cycleList.addAll(mci.pairList);
		cycleList.addAll(mci.bigCycleList);

		//add starting cycles for the chunks
		for(GraphChunk ch : mci.chunks) {
			digForCycles(ch, new ArrayList<>(), mode);
		}
		
		cycle_i = 0;

		//SCIP initialization
		JSCIP.create();
		if(ECHO)
			JSCIP.printVersion(null);
		JSCIP.includeDefaultPlugins();
		JSCIP.createProbBasic("prob");
		if(!ECHO)
			JSCIP.setIntParam("display/verblevel", 0);

		JSCIP.setPresolving(presolve_emph, !(ECHO_SETTINGS && ECHO));
		JSCIP.setEmphasis(solve_emph, !(ECHO_SETTINGS && ECHO));
		
//		JSCIP.enableReoptimization(true);
//		JSCIP.setBoolParam("reoptimization/enable", true);

		//Allocate variables
		vars = new SCIP_VAR[N];
		for(int i=0; i<N; i++) {
			//Each variable has objective weight of 1.0
			vars[i] = JSCIP.createVarBasic("v"+i, 0, 1, 1.0, SCIP_VARTYPE_BINARY);
			JSCIP.addVar(vars[i]);
		}
		
		//first get a few cycle using the base graph (no tentative solution).
		
		ArrayList<Integer> trySol = new ArrayList<Integer>();
		boolean isAcyc = false;
		
		if(Main_Load.TESTING) System.out.println("Operating in mode "+mode);
		
		while(!isAcyc) {
			SCIP_STATUS scip_status = getSCIPSolution(trySol);
			isAcyc = true;
			
			for(GraphChunk ch : mci.chunks) {
				isAcyc = digForCycles(ch, trySol, mode) & isAcyc;
			}
			
			if(isAcyc) {
				if(scip_status == SCIP_STATUS_OPTIMAL) {
					if(Main_Load.TESTING) System.out.println("Exact answer found");
					break;
				} else if(scip_status == SCIP_STATUS_NODELIMIT) {
					node_limit *= 10;
					if(Main_Load.TESTING) System.out.println("Up node_limit to "+node_limit);
					isAcyc = false;
					continue;
				} else if(scip_status == SCIP_STATUS_TIMELIMIT) {
					time_limit = 100 + time_limit;
					if(Main_Load.TESTING) System.out.println("Up time_limit to "+time_limit);
					if(time_limit > 3000) {
						if(Main_Load.TESTING) System.out.println("FAILED TO FIND SOLUTION IN TIME LIMIT");
						trySol = null;
						break;
					}
					isAcyc = false;
					continue;
				}
			}
		}

		//SCIP cleanup
		for(int i=0; i<N; i++) {
			JSCIP.releaseVar(vars[i]);
		}
		JSCIP.free();
		
		return trySol;
	}

	SCIP_STATUS getSCIPSolution(ArrayList<Integer> res) {
		double inf = JSCIP.infinity();
		
		//Constraints
		//Each cycle has at least one variable true
		for( ; cycle_i < cycleList.size(); cycle_i++ ) {
			int[] arr = cycleList.get(cycle_i);
			SCIP_CONS cons = JSCIP.createConsBasicLinear("cons"+cycle_i, null, null, 1, inf);
			
			for(int v : arr) {
				JSCIP.addCoefLinear(cons, vars[v], 1.0);
			}
			JSCIP.addCons(cons);
			JSCIP.releaseCons(cons);
		}
		
//		JSCIP.setLongintParam("limits/nodes", node_limit);
//		JSCIP.setRealParam("limits/time", time_limit);
		JSCIP.setIntParam("reoptimization/maxsavednodes", node_limit);
//		JSCIP.setRealParam("reoptimization/delay", time_limit);
		
//		JSCIP.presolve();
		JSCIP.solve();
		
		SCIP_SOL sol = JSCIP.getBestSol();
		
		SCIP_STATUS scip_status = JSCIP.getStatus();
			
		//Save the variables
		res.clear();
		for(int i=0; i<N; i++) {
			double val = JSCIP.getSolVal(sol, vars[i]);
			if(val > 0.5) {
				res.add(i);
			}
		}

		if(Main_Load.TESTING)
			System.out.println("SCIP solved with "+cycleList.size()+" cycles and gave "+res.size()+" vertices. "+scip_status);
	
		if(Main_Load.TESTING)
			System.out.println("Stage = "+JSCIP.getStage());
//		JSCIP.freeReoptSolve();
//		JSCIP.freeSolve();
		JSCIP.freeTransform();
		if(Main_Load.TESTING)
			System.out.println("Stage = "+JSCIP.getStage());
		
		return scip_status;
	}
	
	//Given a graph and SCIP's output 'solution', check for any cycles. If at least one is found,
	//put it in cycleList and return "false". Otherwise, return true;
	//
	//mode specifies the method for generating edges. *_DFS does a DFS to find one cycle.
	// VERTEX_DFS removes all used vertices after, and repeats.
	// VERTEX_DFS_GENEROUS removes only one used vertex.
	// EDGE_DFS removes all but one used edges.
	// EDGE_DFS_GENEROUS removes only one used edge.
	//EDGE_DFS will obviously generate (typically) more cycles.
	enum GenCyclesMode {
		VERTEX_DFS,
		VERTEX_DFS_GENEROUS,
		EDGE_DFS,
		EDGE_DFS_GENEROUS,
	};
	
	//returns true if no cycles were found
	boolean digForCycles(GraphChunk chunk, ArrayList<Integer> trySolList, GenCyclesMode mode) {
		if(mode == null)
			throw new RuntimeException("Must supply a mode");
		
		//Here's one way to get some cycles.
		//Copy it, remove all the vertices in trySol, then do a DFS to get a cycle.
		Graph g = chunk.gInner.copy();
		HashSet<Integer> trySol = new HashSet<>(trySolList);
		for(int i0=0; i0<g.N; i0++) {
			int i = chunk.mapping[i0];
			if(trySol.contains(i))
				g.clearVertex(i0);
		}
		
		int cyclesAdded = 0;
		
		ArrayDeque<Integer> cycQ;
		while((cycQ = g.findCycleDFS()) != null) {
			//remove that cycle from the graph, hope we get another one.
			if(mode == GenCyclesMode.VERTEX_DFS) {
				for(int i : cycQ)
					g.clearVertex(i);
			} else if(mode == GenCyclesMode.VERTEX_DFS_GENEROUS) {
				g.clearVertex(cycQ.peekFirst());
			} else if(mode == GenCyclesMode.EDGE_DFS_GENEROUS) {
				g.clearEdge(cycQ.peekLast(), cycQ.peekFirst());
			} 
			
			//put it in the cycle list
			int[] cyc = cycQ.stream().mapToInt(i -> chunk.mapping[i]).toArray();
			cycleList.add(cyc);
			cyclesAdded++;
		}
		
		if(Main_Load.TESTING) System.out.println("Dig gave "+cyclesAdded+" new cycles.");
		return cyclesAdded == 0;
	}
}
