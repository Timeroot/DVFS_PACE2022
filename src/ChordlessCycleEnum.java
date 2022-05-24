import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

//Enumerate the chordless cycles in a given graph.
public class ChordlessCycleEnum {
	//Slow, simple method. Fast on small graphs, but
	//O(n!) time on big dense graphs. Simple implementation means I'm more confident in correctness.
	private static long searchStart, nodes;
	public static ArrayList<int[]> enumTreeSimple(Graph g){
		int N = g.N;
		int[] blocks = new int[N];
		ArrayDeque<Integer> path = new ArrayDeque<Integer>();
		ArrayList<int[]> resList = new ArrayList<>();

		searchStart = System.currentTimeMillis();
		nodes = 0;
		for (int i = 0; i < N; i++) {
			enumTreeSimple_help(g, i, path, resList, true, blocks);
			if(nodes > 1000000)
				return null;
			blocks[i]++; //block that vertex off from future exploration
		}
		return resList;
	}
	
	private static void enumTreeSimple_help(Graph g, int v,
			ArrayDeque<Integer> path, ArrayList<int[]> resList, boolean first, int[] blocks) {

		nodes++;
		if(nodes > 1000000)
			return;
		
//		System.out.println("Enter "+v+", "+path+", "+first+", "+Arrays.toString(blocks));
		if(first) {
			blocks[v] = -3*g.N;
		} else {
//			System.out.println(" "+v+" ?= "+path.getFirst());
			if (v == path.getFirst()) {
				//cycle found
				resList.add(path.stream().mapToInt(i -> i).toArray());
//				if(resList.size() % 1000 == 0)
//					System.out.println("Found "+resList.size()+" cycles so far (t="+(System.currentTimeMillis()-searchStart)+"ms)");
				return;
			}
			
			blocks[v]++;
			for (Integer c : g.backEList[v]) {
				blocks[c]++;
			}
		}
		//mark the children as blocked so we don't visit them through a roundabout way
		for (Integer c : g.eList[v]) {
			blocks[c]++;
		}
		
		path.addLast(v);

		//if the start is in the neighbors, must go straight there
		if(g.eList[v].contains(path.getFirst())) {
			enumTreeSimple_help(g, path.getFirst(), path, resList, false, blocks);
		} else {
			for (Integer c : g.eList[v]) {
				if(blocks[c] > 1)//1 because this is a child. Anything else is bad news
					continue;
				enumTreeSimple_help(g, c, path, resList, false, blocks);
				if(nodes > 1000000)
					return;
			}
		}

		if(first) {
			blocks[v] = 0;//reset the 3*N
		} else {
			blocks[v]--;
			for (Integer c : g.backEList[v]) {
				blocks[c]--;
			}
		}
		for (Integer c : g.eList[v]) {
			blocks[c]--;
		}
		
		path.pollLast();
//		System.out.println("Exit "+v);
		return;
	}

