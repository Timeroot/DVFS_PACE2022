import java.util.ArrayList;
import java.util.stream.Collectors;

//A heuristic DVFS solver specifically for use in exact solving trees 
//It produces a fast-and-decent solution, as opposed to trying to produce
//a really great heuristic solution. Just enough to cut off the search tree.
//Does a greedy search alternating with pruning.
public class Heuristic_DVFS_Fast {
	public static ArrayList<Integer> solve(ReducedGraph g) {
		ReducedGraph g_copy = g.copy(false);

		g.prune();
		
		ArrayList<Integer> res = null; //will eventually hold solution
		
		while(g.N-g.dropped_Size > 0) {
			int v0 = GreedyHeuristics.sinkhornHeuristic(g, 4);
//			int v0 = GreedyHeuristics.degreeHeuristic(g);
			
			if(Main_Load.VERBOSE)
				System.out.println("Take out "+v0);
			
			//Just add a self-loop. The pruner will add it to the FVS and go from there
			g.addEdge(v0, v0);
			g.prune();
		}
		
		if(res == null) {//alright, graph is clean, do our thing
			res = g.transformSolution(new ArrayList<Integer>());
		}
		
		
		if(Main_Load.VERBOSE)
			System.out.println("Solution has size "+res.size()+". Start cleanup at "+System.currentTimeMillis());
		
		//Try throwing out things we don't need from the solution, greedily
		g = g_copy;
		for(int v : res)
			g.dropped[v] = true;

		for(int v : res) {
			g.dropped[v] = false;
			if(g.hasCycleWithoutDropped()) {
				g.dropped[v] = true;
			} else {
				if(Main_Load.VERBOSE)
					System.out.println("Saved "+v);
			}
		}
		
		res.clear();
		for(int i=0; i<g.N; i++) {
			if(g.dropped[i])
				res.add(i);
		}

		if(Main_Load.VERBOSE)
			System.out.println("Reduced solution has size "+res.size()+". Finish cleanup at "+System.currentTimeMillis());
		return res;
	}
}
