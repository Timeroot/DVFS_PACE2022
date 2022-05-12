import java.util.ArrayList;
import java.util.stream.Collectors;

public class ExactSolver {
	static final boolean USE_SCC = false;
	
	public static ArrayList<Integer> solve(Graph g_orig) {
		ReducedGraph rg = ReducedGraph.fromGraph(g_orig);
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
					res.addAll(ExactSolver.solveSCC(part, true));
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
//		if(reprune)
//			rg_scc.prune();
		if(Main_Load.TESTING) 
			System.out.println("SCC: V="+rg_scc.real_N()+", E="+rg_scc.E());
//		System.out.println("Heuristic primal: "+ExactSolver_Heuristic.solve(rg_scc).size());

		ArrayList<Integer> res =
//				new JNASCIPSolver()
//				new JNASCIPSolver_Reopt()
//				new SCIPSolver()
				new MinimumCoverSolver()
				.solve(Graph.fromReducedGraph(rg_scc));
		res = rg_scc.transformSolution(res);
		return res;
	}
}
