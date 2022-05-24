import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Stack;

//Tries to reduce the problem to a minimum cover problem and solve that.
//Returns null if it fails to find a solution (like all Solvers).
//If (after all reductions) the graph consists of just K2s, the problem
//still has the nice property that it can be completely solved via matching.
//With a few K3s or Cn's it can still be fast.
public class MinimumCoverDescriptor {

	ArrayList<int[]> pairList;
	LinkedList<int[]> bigCycleList;
	
	static final int BRUTE_LIMIT = 22;
	
	public MinimumCoverDescriptor() {}

	public ArrayList<Integer> solve(Graph g) {
		if(Main_Load.TESTING)
			System.out.println("Solving with MinimumCoverDescriptor");
		pairList = new ArrayList<int[]>();
		bigCycleList = new LinkedList<int[]>();
		
		//Find all K2's
		for(int i=0; i<g.N; i++) {
			for(int j : g.eList[i]) {
				if(j < i)//skip
					continue;
				
				//check for a pair
				if(g.eList[j].contains(i)) {
					pairList.add(new int[]{i,j});
					continue;
				}
			}
		}
		
		//Remove all K2's from the graph
		for(int[] cycle : pairList) {
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
		}
		g.checkConsistency();
		int rem_E = g.E();
		if(Main_Load.TESTING)
			System.out.println("Dropped "+pairList.size()+" K2's, now E="+rem_E);
		
		//Do an SCC strip on remaining graph
		int scc_stripped = 0;
		if(rem_E > 0) {
			scc_stripped = stripSCC(g);
			rem_E = g.E();
			if(Main_Load.TESTING)
				System.out.println("After SCC check, E="+rem_E);
		}
		g.checkConsistency();
		
		//Repeatedly strip any degree zero vertices now
		//EDIT: Realized this is subsumed by SCC analysis.
		//actually let's try checking this
		int d0_stripped = stripDegZero(g);
		if(d0_stripped != 0)
			throw new RuntimeException("StripSCC -> stripDegZero made progress? "+d0_stripped);

		if(g.E() == 0) {
			if(bigCycleList.size() != 0)
				throw new RuntimeException("How did we get big cycles so early?");
			if(Main_Load.TESTING)
				System.out.println("EASY: Vertex cover problem");
			return VertexCoverReductions.solve(pairList, g.N);
		}
		
		if(Main_Load.TESTING)
			System.out.println("Stripping complete");
		
		//Is it all cycles?
		boolean isAllCycles = isAllCycles(g);
		if(Main_Load.TESTING)
			System.out.println("isAllCycles == "+isAllCycles);
		
		killSimpleCycles(g);
		
		rem_E = g.E();
		if(isAllCycles != (rem_E == 0))
			throw new RuntimeException("I don't understand what a cycle is");

		g.checkConsistency();
		
		//Check for skip-merges:
		//   a path triple a->b,b->c,a->c, where a and c are the only neighbors of b.
		// Then the edges through a->b->c are irrelevant and can be removed.
		// This can't trigger further advance scc stripping.
		//
		//skipMergeLong improves this to include things like a->b->c->d->e, a->e.
		if(!isAllCycles) {
			int skipMerged = skipMerge(g);
			if(Main_Load.TESTING)
				System.out.println("Skipmerged = "+skipMerged);
			isAllCycles = isAllCycles(g);
			if(Main_Load.TESTING)
				System.out.println("isAllCycles2 == "+isAllCycles);
			
			killSimpleCycles(g);
			rem_E = g.E();
			if(isAllCycles != (rem_E == 0))
				throw new RuntimeException("I don't understand what a cycle is [2]");
		}

		g.checkConsistency();
		
		if(!isAllCycles) {
			int skipMergeLong = skipMergeLong(g);
			if(skipMergeLong > 0) {
				if(Main_Load.TESTING)
					System.out.println("skipMergeLong! is "+skipMergeLong);
				isAllCycles = isAllCycles(g);
				if(Main_Load.TESTING)
					System.out.println("isAllCycles3 == "+isAllCycles);
				
				killSimpleCycles(g);
				
				rem_E = g.E();
				if(isAllCycles != (rem_E == 0))
					throw new RuntimeException("I don't understand what a cycle is [3]");
			}
		}

		g.checkConsistency();
		rem_E = g.E();

//		g.dump();

		ArrayList<GraphChunk> rem_chunks = null;
		if(rem_E > 0) {
			if(Main_Load.TESTING)
				System.out.println("Start processSCC");
			rem_chunks = processSCC(g);
			//all edges should be pulled out into chunks
			
			rem_E = g.E();
			if(rem_E != 0)
				throw new RuntimeException("processSCC didn't clear it?");
			
			isAllCycles = (rem_chunks == null);
			if(Main_Load.TESTING)
				System.out.println("ProcessSCC "+(isAllCycles?"worked":"failed"));
			
			if(rem_chunks != null) {
				if(Main_Load.TESTING)
					System.out.println("Couldn't split");
				if(Main_Load.TESTING) {
					System.out.println("Rem_chunks = "+rem_chunks);
				}
			}
		}
		
		//try to remove any big cycles that are implied by K2's.
		dropImpliedBigCycles(g);
		
		if(isAllCycles) {
			if(bigCycleList.size() == 0) {
				if(Main_Load.TESTING) {
					System.out.println("EASY: Vertex cover problem");
//					dumpK2Graph();
				}
//				return null;
				return VertexCoverReductions.solve(pairList, g.N);
			} else {
				if(Main_Load.TESTING)
					System.out.println("MEDIUM: Minimum cover.");
				int maxCycleSize = 0;
				for(int[] cycle : bigCycleList)
					maxCycleSize = Math.max(maxCycleSize, cycle.length);
				if(Main_Load.TESTING)
					System.out.println(bigCycleList.size()+" many bigcycles, maximum size "+maxCycleSize);
				
				return CycleCoverReductions.solve(pairList, bigCycleList, g.N);
				
//				MinimumCoverInfo mci = new MinimumCoverInfo(g.N, pairList, bigCycleList, null, null);
//				boolean[] sol = new ILP_MinimumCover().solve(mci);
//				return getTrues(sol);
				//if that returns, we definitely got a solution!
//				return null;
			}
		} else {
			if(Main_Load.TESTING)
				System.out.println("HARD: Not minimum cover!");
			MinimumCoverInfo mci = new MinimumCoverInfo(g.N, pairList, new ArrayList<int[]>(bigCycleList), rem_chunks, null);
			ArrayList<Integer> sol = new ILP_CoverAndChunks_Reopt().solve(mci);
			if(Main_Load.TESTING)
				System.out.println("!!!!!!");
			return sol;
//			g.dump();
		}
		//check e_019 for improved skipMerge
		
		//rule: if only one vertex with indeg>1, or only one with outdeg>1, can use all cycles through that
		
		//Check if it is purely cycles at this point
//		System.exit(1);
	}
	
