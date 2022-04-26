import java.util.ArrayList;
import java.util.stream.Collectors;

public class GreedySolver implements Solver {

	@Override
	public ArrayList<Integer> solve(Graph g) {
		
		if(g.N == 0) {
			return new ArrayList<Integer>(); 
		}

//		System.out.println("Choosing from "+g.N);
		
		//Choose based on highest degree-product vertex
		int bestVal = -1;
		int bestVert = -1;
		for(int i=0; i<g.N; i++) {
			int val = (1+g.inDeg[i]) * (1+g.outDeg[i]);
			if(val > bestVal) {
				bestVal = val;
				bestVert = i;
			}
		}
		if(bestVert == -1)
			throw new RuntimeException("No choice?");
		
		int v0 = bestVert;
//		System.out.println("Take out "+v0+", score="+bestVal);
		
		//Just add a self-loop. The pruner will add it to the FVS and go from there!
		g.eList[v0].add(v0); g.backEList[v0].add(v0); g.inDeg[v0]++; g.outDeg[v0]++;
		
		PruneLowdeg pruner = new PruneLowdeg(g);
		ArrayList<Integer> res = pruner.solve(this);
		
		return res;
	}


	static final boolean CLEANUP_AFTER = true;
	static final boolean CLEAN_WHEN_KILLED = true;
	static final boolean CLEANUP_DIRECTION_FWD = true;
	static final boolean USE_SCC = true;
	
	@Override
	public ArrayList<Integer> solve(ReducedGraph g) {
		return solve(g, false);
	}

	public ArrayList<Integer> solve(ReducedGraph g, boolean skipCleanup) {
		
		g.condense();//questionably necessary before the copy
		ReducedGraph g_copy = g.copy(false);

		int prePrune = g.real_N();
		g.prune();
		
//		System.out.println("Initial prune: N="+g.real_N()+" ("+prePrune+")");
		
		ArrayList<Integer> res = null; //will eventually hold solution
		
		while(g.N-g.dropped_Size > 0) {
//			long t0 = System.currentTimeMillis();
//			System.out.println("Choosing from "+g.real_N());

			if(Main_Load.is_killed) {
				//dump current solution
				
				res = new ArrayList<Integer>();
				for(int i=0; i<g.N; i++) {
					if(!g.dropped[i])
						res.add(i);
				}
				res = g.transformSolution(res);
				
				if(CLEAN_WHEN_KILLED)
					break;
				else
					return res;
			} else {
				//For testing that timeout handling works on the server as it should
//				try {
//					Thread.sleep(1000);
//					continue;
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
			}
			
			long ms_left = Main_Load.msRemaining();
			int v0;
			if(ms_left > 10000 && g.real_N() < 500) {
				v0 = GreedyHeuristics.sinkhornHeuristic(g, 20);
			} else if(ms_left > 400000) {
				v0 = GreedyHeuristics.sinkhornHeuristic(g, 4);
			} else {
				v0 = GreedyHeuristics.degreeHeuristic(g);
			}
//			int v0 = GreedyHeuristics.eigenHeuristic(g);
//			int v0 = FlowSolver.flowHeuristic(g);
			
			if(Main_Load.VERBOSE)
				System.out.println("Take out "+v0);
			
			//Just add a self-loop. The pruner will add it to the FVS and go from there!
			
			g.eList[v0].add(v0); g.backEList[v0].add(v0); g.inDeg[v0]++; g.outDeg[v0]++;
			
//			g.clearVertex(v0);
//			g.mustFVS[g.invMap[v0]] = true;
//			g.mustFVS_Size++;
			
			long t1 = System.currentTimeMillis();

			g.prune();

			if(USE_SCC) {
				SCC scc = new SCC();
				boolean sccSplit = scc.SCC(g);
				if(sccSplit) {
					if(Main_Load.VERBOSE)
						System.out.println("SCC split: "+scc.sccInfo.size()+" "+scc.sccInfo.stream().mapToInt(ArrayList::size).boxed().collect(Collectors.toList()));
					res = g.transformSolution(new ArrayList<Integer>());
					ArrayList<ReducedGraph> sccParts = g.splitOnSCC(scc, false);
					g = null;//free g
					for(ReducedGraph part : sccParts) {
						if(Main_Load.VERBOSE)
							System.out.println("Recurse on SCC of size "+part.real_N());
						res.addAll(this.solve(part, true));
						if(Main_Load.VERBOSE)
							System.out.println("Recursion complete");
					}
					break;
				}
			}
			
//			if(Main_Load.TESTING) {
//				if(g.real_N() <= 100 && g.real_N() > 3) {
//					g.condense();
//					System.out.println("Graph:");
////					System.out.println(g.toString());
//					g.dump();
//					System.out.println("===========");
//					System.out.println("SCC says "+new SCC().SCC(g));
//					System.out.println("RealN = "+g.real_N());
//					StrongArticulationPoints.articulations(g);
//					System.exit(0);
//				}
//			}
			
//			long t2 = System.currentTimeMillis();
//			System.out.println("T1 = "+(t1-t0)*0.001+"s,  T2 = "+(t2-t1)*0.001+"s");
		}
		
		if(res == null) {//alright, graph is clean, do our thing
			res = g.transformSolution(new ArrayList<Integer>());
		} //else
			//we got our solution from recursing on SCCs, or an early kill
		
		
		if(!CLEANUP_AFTER || skipCleanup)
			return res;
		if(Main_Load.VERBOSE)
			System.out.println("Solution has size "+res.size()+". Start cleanup at "+System.currentTimeMillis());
		
		//Try throwing out things we don't need from the solution, greedily
		g = g_copy;
		for(int v : res)
			g.dropped[v] = true;

//		for(int v : res) { //forward only	
		for(int vi=0; vi<res.size(); vi++) {
			int v=(CLEANUP_DIRECTION_FWD ? res.get(vi) : res.get(res.size()-1-vi));
		
			g.dropped[v] = false;
			if(Main_Load.msRemaining() < -20000 || g.hasCycleWithoutDropped()) {
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
