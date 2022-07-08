import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;

import JNA_SCIP.*;

import static JNA_SCIP.SCIP_PARAMSETTING.*;
import static JNA_SCIP.SCIP_PARAMEMPHASIS.*;
import static JNA_SCIP.SCIP_STATUS.*;
import static JNA_SCIP.SCIP_VARTYPE.*;

//Solves the DVFS problem using the ILP formulation. Since there can be exponentially
//many cycles, this implements adds some initial cycles (K2s and C3s) and then solves it.
//It then looks for remaining cycles in the found solution, adds those constraints,
//and then solves the improved problem. This is "less smart" than in-the-loop constraint
//enforcement, but the solver has a fixed set of cycles on each run, which allows
//the presolver to optimize much more aggressively and reach the optimum faster.
//
//This can run either creating a new problem each time, or using SCIP's 
//"reoptimization" feature. Unfortunately, the reoptimization seems to be currently
//broken (v8.0.0) when adding new constraints, as it was made mostly for purposes
//where the objective, and not the valid region, is changed repeatedly.

public class ILP_DVFS_Reopt {

	ArrayList<int[]> cycleList;
	int N;
	int cycle_i;
	SCIP_VAR[] vars;

	static final boolean ECHO = false;//print progress + versioning to STDOUT
	static final boolean ECHO_SETTINGS = true;//print configurations to STDOUT
	
	//How strong to presolve?
	static final SCIP_PARAMSETTING presolve_emph = AGGRESSIVE;
	static final SCIP_PARAMEMPHASIS solve_emph = OPTIMALITY;
	
	int time_limit = 3000;
	int node_limit = 1000;

	public ArrayList<Integer> solve(Graph g) {
		if(Main_Load.TESTING)
			System.out.println("Solving with ILP_DVFS_Reopt");
		cycleList = new ArrayList<int[]>();
		N = g.N;
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
		findTriangles(g);
		cleanUpGraph(g);
		findC4s(g);
		
		GenCyclesMode mode = GenCyclesMode.EDGE_DFS_GENEROUS;
		if(Main_Load.TESTING) System.out.println("Operating in mode "+mode);
		
		while(!isAcyc) {
			SCIP_STATUS scip_status = getSCIPSolution(scip, trySol);
			isAcyc = digForCycles(g, trySol, mode);
			
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
		scip.setIntParam("reoptimization/maxsavednodes", node_limit);
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
	
	//Adds in all the triangles first (and pairs, in case those are in there)
	void findTriangles(Graph g) {
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
		if(Main_Load.TESTING)
			System.out.println("FindTri took "+(time*0.001)+"sec, found "+pairsAdded+" K2 and "+trisAdded+" K3");
	}
	
	//After all K2s have been added to the cycleList enforcement, we can "safely" delete those from the graph --
	// future processing doesn't need to worry about checking them, as at least one is gone. That is to say, all
	// constraints involving those edges are fully represented in the appropriate rows.
	void cleanUpGraph(Graph g) {
		int edgesRemoved = 0;
		for(int[] cycle : cycleList) {
			if(cycle.length > 2)
				continue;
			if(cycle.length == 1)
				throw new RuntimeException("Should've been removed in pruning?");
			int i = cycle[0], j = cycle[1];
			g.eList[i].remove(j);
			g.backEList[i].remove(j);
			g.inDeg[i]--;
			g.outDeg[i]--;
			g.eList[j].remove(i);
			g.backEList[j].remove(i);
			g.inDeg[j]--;
			g.outDeg[j]--;
			edgesRemoved+=2;
		}
		if(Main_Load.TESTING)
			System.out.println("Minimized graph, now has "+edgesRemoved+" less edges. E="+g.E());
	}
	
	void findC4s(Graph g) {
		int c4Added = 0;
		
		long startT = System.currentTimeMillis();
		
		for(int i=0; i<g.N; i++) {
			HashSet<Integer> iList =  g.eList[i];
			for(int j : iList) {
				if(j < i)//skip
					continue;

				HashSet<Integer> jList =  g.eList[j];
				
				//check for a pair
				if(jList.contains(i)) {
					continue;//have pair, don't need to worry
				}
				
				//check for a triangle
				for(int k : jList) {
					if(k <= i)//skip
						continue;
					
					HashSet<Integer> kList =  g.eList[k];
					if(kList.contains(j) || kList.contains(i)) {
						continue;//have triangle or pair, don't need to worry
					}
					
					for(int l : g.eList[k]) {
						if(l <= i || l==j)//skip
							continue;

						HashSet<Integer> lList =  g.eList[l];
						if(lList.contains(i)) {
							//add! unless...
							if(lList.contains(j) || lList.contains(k) || iList.contains(l) || jList.contains(l))
								continue;//have chord, don't need to worry
							cycleList.add(new int[]{i,j,k,l});
						}
					}
				}
			}
		}
		
		long time = System.currentTimeMillis() - startT;
		if(Main_Load.TESTING) System.out.println("FindC4s took "+(time*0.001)+"sec, found "+c4Added+" C4");
	
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
	
	boolean digForCycles(Graph g_orig, ArrayList<Integer> trySol, GenCyclesMode mode) {
		if(mode == null)
			throw new RuntimeException("Must supply a mode");
		
		//Here's one way to get some cycles.
		//Copy it, remove all the vertices in trySol, then do a DFS to get a cycle.
		Graph g = g_orig.copy();
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
			if(mode == GenCyclesMode.VERTEX_DFS) {
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
		return cyclesAdded == 0;
	}
}