	private void dropImpliedBigCycles(Graph g) {
		//need fast neighbor lists
		int N = g.N;
		int[] deg = new int[N];
		for(int[] pair : pairList) {
			deg[pair[0]]++;
			deg[pair[1]]++;
		}
		int[][] neighbors = new int[N][];
		for(int v=0; v<N; v++) {
			neighbors[v] = new int[deg[v]];
		}
		for(int[] pair : pairList) {
			//forgive me god for I have sinned
			neighbors[pair[0]][--deg[pair[0]]] = pair[1];
			neighbors[pair[1]][--deg[pair[1]]] = pair[0];
		}
		for(int v=0; v<N; v++) {
			Arrays.sort(neighbors[v]);
		}
		int dropped = 0;

		cycLoop: for(Iterator<int[]> iter = bigCycleList.iterator();
				iter.hasNext();) {
			int[] cyc = iter.next();
			for(int v1 : cyc) {
				for(int v2 : cyc) {
					if(v2 <= v1)
						continue;
					if(Arrays.binarySearch(neighbors[v1], v2) >= 0) {
						iter.remove();
//						System.out.println("Dropped "+Arrays.toString(cyc));
//						System.out.println(v1+", "+v2);
						dropped++;
						continue cycLoop;
					}
				}
			}
		}
		if(Main_Load.TESTING)
			System.out.println("Dropped "+dropped+" out of "+(dropped+bigCycleList.size()));
	}

