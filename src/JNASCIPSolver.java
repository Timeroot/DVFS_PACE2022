import java.util.ArrayDeque;
import java.util.ArrayList;

import JNA_SCIP.JSCIP;
import JNA_SCIP.SCIP_CONS;
import JNA_SCIP.SCIP_SOL;
import JNA_SCIP.SCIP_VAR;
import JNA_SCIP.JSCIP.SCIP_STATUS;
import static JNA_SCIP.JSCIP.SCIP_VARTYPE.*;
import static JNA_SCIP.JSCIP.SCIP_PARAMSETTING.*;
import static JNA_SCIP.JSCIP.SCIP_STATUS.*;

public class JNASCIPSolver implements Solver {

	ArrayList<int[]> cycleList = new ArrayList<int[]>();
	int N;
	
	static final boolean ECHO = false;
	
	public JNASCIPSolver() {
	}

	int time_limit = 3;
	int node_limit = 10;
	@Override
	public ArrayList<Integer> solve(Graph g) {
		N = g.N;
		
		//first get a cycle using the base graph (no tentative solution).
		
		ArrayList<Integer> trySol = new ArrayList<Integer>();
//		boolean isAcyc = digForCycles(g, trySol, GenCyclesMode.EDGE_DFS_GENEROUS);
		
		boolean isAcyc = false;
		findTriangles(g);
		
		GenCyclesMode mode = GenCyclesMode.EDGE_DFS_GENEROUS;
		if(Main_Load.TESTING) System.out.println("Operating in mode "+mode);
		
		
		while(!isAcyc) {
			SCIP_STATUS scip_status = getSCIPSolution(trySol);
			isAcyc = digForCycles(g, trySol, mode);
			
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
						return null;
					}
					isAcyc = false;
					continue;
				}
			}
		}
		return trySol;
	}

	@Override
	public ArrayList<Integer> solve(ReducedGraph g) {
		// TODO Auto-generated method stub
		return null;
	}
	
	SCIP_STATUS getSCIPSolution(ArrayList<Integer> res) {
		JSCIP.create();
		JSCIP.includeDefaultPlugins();
		JSCIP.createProbBasic("prob");
		JSCIP.setIntParam("display/verblevel", 0);
		
		double inf = JSCIP.infinity();
		
		//Create variables
		SCIP_VAR[] vars = new SCIP_VAR[N];
		for(int i=0; i<N; i++) {
			//Each variable has objective weight of 1.0
			vars[i] = JSCIP.createVarBasic("v"+i, 0, 1, 1.0, SCIP_VARTYPE_BINARY);
			JSCIP.addVar(vars[i]);
		}

		//Constraints
		//Each cycle has at least one variable true
		int cons_i = 0;
		for(int[] arr : cycleList) {
			SCIP_CONS cons = JSCIP.createConsBasicLinear("cons"+cons_i, 0, null, null, 1, inf);
			cons_i ++;
			
			for(int v : arr) {
				JSCIP.addCoefLinear(cons, vars[v], 1.0);
			}
			JSCIP.addCons(cons);
			JSCIP.releaseCons(cons);
		}

		JSCIP.setPresolving(SCIP_PARAMSETTING_AGGRESSIVE, true);
//		JSCIP.setEmphasis(emph, quiet);
		
		JSCIP.setLongintParam("limits/nodes", node_limit);
		JSCIP.setRealParam("limits/time", time_limit);
		
		JSCIP.presolve();
		JSCIP.solve();
		
		SCIP_SOL sol = JSCIP.getBestSol();
		
		SCIP_STATUS scip_status = null;
		
//			if(line1.contentEquals("solution status: infeasible")) {
//				throw new RuntimeException("SCIP Reported infeasible problem in "+lpName);
//			} else if(line1.contentEquals("solution status: optimal solution found")) {
//				scip_status = SCIP_STATUS.OPTIMAL;
//			} else if(line1.contentEquals("solution status: node limit reached")) {
//				scip_status = SCIP_STATUS.NODE_LIMIT;
//			} else  if(line1.contentEquals("solution status: time limit reached")) {
//				scip_status = SCIP_STATUS.TIME_LIMIT;
//			} else {
//				if(Main_Load.TESTING) System.out.println("SCIP gave "+line1);
//				throw new RuntimeException("SCIP gave "+line1);
//			}
//			scipSolReader.readLine();
			
		//Save the variables
		scip_status = JSCIP.getStatus();
		res.clear();
		
		for(int i=0; i<N; i++) {
			double val = JSCIP.getSolVal(sol, vars[i]);
			if(val > 0) {
				res.add(i);
			}
		}

		for(int i=0; i<N; i++) {
			JSCIP.releaseVar(vars[i]);
		}
		
		JSCIP.free();

		if(Main_Load.TESTING) System.out.println("SCIP solved with "+cycleList.size()+" cycles and gave "+res.size()+" vertices. "+scip_status);

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
		if(Main_Load.TESTING) System.out.println("FindTri took "+(time*0.001)+"sec, found "+pairsAdded+" K2 and "+trisAdded+" K3");
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
		while((cycQ = g.findCycleDFS()) != null) {
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
