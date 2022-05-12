import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

import JNA_SCIP.SCIP_VAR;

//Tries to reduce the problem to a minimum cover problem and solve that.
//Returns null if it fails to find a solution (like all Solvers).
//If (after all reductions) the graph consists of just K2s, the problem
//still has the nice property that it can be completely solved via matching.
//With a few K3s or Kn's it can still be fast.
public class MinimumCoverSolver implements Solver {

	ArrayList<int[]> pairList;
	ArrayList<int[]> bigCycleList;
	int N;
	SCIP_VAR[] vars;
	
	static final boolean ECHO = false;
	static final int MAX_SCC_SIZE = 100;
	
	public MinimumCoverSolver() {
	}

	@Override
	public ArrayList<Integer> solve(Graph g) {
		if(Main_Load.TESTING)
			System.out.println("Solving with IndependentSetSolver");
		pairList = new ArrayList<int[]>();
		bigCycleList = new ArrayList<int[]>();
		N = g.N;
		
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
//		int d0_stripped = 0;
//		if(rem_E > 0) {
//			d0_stripped = stripDegZero(g);
//			if(d0_stripped > 0)
//				throw new RuntimeException("WOAH how did that happen?");
//			rem_E = g.E();
//			if(Main_Load.TESTING)
//				System.out.println("D0stripped "+d0_stripped+", now E="+rem_E);
//		}
//		while(rem_E > 0 && d0_stripped > 0) {
//			
//			if(Main_Load.TESTING)
//				System.out.println("Another round of SCCstripping");
//			
//			scc_stripped = stripSCC(g);
//			rem_E = g.E();
//			if(Main_Load.TESTING)
//				System.out.println("After SCC check, E="+rem_E);
//			
//			if(rem_E == 0 || scc_stripped == 0)
//				break;
//			
//			if(Main_Load.TESTING)
//				System.out.println("Another round of D0stripping");
//			d0_stripped = stripDegZero(g);
//			rem_E = g.E();
//			if(Main_Load.TESTING)
//				System.out.println("D0stripped "+d0_stripped+", now E="+rem_E);
//		}

		if(Main_Load.TESTING)
			System.out.println("Stripping complete");
		
		//Is it all cycles?
		boolean isAllCycles = isAllCycles(g);
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
			System.out.println("Skipmerged = "+skipMerged);
			isAllCycles = isAllCycles(g);
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
				System.out.println("skipMergeLong! is "+skipMergeLong);
				isAllCycles = isAllCycles(g);
				System.out.println("isAllCycles3 == "+isAllCycles);
				
				killSimpleCycles(g);
				
				rem_E = g.E();
				if(isAllCycles != (rem_E == 0))
					throw new RuntimeException("I don't understand what a cycle is [3]");
			}
		}

		g.checkConsistency();
		rem_E = g.E();

		if(rem_E > 0) {
			if(Main_Load.TESTING)
				System.out.println("Start processSCC");
			int proSCCret = processSCC(g);
			rem_E = g.E();
			
			isAllCycles = (rem_E == 0);
			if(Main_Load.TESTING) {
				System.out.println("ProcessSCC "+(isAllCycles?"worked":"failed"));
				if(proSCCret == 0) {
					System.out.println("Couldn't split");
					g.dump();
				}
			}
		}
		
		if(isAllCycles) {
			if(bigCycleList.size() == 0)
				System.out.println("EASY: Vertex cover problem");
			else {
				System.out.println("MEDIUM: Minimum cover.");
				int maxCycleSize = 0;
				for(int[] cycle : bigCycleList)
					maxCycleSize = Math.max(maxCycleSize, cycle.length);
				System.out.println(maxCycleSize+" many bigcycles, maximum size "+maxCycleSize);
			}
		} else {
			System.out.println("HARD: Not minimum cover!");
//			g.dump();
		}
		//check e_019 for improved skipMerge
		
		//rule: if only one vertex with indeg>1, or only one with outdeg>1, can use all cycles through that
		
		//Check if it is purely cycles at this point