	static ArrayList<Integer> getTrues(boolean[] bools){
		ArrayList<Integer> res = new ArrayList<>();
		for(int i=0; i<bools.length; i++)
			if(bools[i])
				res.add(i);
		return res;
	}
	
	void dumpK2Graph() {
		System.out.print("{");
		for(int[] pair : pairList) {
			System.out.print("{"+(1+pair[0])+","+(1+pair[1])+"},");
			if(Math.random() < 0.1)
				System.out.println();
		}
		System.out.println("}");
		System.exit(1);
	}
	
	//returns how many simple cycles were added
	int killSimpleCycles(Graph g) {
		int cyclesFound = 0;
		boolean[] visited = new boolean[g.N];
		iLoop: for(int i=0; i<g.N; i++) {
			if(visited[i])
				continue;
			visited[i] = true;
			
			int inDeg = g.inDeg[i];
			int outDeg = g.outDeg[i];
			if(inDeg != 1 || outDeg != 1)
				continue iLoop;

			//might be in a cycle. Follow.
			int v = i;
			int cycleLen = 1;
			while(true) {
				v = g.eList[v].iterator().next();
				
				if(v == i){//yup, found a simple cycle
					int[] cycle = new int[cycleLen];
					int dest = 0;
					do {
						v = g.eList[v].iterator().next();
						cycle[dest++] = v;
					} while(v != i);
//					if(Main_Load.TESTING)
//						System.out.println(" + Cycle "+Arrays.toString(cycle));
					bigCycleList.add(cycle);
					cyclesFound++;
					
					for(int vc : cycle) {
						g.eList[vc].clear();
						g.backEList[vc].clear();
						g.inDeg[vc] = g.outDeg[vc] = 0;
					}
					continue iLoop;
				}
				
				if(visited[v])//nope, been here before and it didn't work
					continue iLoop;
				visited[v] = true;
				
				inDeg = g.inDeg[v];
				outDeg = g.outDeg[v];
				if(inDeg != 1 || outDeg != 1)//nope, not a simple path
					continue iLoop;
				//else continue following
				cycleLen++;
			}
		}
		return cyclesFound;
	}
	
	int skipMerge(Graph g) {
		int skipMerged = 0;
		for(int i=0; i<g.N; i++) {
			int inDeg = g.inDeg[i];
			int outDeg = g.outDeg[i];
			if(inDeg == 1 && outDeg == 1) {
				int prevA = g.backEList[i].iterator().next();
				int nextC = g.eList[i].iterator().next();
				if(g.eList[prevA].contains(nextC)) {
					//Simplify!
					g.eList[i].clear();
					g.backEList[i].clear();
					g.inDeg[i] = 0;
					g.outDeg[i] = 0;
					g.eList[prevA].remove(i);
					g.outDeg[prevA]--;
					g.backEList[nextC].remove(i);
					g.inDeg[nextC]--;
					skipMerged++;
				}
			}
		}
		return skipMerged;
	}

	//A 3-cycle with a degree-2 vertex can be added to the cycle list and have
	//the degree-2 vertex removed:
	// in a->b->c->a, with extra edges d->b->e and f->c->g, (so a has indeg=outdeg=1)
	// add {a,b,c} to cycles and delete a.
	int dropC3(Graph g) {
		int dropped = 0;
		for(int i=0; i<g.N; i++) {
			int inDeg = g.inDeg[i];
			int outDeg = g.outDeg[i];
			if(inDeg == 1 && outDeg == 1) {
				int prevA = g.backEList[i].iterator().next();
				int nextC = g.eList[i].iterator().next();
				if(g.backEList[prevA].contains(nextC)) {
//					if(true)
//						System.exit(1);
					//Simplify!
					g.eList[i].clear();
					g.backEList[i].clear();
					g.inDeg[i] = 0;
					g.outDeg[i] = 0;
					g.eList[prevA].remove(i);
					g.outDeg[prevA]--;
					g.backEList[nextC].remove(i);
					g.inDeg[nextC]--;
					
					bigCycleList.add(new int[]{i, prevA, nextC});
					g.checkConsistency();
					dropped++;
				}
			}
		}
		return dropped;
	}
	
