import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import JNA_SCIP.*;

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

	static final boolean ECHO = true && Main_Load.TESTING;//print progress + versioning to STDOUT
	static final boolean ECHO_SETTINGS = false;//print configurations to STDOUT
	
	//How strong to presolve?
	static final SCIP_PARAMSETTING presolve_emph = AGGRESSIVE;
	static final SCIP_PARAMEMPHASIS solve_emph = OPTIMALITY;

	GenCyclesMode mode = GenCyclesMode.EDGE_DFS_GENEROUS;
	
	int time_limit = 3000;
	int node_limit = 1000;
	
	static final int extra_random_init = 50;
	static final int extra_random_loop = 50;

	public ArrayList<Integer> solve(MinimumCoverInfo mci) {
		
		if(Main_Load.TESTING) {
			System.out.println("Solving with ILP_CoverAndChunks_Reopt");
		}
		
		this.N = mci.N;
		
		cycleList = new ArrayList<int[]>();
		cycleList.addAll(mci.pairList);
		cycleList.addAll(mci.bigCycleList);
		dig_rand = new Random(1001);

		//add starting cycles for the chunks
		for(GraphChunk ch : mci.chunks) {
			digForCycles(ch, new ArrayList<>(), mode);
			for(int i=0; i<extra_random_init; i++) {
				digForCycles(ch, new ArrayList<>(), GenCyclesMode.RANDOM_EXTRA);
			}
		}
		
		cycle_i = 0;

		//SCIP initialization
		SCIP scip = SCIP.create();
		if(ECHO)
			scip.printVersion(null);
		scip.includeDefaultPlugins();
		scip.createProbBasic("prob");
		if(!ECHO)
			scip.setIntParam("display/verblevel", 0);

		scip.setPresolving(presolve_emph, !(ECHO_SETTINGS && ECHO));
		scip.setEmphasis(solve_emph, !(ECHO_SETTINGS && ECHO));
		
//		JSCIP.enableReoptimization(true);
//		JSCIP.setBoolParam("reoptimization/enable", true);

		//Allocate variables
		vars = new SCIP_VAR[N];
		for(int i=0; i<N; i++) {
			//Each variable has objective weight of 1.0
			vars[i] = scip.createVarBasic("v"+i, 0, 1, 1.0, BINARY);
			scip.addVar(vars[i]);
		}
		
		//first get a few cycle using the base graph (no tentative solution).
		
		ArrayList<Integer> trySol = new ArrayList<Integer>();
		boolean isAcyc = false;
		
		if(Main_Load.TESTING) System.out.println("Operating in mode "+mode);
		
		while(!isAcyc) {
			SCIP_STATUS scip_status = getSCIPSolution(scip, trySol);
			isAcyc = true;
			
			for(GraphChunk ch : mci.chunks) {
				isAcyc = digForCycles(ch, trySol, mode) & isAcyc;
				for(int i=0; i<extra_random_loop; i++) {
					digForCycles(ch, trySol, GenCyclesMode.RANDOM_EXTRA);
				}
			}
			
			if(isAcyc) {
				if(scip_status == OPTIMAL) {
					if(Main_Load.TESTING) System.out.println("Exact answer found");
					break;
				} else if(scip_status == NODELIMIT) {
					node_limit *= 10;
					if(Main_Load.TESTING) System.out.println("Up node_limit to "+node_limit);
					isAcyc = false;
					continue;
				} else if(scip_status == TIMELIMIT) {
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
			} else {
				//experiment: try checking ALL the solutions!
//				SCIP_SOL[] solList = JSCIP.getSols();
			}
		}

		//SCIP cleanup
		for(int i=0; i<N; i++) {
			scip.releaseVar(vars[i]);
		}
		scip.free();
		
		return trySol;
	}

	SCIP_STATUS getSCIPSolution(SCIP scip, ArrayList<Integer> res) {
		double inf = scip.infinity();
		
		//Constraints
		//Each cycle has at least one variable true
		for( ; cycle_i < cycleList.size(); cycle_i++ ) {
			int[] arr = cycleList.get(cycle_i);
			SCIP_CONS cons = scip.createConsBasicLinear("cons"+cycle_i, null, null, 1, inf);
			
			for(int v : arr) {
				scip.addCoefLinear(cons, vars[v], 1.0);
			}
			scip.addCons(cons);
			scip.releaseCons(cons);
		}
		
//		scip.setLongintParam("limits/nodes", node_limit);
//		scip.setRealParam("limits/time", time_limit);
//		scip.setIntParam("reoptimization/maxsavednodes", node_limit);
//		scip.setRealParam("reoptimization/delay", time_limit);
		
//		scip.presolve();
		scip.solve();
		
		SCIP_SOL sol = scip.getBestSol();
		
		SCIP_STATUS scip_status = scip.getStatus();
			
		//Save the variables
		res.clear();
		for(int i=0; i<N; i++) {
			double val = scip.getSolVal(sol, vars[i]);
			if(val > 0.5) {
				res.add(i);
			}
		}

		if(Main_Load.TESTING)
			System.out.println("SCIP solved with "+cycleList.size()+" cycles and gave "+res.size()+" vertices. "+scip_status);
	
		if(Main_Load.TESTING)
			System.out.println("Stage = "+scip.getStage());
//		scip.freeReoptSolve();
//		scip.freeSolve();
		scip.freeTransform();
		if(Main_Load.TESTING)
			System.out.println("Stage = "+scip.getStage());
		
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
		RANDOM_EXTRA,
	};
	Random dig_rand;
	
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
		Random rand = (mode == GenCyclesMode.RANDOM_EXTRA) ? dig_rand : null;
		
		ArrayDeque<Integer> cycQ;
		while((cycQ = g.findCycleDFS(rand)) != null) {
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
			
			if(mode == GenCyclesMode.RANDOM_EXTRA) {
				int cycLen = cyc.length;
				if(cyc.length > 2) {
					int randI = dig_rand.nextInt(cycLen-2);
					for(int p=0; p<randI; p++) {
						cycQ.removeFirst();
					}
					for(int p=0; p<cycLen-2-randI; p++) {
						cycQ.removeLast();
					}
				}
				g.clearEdge(cycQ.peekFirst(), cycQ.peekLast());
			}
		}
		
		if(Main_Load.TESTING) System.out.println("Dig gave "+cyclesAdded+" new cycles.");
		return cyclesAdded == 0;
	}
}
