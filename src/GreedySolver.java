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
	
	static final boolean START_ARTICULATION_CHECK = false;
	static final int ARTICULATION_CHECK_FREQ = 100000;
	static final int ARTICULATION_MIN_N = 200;
	
	static final int FAST_SINKHORN_MARGIN = 10000;
	static final int DEGREE_HEURISTIC_SWITCH = 400000;
	static final int CLEANUP_MARGIN = -20000;
	
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
		
		int passesSinceLastArticulationCheck = START_ARTICULATION_CHECK ? ARTICULATION_CHECK_FREQ : 0;
		
		while(g.N-g.dropped_Size > 0) {

			if(USE_SCC) {
				SCC scc = new SCC();
				boolean sccSplit = scc.SCC(g);
				if(sccSplit) {
					if(Main_Load.VERBOSE)
						System.out.println("SCC split: "+scc.sccInfo.size()+" "+scc.sccInfo.stream().mapToInt(ArrayList::size).boxed().collect(Collectors.toList()));
					res = g.transformSolution(new ArrayList<Integer>());
					ArrayList<ReducedGraph> sccParts = g.splitOnSCC(scc, false);
					g = null;//free g
					if(skipCleanup)
						g_copy = null;
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
			
			int v0 = -1;
			long ms_left = Main_Load.msRemaining();
			
			if(g.real_N() >= ARTICULATION_MIN_N && passesSinceLastArticulationCheck >= ARTICULATION_CHECK_FREQ) {
				int artic = StrongArticulationPoints.articulations(g);
				if(Main_Load.VERBOSE)
					System.out.println("Articulation output: "+artic);
				if(artic != -1) {
					v0 = artic;
				}
				passesSinceLastArticulationCheck=0;
			} else {
				passesSinceLastArticulationCheck++;
			}
			
			if(v0 == -1) {
				if(ms_left > FAST_SINKHORN_MARGIN && g.real_N() < 500) {
					v0 = GreedyHeuristics.sinkhornHeuristic(g, 20);
				} else if(ms_left > DEGREE_HEURISTIC_SWITCH) {
					v0 = GreedyHeuristics.sinkhornHeuristic(g, 4);
				} else {
					v0 = GreedyHeuristics.degreeHeuristic(g);
				}
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
			
//			if(Main_Load.TESTING) {
//				if(g.real_N() <= 100 && g.real_N() > 3) {
//					g.condense();
//					System.out.println("Graph:");
////					System.out.println(g.toString());
//					g.dump();
//					System.out.println("===========");
//					System.out.println("SCC says "+new SCC().SCC(g));
//					System.out.println("RealN = "+g.real_N());
//					int artic = StrongArticulationPoints.articulations(g);
//					System.out.println("Articulation output: "+artic);
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
			if(Main_Load.msRemaining() < CLEANUP_MARGIN || g.hasCycleWithoutDropped()) {
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