	//From a vertex v, follows a path from v until it hits a vertex with
	//indegree > 1 or outdegree > 1 (and return that) or until it hits v,
	//in which case return v.
	int followPathFrom(Graph g, int v) {
		int v0 = v;
		if(g.outDeg[v0] > 1 || g.inDeg[v0] > 1)
			throw new RuntimeException("Bad call with v="+v);
		do {
			v = g.eList[v].iterator().next();
			if(g.inDeg[v] > 1 || g.outDeg[v] > 1)
				return v;
		} while(v != v0);
		return v0;
	}
	
	//Deletes all edges from v forward until it hits something indeg>1 or
	//outdeg>1. Throws an error if it hits itself.
	int clearPathFrom(Graph g, int v) {
		int v0 = v;
		if(g.outDeg[v0] > 1 || g.inDeg[v0] > 1)
			throw new RuntimeException("Bad call with v="+v);
		do {
			int vNew = g.eList[v].iterator().next();
			int vNewIndeg = g.inDeg[vNew];//since we're about to change it
			
			g.inDeg[vNew]--;
			g.backEList[vNew].remove(v);
			g.eList[v].clear();
			g.outDeg[v] = 0;
			
			v = vNew;
			if(vNewIndeg > 1 || g.outDeg[v] > 1)
				return v;
			
		} while(v != v0);
		
		throw new RuntimeException("clearPathFrom gave a cycle?");
	}

	int skipMergeLong(Graph g) {
		int skipMerged = 0;
		iLoop: for(int i=0; i<g.N; i++) {
			int inDeg = g.inDeg[i];
			int outDeg = g.outDeg[i];
			if(inDeg == 1 && outDeg == 1) {
				int prevA = g.backEList[i].iterator().next();
				if(g.outDeg[prevA] == 1)
					continue iLoop;
				int nextC = followPathFrom(g, i);
//				System.out.println("Landed from "+i+" to "+nextC);
				if(g.eList[prevA].contains(nextC)) {
//					System.out.println("Cleared! Because "+prevA+" -> "+nextC);
					//Simplify!
					g.backEList[i].clear();
					g.inDeg[i] = 0;
					g.eList[prevA].remove(i);
					g.outDeg[prevA]--;
					int clearRes = clearPathFrom(g, i);
					if(clearRes != nextC)
						throw new RuntimeException("Inconsistent path following");
					skipMerged++;
				}
			}
		}
		return skipMerged;
	}
	
	boolean isAllCycles(Graph g) {
		boolean pass = true;
		for(int i=0; i<g.N; i++) {
			int inDeg = g.inDeg[i];
			int outDeg = g.outDeg[i];
			if(inDeg == 0 && outDeg == 0)
				continue;
			if(inDeg == 1 && outDeg == 1)
				continue;
			if(inDeg == 0 && outDeg != 0)
				throw new RuntimeException("What?");
			if(inDeg != 0 && outDeg == 0)
				throw new RuntimeException("What?");
			pass=false;
		}
		return pass;
	}

	//Notes SCCs and removes edges between them
	int stripSCC(Graph g) {
		ReducedGraph rg = ReducedGraph.wrap(g, true);
		SCC scc = new SCC();
		boolean sccSplit = scc.doSCC(rg);
		if(Main_Load.TESTING)
			System.out.println("SCC did split? "+sccSplit+", "+scc.sccInfo.size());
		if(!sccSplit)
			return 0;
		int sccStrip = scc.stripInterSCCEdges(rg);
		return sccStrip;
	}
	
	int stripDegZero(Graph g) {
		int stripped = 0;
		ArrayDeque<Integer> toDrop = new ArrayDeque<Integer>();
		for(int i=0; i<g.N; i++) {
			int inDeg = g.inDeg[i];
			int outDeg = g.outDeg[i];
			if((inDeg == 0 && outDeg != 0) || (outDeg == 0 && inDeg != 0)) {
				toDrop.add(i);
			}
		}
		while(toDrop.size() > 0) {
			int i = toDrop.pollFirst();
//			if(Main_Load.TESTING)
//				System.out.println("Processing "+i);
			int inDeg = g.inDeg[i];
			int outDeg = g.outDeg[i];
			if(inDeg == 0 && outDeg > 0) {
				stripped += outDeg;
				for(int no : g.eList[i]) {
					g.inDeg[no]--;
					g.backEList[no].remove(i);
					if(g.inDeg[no] ==0 ) {
						toDrop.add(no);
					}
				}
				g.outDeg[i] = 0;
				g.eList[i].clear();
				
			} else if(outDeg == 0 && inDeg > 0) {
				stripped += inDeg;
				for(int no : g.backEList[i]) {
					g.outDeg[no]--;
					g.eList[no].remove(i);
					if(g.outDeg[no] == 0) {
						toDrop.add(no);
					}
				}
				g.inDeg[i] = 0;
				g.backEList[i].clear();
				
			} else {
//				if(Main_Load.TESTING)
//					System.out.println("Hit "+i+" redundantly?");
			}
		}
		return stripped;
	}