	///////////////////////////////////
	//SLOW algorithm that enumerates through explicitly all 2^n checks.
	static final int BRUTE_LIMIT = 22;
	public static ArrayList<int[]> bruteForceCycleEnumerate(Graph g) {
		int N = g.N;
		
		if(g.N > BRUTE_LIMIT) {
			return null;
		}
		ArrayList<int[]> resList = new ArrayList<>();

		///////////////////////////////////
		//Is the bitfield i enough that their removal makes it acyclic?
		boolean[] isSufficient = new boolean[1<<N];
		for(int flags = (1<<N); flags-->0; ) {
			boolean areAllParentSuff = true;
			//check if all parents (this + one more) are sufficient, first
			for(int parentI = 0; parentI < N; parentI++) {
				int parent = flags | (1<<parentI);
				if(parent == flags)
					continue;//already contained in flags
				if(!isSufficient[parent]) {
					areAllParentSuff = false;
					break;
				}
			}
			if(!areAllParentSuff) {
				//isSufficient[flags] = false;
				continue;
			}
			boolean isFSuff = isDVFS(g, flags, N);
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
						cycle[arrDest++] = i;
					}
				}
				resList.add(cycle);
			}
		}
		return resList;
	}
	
	private static boolean isDVFS(Graph g_orig, int flags, int N) {
		Graph g = g_orig.copy();
		for(int i=0; i<N; i++) {
			if((flags & (1<<i)) != 0)
				g.clearVertex(i);
		}
		return !g.hasCycle(); 
	}
	
	///////////////////////////////////
	//Implementation from https://rbisdorff.github.io/documents/chordlessCircuits.pdf
	// which enumerates circuits, but we skip some to have only cycles.
	public static ArrayList<int[]> enumCycles_bisdorff(Graph g){
		int N = g.N;
		ArrayDeque<Integer> path = new ArrayDeque<Integer>();
		ArrayList<int[]> resList = new ArrayList<>();
		
		@SuppressWarnings("unchecked")
		HashSet<Integer>[] visitedLEdges = new HashSet[N];
		for (int i = 0; i < N; i++) {
			visitedLEdges[i] = new HashSet<Integer>();
		}
		
		searchStart = System.currentTimeMillis();
		for (int i = 0; i < N; i++) {
			path.clear();
			path.add(i);
			bisdorff_helper(g, path, i, resList, visitedLEdges);
		}
		//bisdorff doesn't get K2's
		for(int v1=0; v1<N; v1++) {
			for(int v2 : g.eList[v1]) {
				if(v2 < v1)
					continue;
				if(g.eList[v2].contains(v1))
					resList.add(new int[] {v1,v2});
			}
		}
		return resList;
	}
	
	//what bisdorff calls chordlessCircuit(P, vk)
	private static void bisdorff_helper(Graph g, ArrayDeque<Integer> path,
			int vk, ArrayList<int[]> resList, HashSet<Integer>[] visitedLEdges){
		int vPrev = path.getLast();
		visitedLEdges[vPrev].add(vk);
		
		if(g.eList[vPrev].contains(vk)) {
			resList.add(path.stream().mapToInt(i -> i).toArray());
			if(resList.size() % 1000 == 0)
				System.out.println("Found "+resList.size()+" cycles so far (t="+(System.currentTimeMillis()-searchStart)+"ms)");
			return;
		} else {
			for(int v : g.eList[vPrev]) {
				//strict neigborhood only
				if(g.backEList[vPrev].contains(v))
					continue;
				if(!visitedLEdges[vPrev].contains(v)) {
					boolean noChord = true;
					int PcurrentSize = path.size();
					for(int x : path) {
						if(x == vPrev)
							break;
						if(x == vk) {
							 if(g.eList[x].contains(v)) {
								 noChord = false;
							 }
						} else {
							if(g.eList[x].contains(v) || g.eList[v].contains(x)) {
								noChord = false;
							}
						}
					}
					if(noChord) {
						path.addLast(v);
						bisdorff_helper(g, path, vk, resList, visitedLEdges);
						//pop path back to down to its original content
						while(path.size() > PcurrentSize)
							path.removeLast();
					}
				}
			}
		}
	}
	
	//An algorithm that makes sense to me and is simple:
	
	///////////////////////////////////
	//Try different ones and check that they agree
	private static final boolean trySimple = true;
	private static final boolean tryBrute = false;
	private static final boolean tryBisdorff = false;
	public static void enumCycles(Graph g, ArrayList<int[]> k2s) {
		if(k2s != null) {
			System.out.println("Re-including "+k2s.size()+" k2s");
			for(int[] pair : k2s) {
				int v1 = pair[0], v2 = pair[1];
				g.addEdge(v1, v2);
				g.addEdge(v2, v1);
			}
		}
		
		long startT, endT;
		ArrayList<int[]> res;
		int size;
		long hash;
		
		if(tryBisdorff) {
			startT = System.currentTimeMillis();
			res = enumCycles_bisdorff(g);
			size = res.size();
			hash = hashCycs(res);
			endT = System.currentTimeMillis();
			System.out.println("enumCycles_bisdorff: "+(endT-startT)+"ms, size = "+size);
	//		for(int[] cyc : res)
	//			System.out.println(Arrays.toString(cyc));
		}
		
		if(tryBrute) {
			startT = System.currentTimeMillis();
			res = bruteForceCycleEnumerate(g);
			endT = System.currentTimeMillis();
			if(res != null) {
				System.out.println("bruteForceCycleEnumerate: "+(endT-startT)+"ms, size = "+res.size());
	//			for(int[] cyc : res)
	//				System.out.println(Arrays.toString(cyc));
				if(hashCycs(res) != hash) {
					throw new RuntimeException();
				}
			} else
				System.out.println("bruteForceCycleEnumerate skipped.");
		}

		if(trySimple) {
			startT = System.currentTimeMillis();
			res = enumTreeSimple(g);
			endT = System.currentTimeMillis();
			System.out.println("enumTreeSimple: "+(endT-startT)+"ms, size = "+res.size());
	//		for(int[] cyc : res)
	//			System.out.println(Arrays.toString(cyc));
//			if(hashCycs(res) != hash) {
//				throw new RuntimeException();
//			}
		}

		if(k2s != null) {
			System.out.println("Re-removing "+k2s.size()+" k2s");
			for(int[] pair : k2s) {
				int v1 = pair[0], v2 = pair[1];
				g.clearEdge(v1, v2);
				g.clearEdge(v2, v1);
			}
		}
	}
	
	private static long hashCycs(ArrayList<int[]> cycList) {
		int res = 0;
		for(int[] cyc : cycList) {
			long cycHash = cyc.length;
			for(int v : cyc) {
				long aL = v;
				cycHash ^= 0x123456789ABCDEFL * (1+aL) * (10+aL) * (100000+aL);
			}
			cycHash = (cycHash * (cycHash + 0x1239)) ^ (cycHash * 0xf7e7d7c7b7a79787L);
			res += cycHash;
		}
		return res;
	}
	
	//Testing method
	public static void main(String[] args) {
		int N = 10;
		float density = 0.1f;
		while(true) {
			Graph g = randGraph(N, density);
			System.out.println("N="+N+", E="+g.E());
			try{
				enumCycles(g, null);
			} catch(RuntimeException re) {
				g.dump();
				throw re;
			}
			density += 0.1f;
			if(density > 0.9f) {
				density = 0.1f;
				N++;
			}
		}
	}
	
	static Random rand = new Random(1235);
	private static Graph randGraph(int N, float density) {
		HashSet<Integer>[] eList = new HashSet[N];
		HashSet<Integer>[] backEList = new HashSet[N];
		for(int i=0; i<N; i++) {
			eList[i] = new HashSet<>();
			backEList[i] = new HashSet<>();
		}
		Graph g = new Graph(N, eList, backEList);
		//give each vertex indeg and outdeg at least 1
		for(int i=0; i<N; i++) {
			int dest;
			do {
				dest = (1+i+rand.nextInt(N-1))%N;
			} while(g.eList[i].contains(dest));
			g.addEdge(i, dest);
			
			do {
				dest = (1+i+rand.nextInt(N-1))%N;
			} while(g.backEList[i].contains(dest));
			g.addEdge(dest, i);
		}
		//add some more edges
		int moreEdges = (int)(N*(N-1)/2 * density) - 2*N;
		for(int e=0; e<moreEdges; e++) {
			int start, end;
			do{
				start = rand.nextInt(N);
				end = rand.nextInt(N);
			} while(start == end || g.eList[start].contains(end));
			g.addEdge(start, end);
		}
		//all done
		return g;
	}
}