//		System.exit(1);
		return null;
	}
	
	//does killSimpleCycles, fixing the indexing for chunks.
	int killSimpleCycles(GraphChunk g) {
		int res = killSimpleCycles(g.gInner);
		for(int cycI = 0; cycI < res; cycI++) {
			int savedCycI = bigCycleList.size()-1-cycI;
			int[] cycle = bigCycleList.get(savedCycI);
			for(int i=0; i<cycle.length; i++) {
				cycle[i] = g.mapping[cycle[i]];
			}
		}
		return res;
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
					//TODO add to bigCycleList
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
		for(int i=0; i<N; i++) {
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

	//Cuts into SCCs and processes each component separately
	int processSCC(Graph g) {
		ReducedGraph rg = ReducedGraph.wrap(g, true);
		SCC scc = new SCC();
		boolean sccSplit = scc.doSCC(rg);
		if(!sccSplit) {
			//alright, it didn't split. Still make the "chunk" and process
			GraphChunk chunk = GraphChunk.wrap(g);
			processChunk(chunk);
			if(chunk.gInner.E() != 0) {
				chunk.addInto(g);
//				chunk.gInner.dump();
			}			
			return 0;
		}
//		ArrayList<GraphChunk> chunkies = new ArrayList<GraphChunk>();
		for(ArrayList<Integer> sccComp : scc.sccInfo) {
			if(sccComp.size() > MAX_SCC_SIZE) {
				continue;
			}
			GraphChunk chunk = new GraphChunk(g, sccComp, true);
			processChunk(chunk);
			if(chunk.gInner.E() != 0) {
				chunk.addInto(g);
				chunk.gInner.dump();
			}
		}
//		g.dump();
		return 1;
	}
	
	void processChunk(GraphChunk chunk) {
//		chunk.gInner.dump();
//		System.out.println("Chunky chunk "+Arrays.toString(chunk.mapping));
		while(skipMergeLong(chunk.gInner) > 0) {
			if(Main_Load.TESTING)
				System.out.println("Another skipMerge was useful...");
			chunk.gInner.checkConsistency();
		}
		int killed = killSimpleCycles(chunk);
		if(chunk.gInner.E() == 0)
			return;
		if(checkSingleMultivert(chunk))
			return;
		splitWAP(chunk);
	}
	
	//splits on weak articulation points
	void splitWAP(GraphChunk chunk) {
		int wap = WeakArticulationPoints.AP_One(chunk.gInner);
//		if(Main_Load.TESTING)
//			System.out.println("findAP = "+findAP);
		if(wap == -1) {
			if(Main_Load.TESTING)
				System.out.println("Couldn't split");
			bruteForceCycleEnumerate(chunk);
			return;//couldn't split
		}
		if(Main_Load.TESTING)
			System.out.println("Split at "+wap+" on: ");
		chunk.gInner.dump();

		//Now we have a WAP, split into connected components.
		//each one gets the WAP itself, too.
		int N = chunk.gInner.N;
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
			queue.addAll(chunk.gInner.eList[v]);
			queue.addAll(chunk.gInner.backEList[v]);
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
			GraphChunk gc = new GraphChunk(chunk.gInner, comp, true);
//			System.out.println("Subgraph is ");
			processChunk(gc);
			if(gc.gInner.E() != 0) {
//				if(Main_Load.TESTING)
//					System.out.println("Didn't complete");
				gc.addInto(chunk.gInner);
				//TODO instead of adding chunks back in (and agglutinating over WAPs)
				// keep them separate for other processing, maybe?
			}
		}
//		System.out.println("After WAP is ");
//		chunk.gInner.dump();
		if(chunk.gInner.E() == 0)
			System.out.println("Splitting SOLVED this chunk");
	}
	
	//Idea here is that for small chunks (< 10 vertices?) we can find a
	//minimal set of cycles by brute force.
	void bruteForceCycleEnumerate(GraphChunk gc) {
		//TODO
	}
	
	//Checks if the graph has a single vertex with indeg>1 (or a single with outdeg>1).
	//In this case, you can describe the graph just by the cycles from each those options,
	//pretty easily. Returns "true" if this worked and the graph was resolved.
	boolean checkSingleMultivert(GraphChunk chunk) {
		Graph g = chunk.gInner;
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
				throw new RuntimeException("What?");
			if(inDeg != 0 && outDeg == 0)
				throw new RuntimeException("What?");
		}
		if(numWithIndegOver1 == 1) {
			int highdeg = -1;
			for(int i=0; i<g.N; i++) {
				int inDeg = g.inDeg[i];
				if(inDeg > 1) {
					highdeg = i; break;
				}
			}
			for(int v2 : g.backEList[highdeg]) {
				ArrayList<Integer> alCycle = new ArrayList<>();
				alCycle.add(chunk.mapping[highdeg]);
				do {
					alCycle.add(chunk.mapping[v2]);
					v2 = g.backEList[v2].iterator().next();
				} while(v2 != highdeg);
				int[] cycle = alCycle.stream().mapToInt(i -> i).toArray();
				bigCycleList.add(cycle);
			}
			chunk.gInner.clear();
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
				alCycle.add(chunk.mapping[highdeg]);
				do {
					alCycle.add(chunk.mapping[v2]);
					v2 = g.eList[v2].iterator().next();
				} while(v2 != highdeg);
				int[] cycle = alCycle.stream().mapToInt(i -> i).toArray();
				bigCycleList.add(cycle);
			}
			chunk.gInner.clear();
			return true;
		}
		return false;
	}
	
	//Captures a small subgraph, together with data about its inclusion in the original. 
	static class GraphChunk {
		static int biggestChunk = -1;
		
		int[] mapping;
		Graph gInner;
		
		@SuppressWarnings("unchecked")
		public GraphChunk(Graph g, ArrayList<Integer> verts, boolean destructive) {
			verts.sort(Integer::compare);
			
			int N = verts.size();
			mapping = new int[N];
			for(int i=0; i<N; i++)
				mapping[i] = verts.get(i);
			HashSet<Integer>[] eList = new HashSet[N];
			HashSet<Integer>[] backEList = new HashSet[N];
			for(int i=0; i<N; i++) {
				eList[i] = new HashSet<Integer>();
				backEList[i] = new HashSet<Integer>();
			}

			for(int i=0; i<N; i++) {
				HashSet<Integer> eL = eList[i];
				int iOrig = mapping[i];
				
				for(Iterator<Integer> jOrigIter = g.eList[iOrig].iterator();
						jOrigIter.hasNext();
						) {
					int jOrig = jOrigIter.next();
					
					int j = Arrays.binarySearch(mapping, jOrig);
					if(j < 0)
						continue;//not in subgraph
					eL.add(j);
					backEList[j].add(i);

					if(destructive) {
//						g.eList[iOrig].remove(jOrig);
						jOrigIter.remove();
						
						g.outDeg[iOrig]--;
						g.backEList[jOrig].remove(iOrig);
						g.inDeg[jOrig]--;
					}
				}
			}
			
			gInner = new Graph(N, eList, backEList);
			g.checkConsistency();
			gInner.checkConsistency();
			biggestChunk = Math.max(biggestChunk, N);
//			if(Main_Load.TESTING)
//				System.out.println("Made chunk of size "+N+", record "+biggestChunk);
		}
		
		private GraphChunk(Graph g) {
			this.gInner = g;
			this.mapping = new int[g.N];
			for(int i=0; i<g.N; i++) {
				mapping[i] = i;
			}
		}
		public static GraphChunk wrap(Graph g) {
			return new GraphChunk(g);
		}
		
		//adds edges in this subgraph back into parent graph
		void addInto(Graph g) {
			for(int i=0; i<gInner.N; i++) {
				int iOrig = mapping[i];
				
				for(int j : gInner.eList[i]) {
					int jOrig = mapping[j];
					g.eList[iOrig].add(jOrig);
				}
				g.outDeg[iOrig] = g.eList[iOrig].size();
				
				for(int j : gInner.backEList[i]) {
					int jOrig = mapping[j];
					g.backEList[iOrig].add(jOrig);
				}
				g.inDeg[iOrig] = g.backEList[iOrig].size();
			}
		}

		
		//copies this induced subgraph back into parent graph
		void copyInto(Graph g) {
			//Clear out old edges
			for(int i=0; i<gInner.N; i++) {
				int iOrig = mapping[i];				
				for(int jOrig : g.eList[iOrig]) {
					if(Arrays.binarySearch(mapping, jOrig) >= 0) {
						//responsibility of this induced subgraph
						g.eList[iOrig].remove(jOrig);
					}
				}
				for(int jOrig : g.backEList[iOrig]) {
					if(Arrays.binarySearch(mapping, jOrig) >= 0) {
						//responsibility of this induced subgraph
						g.backEList[iOrig].remove(jOrig);
					}
				}
			}
			addInto(g);
		}
	}

	@Override
	public ArrayList<Integer> solve(ReducedGraph g) {
		throw new RuntimeException("Not implemented");
	}
}