	//Cuts into SCCs and processes each component separately.
	//Returns null if fully resolved into cycles. Otherwise returns chunks
	ArrayList<GraphChunk> processSCC(Graph g) {
		ReducedGraph rg = ReducedGraph.wrap(g, true);
		SCC scc = new SCC();
		boolean sccSplit = scc.doSCC(rg);
		if(!sccSplit) {
			if(Main_Load.TESTING)
				System.out.println("No split");
			//alright, it didn't split. Still make the "chunk" and process
			GraphChunk chunk = GraphChunk.wrap(g.copy());
			g.clear();
			processChunk(chunk.gInner);
			if(chunk.gInner.E() != 0) {
//				chunk.addInto(g);
//				chunk.gInner.dump();
				ArrayList<GraphChunk> res = new ArrayList<>();
				res.add(chunk);
				return res;
			} else {
				return null;
			}
		}
		
		ArrayList<GraphChunk> res = new ArrayList<>();
		for(ArrayList<Integer> sccComp : scc.sccInfo) {
			int bigCycleListSizeBefore = bigCycleList.size();
			GraphChunk chunk = new GraphChunk(g, sccComp, true);
//			if(Main_Load.TESTING)
//				System.out.println("processSCC in on "+Arrays.toString(chunk.mapping));
			processChunk(chunk.gInner);
			fixChunksSince(bigCycleListSizeBefore, chunk.mapping);
			if(chunk.gInner.E() != 0) {
//				chunk.addInto(g);
				res.add(chunk);
			}
		}
//		g.dump();
//		return 1;
		if(res.size() == 0) {
			return null;
		} else {
			return res;
		}
	}
	
	void fixChunksSince(int bigCycleListSizeBefore, int[] mapping) {
		for(int i=bigCycleListSizeBefore; i<bigCycleList.size(); i++) {
			int[] cycle = bigCycleList.get(i);
			for(int vi=0; vi<cycle.length; vi++) {
				cycle[vi] = mapping[cycle[vi]];
			}
		}
	}
	
	void processChunk(Graph g) {
//		System.out.println("Start chunk with "+Arrays.toString(chunk.mapping));
//		int bigCycleListI = bigCycleList.size();
		
//		g.dump();
//		System.out.println("Chunky chunk "+Arrays.toString(chunk.mapping));

		stripDegZero(g);
		boolean didSkipOrDrop = false;
		while((skipMergeLong(g) > 0) ||
				(dropC3(g) > 0)) {
			stripDegZero(g);
			didSkipOrDrop = true;
			if(Main_Load.TESTING)
				System.out.println("Another skipMerge or dropC3 was useful...");
			g.checkConsistency();
		}
		killSimpleCycles(g);
		if(g.E() == 0) {
			return;
		}
		stripSCC(g);
		if(checkSingleMultivert(g)) {
			return;
		}
		splitWAP(g);
	}
	
