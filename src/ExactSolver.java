import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class ExactSolver {
	static final boolean USE_SCC = true;
	
	//First steps to exact solving: split into strongly connected components,
	//and then prune using reduction rules for DVFS (vertices with indeg<=1 or outdeg<=1),
	//The pruning can't break the SCC any more.
	//
	// solve() does a prune and splits into SCC, and then passes the (one or more) components to solveSCC().
	// solveSCC() then handles the minimized directed graph using MinimumCoverDescriptor,
	// which solves the rest. (See that file)
	public static ArrayList<Integer> solve(Graph g_orig) {
		ReducedGraph rg = ReducedGraph.wrap(g_orig, true);//ReducedGraph.fromGraph(g_orig);
		rg.prune();
		
		if(USE_SCC) {
			SCC scc = new SCC();
			boolean sccSplit = scc.doSCC(rg);
			if(sccSplit) {
				if(Main_Load.VERBOSE)
					System.out.println("SCC split: "+scc.sccInfo.size()+" "+scc.sccInfo.stream().mapToInt(ArrayList::size).boxed().collect(Collectors.toList()));
				ArrayList<ReducedGraph> sccParts = rg.splitOnSCC(scc, false);
				ArrayList<Integer> res = new ArrayList<>();
				for(ReducedGraph part : sccParts) {
					if(Main_Load.VERBOSE)
						System.out.println("Recurse on SCC of size "+part.real_N());
					
					ArrayList<Integer> sccRes = ExactSolver.solveSCC(part, true);
					
					if(sccRes == null)//no solution (for some reason, probably debugging purposes)
						return null;
					
					//add it to the solution we're building
					res.addAll(sccRes);
					if(Main_Load.VERBOSE)
						System.out.println("Recursion complete");
				}
				res = rg.transformSolution(res);
				return res;
			} else {
				if(Main_Load.VERBOSE)
					System.out.println("All one big SCC");
			}
		}
		
		return solveSCC(rg, false);
	}
	
	public static ArrayList<Integer> solveSCC(ReducedGraph rg_scc, boolean reprune) {
		if(reprune)
			rg_scc.prune();
		if(Main_Load.VERBOSE) 
			System.out.println("SCC: V="+rg_scc.real_N()+", E="+rg_scc.E());
//		System.out.println("Heuristic primal: "+ExactSolver_Heuristic.solve(rg_scc).size());

		MinimumCoverDescriptor solver = new MinimumCoverDescriptor();
		ArrayList<Integer> res = solver.solve(Graph.fromReducedGraph(rg_scc));
//		ArrayList<Integer> res = new ILP_DVFS_Reopt().solve(Graph.fromReducedGraph(rg_scc));
		
		if(res == null)//no solution (for some reason, probably debugging purposes)
			return res;
		
		if(Main_Load.VERIFY_DVFS){//verify
			Graph check = Graph.fromReducedGraph(rg_scc);
			for(int v : res)
				check.clearVertex(v);
			ArrayDeque<Integer> cyc = check.findCycleDFS(null);
			if(cyc != null) {
				//BAD SOLUTION!
				System.out.println(cyc);
				throw new RuntimeException("Didn't pass verifying!");
			}
		}
		
		res = rg_scc.transformSolution(res);
		return res;
	}
}