	//splits on weak articulation points
	void splitWAP(Graph chunk) {
		int wap = WeakArticulationPoints.AP_One(chunk);
//		if(Main_Load.TESTING)
//			System.out.println("findAP = "+findAP);
		if(wap == -1) {
			if(Main_Load.TESTING)
				System.out.println("Couldn't split with WAP, fall to splitEdge");
			splitEdge(chunk);
			if(Main_Load.TESTING && chunk.E() > 0) {
				System.out.println("splitWAP didn't split AND splitEdge didn't solve");
			}
			return;
		}
		if(Main_Load.TESTING) {
//			System.out.println("Split at "+wap+" on: ");
//			chunk.gInner.dump();
		}

		//Now we have a WAP, split into connected components.
		//each one gets the WAP itself, too.
		int N = chunk.N;
		boolean[] visited = new boolean[N];
		int[] compId = new int[N];
		Stack<Integer> queue = new Stack<Integer>();
		int numComps = 0;
		int nextToVisit = 0;
		visited[wap] = true;
		while(queue.size() > 0 || nextToVisit < N) {
			if(queue.size() == 0) {
				if(visited[nextToVisit]) {
					nextToVisit++;
					continue;
				}
				numComps++;
				queue.add(nextToVisit++);
			}
			int v = queue.pop();
			if(visited[v])
				continue;
			compId[v] = numComps-1;
			visited[v] = true;
			queue.addAll(chunk.eList[v]);
			queue.addAll(chunk.backEList[v]);
		}

		@SuppressWarnings("unchecked")
		ArrayList<Integer>[] components = new ArrayList[numComps];
		for(int i=0; i<numComps; i++) {
			components[i] = new ArrayList<Integer>();
			if(i>0)
				components[i].add(wap);
		}
		for(int v=0; v<N; v++) {
			components[compId[v]].add(v);
		}
//		System.out.println("Split into "+Arrays.toString(components));
		
		for(ArrayList<Integer> comp : components) {
			int bigCycleListSizeBefore = bigCycleList.size();
			GraphChunk gc = new GraphChunk(chunk, comp, true);
			if(Main_Load.TESTING)
				System.out.println("splitWAP in with mapping "+Arrays.toString(gc.mapping));
			processChunk(gc.gInner);
			fixChunksSince(bigCycleListSizeBefore, gc.mapping);
			if(gc.gInner.E() != 0) {
//				if(Main_Load.TESTING)
//					System.out.println("Didn't complete");
				gc.addInto(chunk);
				//TODO instead of adding chunks back in (and agglutinating over WAPs)
				// keep them separate for other processing, maybe?
			}
		}
//		System.out.println("After WAP is ");
//		chunk.gInner.dump();
		if(Main_Load.TESTING) {
			if(chunk.E() == 0) 
				System.out.println("SplitWAP SOLVED this chunk");
			else {
				System.out.println("splitWAP split but didn't solve this chunk");
				chunk.dump();
			}
		}
	}
	
	//Idea here is that for small chunks (< 10 vertices?) we can find a
	//minimal set of cycles by brute force.
	void bruteForceCycleEnumerate(Graph g) {
		int N = g.N;
		
		if(g.N > BRUTE_LIMIT) {
			if(g.nonZeroDegN() <= BRUTE_LIMIT) {
				if(Main_Load.TESTING)
					System.out.println("Brute force reduced to mini-chunk bc N="+g.N);
				GraphChunk reduced = GraphChunk.nonzeroVerts(g, true);
				int bigCycleListSizeBefore = bigCycleList.size();
				bruteForceCycleEnumerate(reduced.gInner);
				fixChunksSince(bigCycleListSizeBefore, reduced.mapping);
				return;
			}
			if(Main_Load.TESTING) {
				System.out.println("Brute force not running on size "+g.N);
				g.dump();
			}
			return;
		}
		
		
		//Is the bitfield i enough that their removal makes it acyclic?
		boolean[] isSufficient = new boolean[1<<N];
		for(int flags = (1<<N); flags-->0; ) {
//			System.out.println("Try flags = 0b"+Integer.toBinaryString(flags));
			boolean areAllParentSuff = true;
			//check if all parents (this + one more) are sufficient, first
			for(int parentI = 0; parentI < N; parentI++) {
				int parent = flags | (1<<parentI);
				if(parent == flags)
					continue;//already contained in flags
				if(!isSufficient[parent]) {
//					System.out.println(" Insufficient parent 0b"+Integer.toBinaryString(parent));
					areAllParentSuff = false;
					break;
				}
			}
			if(!areAllParentSuff) {
				//this's an insufficient smaller set. skip
				//isSufficient[flags] = false;
				continue;
			}
			boolean isFSuff = isDVFS(g, flags, N);
//			System.out.println(" isDVFS gives "+isFSuff);
			if(isFSuff) {
				isSufficient[flags] = true;
			} else {
				//this is insufficient to remove, but this plus any one
				//does suffice. So the logical negation is a good cycle
				int cycleLen = N - Integer.bitCount(flags);
				int[] cycle = new int[cycleLen];
				int arrDest = 0;
				for(int i=0; i<N; i++) {
					if((flags & (1<<i)) == 0) {
//						System.out.println((1<<i)+" & --> "+(flags & (1<<i)));
						cycle[arrDest++] = i;
					}
				}
//				System.out.println(" *"+Arrays.toString(cycle));
				bigCycleList.add(cycle);
			}
		}
		//all minimal sets have been added
		g.clear();
	}
	
	//Slow but effective way to break down a big chunk.
	//Look for an edge (u,v) such that removing u and v splits the graph into SCCs.
	//Then you can look at just cycles in graphs A+{u,v} and B+{u,v}.
	//There's probably a linear-time algorithm for this a-la WAP, but alas, I do not know it.
	void splitEdge(Graph chunk_orig) {
//		if(true) {
//			bruteForceCycleEnumerate(chunk_orig);
//			return;
//		}
		
		int N = chunk_orig.N;
		int[] compId = new int[N];
		
		boolean didSplit = false;
		int splitU = -1, splitV = -1;
		int numComps = -1;
		
		for(int u = 0; u<N; u++) {
			for(int v : chunk_orig.eList[u]) {
				//Make a version of the graph with this u and v gone
				Graph chunk_minus = chunk_orig.copy();
				chunk_minus.clearVertex(u);
				chunk_minus.clearVertex(v);
				
				boolean[] visited = new boolean[N];
				Arrays.fill(compId, 0);
				Stack<Integer> queue = new Stack<Integer>();
				numComps = 0;
				int nextToVisit = 0;
				//mark the two special
				visited[u] = true;
				visited[v] = true;
				compId[u] = -1;
				compId[v] = -1;
				
				while(queue.size() > 0 || nextToVisit < N) {
					if(queue.size() == 0) {
						if(visited[nextToVisit]) {
							nextToVisit++;
							continue;
						}
						numComps++;
						queue.add(nextToVisit++);
					}
					int vis = queue.pop();
					if(visited[vis])
						continue;
					compId[vis] = numComps-1;
					visited[vis] = true;
					queue.addAll(chunk_minus.eList[vis]);
					queue.addAll(chunk_minus.backEList[vis]);
				}
				if(numComps > 1) {
					if(Main_Load.TESTING)
						System.out.println("Num comps = "+numComps+" for u="+u+", v="+v);
					didSplit = true;
					splitU = u;
					splitV = v;
					//save compId and numComps
					break;
				}
			}
			if(didSplit)
				break;
		}
		
		if(!didSplit) {
			if(Main_Load.TESTING)
				System.out.println("Couldn't splitEdge");
			//try to clear up manually if we can
//			ChordlessCycleEnum.enumCycles(chunk_orig, null);//can't use K2's right now because mapping issues
			
//			bruteForceCycleEnumerate(chunk_orig);
			
			ArrayList<int[]> enumerated = ChordlessCycleEnum.enumTreeSimple(chunk_orig);
			if(enumerated == null) {
				if(Main_Load.TESTING)
					System.out.println("ChordlessCycleEnum quit");
				//didn't resolve. :(
			} else {
				bigCycleList.addAll(enumerated);
				chunk_orig.clear();
			}
			return;
		}

//		System.out.println(Arrays.toString(compId));
//		chunk_orig.dump();
		
		@SuppressWarnings("unchecked")
		ArrayList<Integer>[] components = new ArrayList[numComps];
		for(int i=0; i<numComps; i++) {
			components[i] = new ArrayList<Integer>();
			components[i].add(splitU);
			components[i].add(splitV);
		}
		for(int v=0; v<N; v++) {
			if(v == splitU || v == splitV)
				continue;
			components[compId[v]].add(v);
		}
		if(Main_Load.TESTING) {
			System.out.println("Split with "+splitU+"->"+splitV+" into "+Arrays.toString(components));
		}
		
		//each chunk is taken destructively, and this removes the "shared" edge u->v.
		//if we just take the chunks, that means only the first component would get the edge, instead of
		//all of them. So before each chunk formation we need to add it in.
		//This might be already there -- if we added it back with the gc.addInto() if 
		//the relevant chunk failed to resolve.
		//So we actually keep track of "did we always have to add it back in". If we ever
		//didn't, then it should be in the final graph, so we make sure to add it at the end.
		
		//start without it.
		chunk_orig.clearEdge(splitU, splitV);
		
		boolean alwaysNeededExtraEdge = true;
		for(ArrayList<Integer> comp : components) {
			//add the one extra edge to chunk_orig if it's missing
			if(!chunk_orig.eList[splitU].contains(splitV)) {
				chunk_orig.addEdge(splitU, splitV);
			} else {
				alwaysNeededExtraEdge = false;
			}
			GraphChunk gc = new GraphChunk(chunk_orig, comp, true);
//			System.out.println("Subgraph is ");
			if(Main_Load.TESTING) {
				System.out.println("splitEdge in on size "+gc.gInner.N);
				System.out.println("Mapping = "+Arrays.toString(gc.mapping));
			}
			int bigCycleListSizeBefore = bigCycleList.size();
			processChunk(gc.gInner);
			fixChunksSince(bigCycleListSizeBefore, gc.mapping);
			if(Main_Load.TESTING) {
				System.out.println("splitEdge out");
			}
			if(gc.gInner.E() != 0) {
//				if(Main_Load.TESTING)
//					System.out.println("Didn't complete");
				gc.addInto(chunk_orig);
				//TODO instead of adding chunks back in (and agglutinating over WAPs)
				// keep them separate for other processing, maybe?
			}
		}
		
		if(!alwaysNeededExtraEdge) {
			chunk_orig.addEdge(splitU, splitV);
		}
	}
	
	boolean isDVFS(Graph g_orig, int flags, int N) {
		Graph g = g_orig.copy();
		for(int i=0; i<N; i++) {
			if((flags & (1<<i)) != 0)
				g.clearVertex(i);
		}
		return !g.hasCycle(); 
	}
	
	//Checks if the graph has a single vertex with indeg>1 (or a single with outdeg>1).
	//In this case, you can describe the graph just by the cycles from each those options,
	//pretty easily. Returns "true" if this worked and the graph was resolved.
	boolean checkSingleMultivert(Graph g) {
		System.gc();
		int numWithIndegOver1 = 0;
		int numWithOutdegOver1 = 0;
		for(int i=0; i<g.N; i++) {
			int inDeg = g.inDeg[i];
			int outDeg = g.outDeg[i];
			if(inDeg > 1)
				numWithIndegOver1++;
			if(outDeg > 1)
				numWithOutdegOver1++;
			if(inDeg == 0 && outDeg != 0)
				throw new RuntimeException("What[1]? @ "+i);
			if(inDeg != 0 && outDeg == 0)
				throw new RuntimeException("What[2]? @ "+i);
		}
		if(numWithIndegOver1 == 1) {
			int highdeg = -1;
			for(int i=0; i<g.N; i++) {
				int inDeg = g.inDeg[i];
				if(inDeg > 1) {
					highdeg = i; break;
				}
			}
			if(Main_Load.TESTING) {
				if(g.N==20)System.out.println(g.dumpS());
			}
			for(int v2 : g.backEList[highdeg]) {
				ArrayList<Integer> alCycle = new ArrayList<>();
				alCycle.add(highdeg);
				do {
					alCycle.add(v2);
					v2 = g.backEList[v2].iterator().next();
				} while(v2 != highdeg);
				int[] cycle = alCycle.stream().mapToInt(i -> i).toArray();
				bigCycleList.add(cycle);
			}
			g.clear();
			return true;
		}
		if(numWithOutdegOver1 == 1) {
			int highdeg = -1;
			for(int i=0; i<g.N; i++) {
				int outDeg = g.outDeg[i];
				if(outDeg > 1) {
					highdeg = i; break;
				}
			}
			for(int v2 : g.eList[highdeg]) {
				ArrayList<Integer> alCycle = new ArrayList<>();
				alCycle.add(highdeg);
				do {
					alCycle.add(v2);
					v2 = g.eList[v2].iterator().next();
				} while(v2 != highdeg);
				int[] cycle = alCycle.stream().mapToInt(i -> i).toArray();
				bigCycleList.add(cycle);
			}
			g.clear();
			return true;
		}
		return false;
	}

}
