import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

public class VertexCover {
	//tries to solve vertex cover problems using some branching rules,
	//especially those described at https://epubs.siam.org/doi/pdf/10.1137/1.9781611976472.13
	// and https://www.sciencedirect.com/science/article/pii/S0304397512008729

	static int N;
	static ArrayList<Integer>[] neighbors;
	
	//this data saves things needed to save the reduction.
	//degree 2 foldings. For a deg-2 pattern a-b-c, this stores {0, b,a,c}.
	//for a funnel u-v-{N(v)-u}, this saves {1, u,v, ...N(v)\N[u]... }.
	static ArrayList<int[]> deg2Folds_and_funnels;
	static ArrayList<Integer> includes;//forced inclusions from e.g. domination, deg-1..
	
	static final boolean CHECK_TWINS = false;
	static final boolean VERIFY = true;
	static final boolean LOG = false;
	
	public static ArrayList<Integer> solve(ArrayList<int[]> _edges, int _N) {
		//reduction data for rebuilding solution
		deg2Folds_and_funnels = new ArrayList<int[]>();
		includes = new ArrayList<>();

		N = _N;
		neighbors = new ArrayList[N];
		
		for(int v=0; v<N; v++)
			neighbors[v] = new ArrayList<>();
		
		for(int[] edge : _edges) {
			int a = edge[0], b = edge[1];
			neighbors[a].add(b);
			neighbors[b].add(a);
		}
		
		if(Main_Load.TESTING){
			System.out.println("Initial VertexCover problem: N = "+N);
			System.out.println(numNonz()+" nonempty vertices");
		}
		
//		int minDeg = N+1;
//		for(int v=0; v<N; v++)
//			minDeg = Math.max(minDeg, neighbors[v].size());
		
		boolean progress = true;
		while(progress) {
			checkConsistency();
			progress = false;
			for(int v=0; v<N; v++) {
				int deg = neighbors[v].size();
				//deg 0 --> it's not even there
				if(deg == 0)
					continue;
				//prune degree 1
				if(deg == 1) {
					int other = neighbors[v].get(0);
					//remove the deg-1
					neighbors[v].clear();
					neighbors[other].remove((Integer)other);
					//include the other
					includes.add(other);
					clearVertex(other);
					if(LOG) System.out.println("Remove degone "+v+"-"+other);
					progress = true;
					continue;
				}
				//prune degree 2
				if(deg == 2) {
					int oA = neighbors[v].get(0);
					int oB = neighbors[v].get(1);
					//if oA and oB are neighbors, do a folding.
					//if they're not, v is simplicial.
					if(neighbors[oA].contains(oB)) {
						//simplicial. drop v
						neighbors[v].clear();
						neighbors[oA].remove((Integer)v);
						neighbors[oB].remove((Integer)v);
						//include the rest of the clique
						includes.add(oA);
						clearVertex(oA);
						includes.add(oB);
						clearVertex(oB);
						//we progressed
						if(LOG) System.out.println("Remove simplicial degtwo {"+v+","+oA+","+oB+"}");
						progress = true;
						continue;
					} else {
						//remove v from both
						neighbors[v].clear();
						neighbors[oA].remove((Integer)v);
						neighbors[oB].remove((Integer)v);
						//add all of oA's to oB's
						TreeSet<Integer> merge = new TreeSet<>(neighbors[oB]);
						for(int v2 : neighbors[oA]) {
							neighbors[v2].remove((Integer)oA);
							if(!merge.contains(v2))
								neighbors[v2].add(oB);
						}
						merge.addAll(neighbors[oA]);
						neighbors[oB] = new ArrayList<>(merge);
						neighbors[oA].clear();
						//save folding info
						int[] foldData = new int[4];
						foldData[0] = 0;//type=deg2fold
						foldData[1] = v;
						foldData[2] = oA;
						foldData[3] = oB;
						deg2Folds_and_funnels.add(foldData);
						//we progressed
						if(LOG) System.out.println("Fold degtwo {"+v+","+oA+","+oB+"}");
						progress = true;
						continue;
					}
				}
				//check for simplicial. If the neighborhood is a clique, delete v
				{
					boolean isSimplicial = true;
					ArrayList<Integer> Nv = neighbors[v];
					for(int n1 : Nv) {
						for(int n2 : Nv) {
							if(n1 == n2)//doesn't need to neighbor itself
								continue;
							if(n2 < n1)//only need to check it one way
								continue;
							if(neighbors[n1].contains(n2))
								continue;
							//nope.
							isSimplicial = false;
							break;
						}
						if(!isSimplicial)
							break;
					}
					if(isSimplicial) {
						//include surrounding clique
						for(int n : new ArrayList<>(Nv)) {//make a copy of Nv bc concurrent modification
							includes.add(n);
							clearVertex(n);
						}
						//remove v itself
						clearVertex(v);
						//we progressed
						if(LOG) System.out.println("Delete simplicial of degree "+deg+" ("+v+")");
						progress = true;
						continue;
					}
				}
				//check for vertices we dominate
				{
					ArrayList<Integer> Nv = neighbors[v];
					TreeSet<Integer> Nv_ts = new TreeSet<>(Nv);
					boolean v_dominates = false;
					
					for(int n1 : Nv) {
						int deg2 = neighbors[n1].size();
						if(deg2 >= deg)//n1 can't dominate v if it has at least many other neighbors
							continue;
						boolean n1_isDominated = true;
						for(int n2 : neighbors[n1]) {
							if(n2 == v)
								continue;
							if(Nv_ts.contains(n2))
								continue;
							n1_isDominated = false;
							break;
						}
						if(!n1_isDominated)
							continue;
						v_dominates = true;
						break;
					}
					if(v_dominates) {
						//v must be in an optimum solution
						includes.add(v);
						clearVertex(v);
						if(LOG) System.out.println("Include dominating vertex of deg "+deg+" ("+v+")");
						progress = true;
						continue;
					}
				}
				//check for 3-funnel
				//TODO k-funnels, desks?
				{
					if(deg == 3) {
						int oA = neighbors[v].get(0);
						int oB = neighbors[v].get(1);
						int oC = neighbors[v].get(2);
						int u = -1;
						if(neighbors[oA].contains(oB)) {
							u = oC;
						} else if(neighbors[oB].contains(oC)) {
							u = oA;
						} else if(neighbors[oA].contains(oC)) {
							u = oB;
						}
						if(u != -1) {
							//a funnel exists, oC - v - {oA, oB}
							HashSet<Integer> Nv = new HashSet<Integer>(neighbors[v]);
							HashSet<Integer> Nu = new HashSet<Integer>(neighbors[u]);
							//differences in neighborhoods
							HashSet<Integer> Nv_sub_Nu = new HashSet<Integer>(Nv);
							Nv_sub_Nu.removeAll(Nu);
							//other difference
							HashSet<Integer> Nu_sub_Nv = new HashSet<Integer>(Nu);
							Nu_sub_Nv.removeAll(Nv);
							//intersection of neighborhoods
							HashSet<Integer> Nu_cap_Nv = new HashSet<Integer>(Nu);
							Nu_cap_Nv.removeAll(Nu_sub_Nv);
							//okay, data is ready, do the operations.
							//remove u and v from the graph
							clearVertex(u);
							clearVertex(v);
							Nv_sub_Nu.remove(u);
							Nu_sub_Nv.remove(v);
							//add Nu cap Nv to the vertex cover
							includes.addAll(Nu_cap_Nv);
							for(int cap : Nu_cap_Nv)
								clearVertex(cap);
							//connect all the differences in neighborhood together
							for(int x : Nv_sub_Nu) {
								for(int y : Nu_sub_Nv) {
									if(neighbors[x].contains(y))
										continue;
									neighbors[x].add(y);
									neighbors[y].add(x);
								}
							}
							//save the data for reconstructing solution later
							int[] funnelData = new int[3 + Nv_sub_Nu.size()];
							funnelData[0] = 1;//type=funnel
							funnelData[1] = u;
							funnelData[2] = v;
							{int dest = 3;
							for(int x : Nv_sub_Nu)
								funnelData[dest++] = x;//all of N(v)\N(u)
							}
							deg2Folds_and_funnels.add(funnelData);
							//we progressed
							if(LOG) System.out.println("Funnel found at "+v+" with "+u+" special");
							progress = true;
							continue;
						}
					}
				}
			}
			if(LOG) System.out.println("End pass");
		}
		
		//Check for twins
		if(CHECK_TWINS){
//			long[] hashes = new long[numNonz()];
//			int dest = 0;
			HashMap<Long, ArrayList<Integer>> hashes = new HashMap<>();
			for(int v=0; v<N; v++)
				if(neighbors[v].size() > 0) {
					long hash = hashInts(neighbors[v]);
					if(hashes.containsKey(hash)) {
						hashes.get(hash).add(v);
					} else {
						ArrayList<Integer> list = new ArrayList<>();
						list.add(v);
						hashes.put(hash, list);
					}
				}
			for(ArrayList<Integer> list : hashes.values()) {
				if(list.size() == 1)
					continue;
				System.out.println("Twins found in "+list);
				throw new RuntimeException();
			}
		}

		if(Main_Load.TESTING){
			System.out.println("Reductions complete.");
			System.out.println(numNonz()+" nonempty vertices");
		}
		
		boolean[] solution = solveCore();
		//add in the "includes"
		for(int v : includes)
			solution[v] = true;
		//go through the folds / funnels in reverse order to project back
		for(int projI=deg2Folds_and_funnels.size(); projI-->0; ) {
			int[] projData = deg2Folds_and_funnels.get(projI);
			int type = projData[0];
			if(type == 0) {//deg2 fold
				int v = projData[1];
				int oA = projData[2];
				int oB = projData[3];
				if(solution[oB]) {//u'==v' is in, include u too 
					solution[oA] = true;
				} else { //u'==v' isn't in, use v to cover
					solution[v] = true;
				}
			} else if(type == 1) {//funnel
				int u = projData[1];
				int v = projData[2];
				boolean hasAll_nv_sub_nu = true;
				for(int i=3; i<projData.length; i++) {
					if(!solution[projData[i]]) {
						hasAll_nv_sub_nu = false; break;
					}
				}
				if(hasAll_nv_sub_nu) {//ALL of N(v)\N(u) is in, include u
					solution[u] = true;
				} else {//ALL of N(u)\N(v) is in, include v
					solution[v] = true;
				}
			} else {
				throw new RuntimeException("Saved projData = "+Arrays.toString(projData)+", type not understood");
			}
		}
		
		verify(solution, _edges);
		
		return getTrues(solution);
//		System.exit(1);
//		return null;
	}
	
	static void verify(boolean[] solution, ArrayList<int[]> edgeList) {
		if(!VERIFY)
			return;
		for(int[] edge : edgeList) {
			int x = edge[0], y = edge[1];
			if(solution[x] || solution[y])
				continue;
			if(Main_Load.TESTING) {
					System.out.println("FAILURE");
					System.out.println("Dumping bad solution: {");
					for(int i=0; i<N; i++)
						if(solution[i])
							System.out.print(i+",");
					System.out.println("}");
			}
			throw new RuntimeException("Edge {"+x+","+y+"} unfulfilled!");
		}
		if(Main_Load.TESTING)
			System.out.println("Verified");
	}
	
	static void checkConsistency() {
		if(!Graph.CHECK)
			return;
		for(int v=0; v<N; v++) {
			for(int Nv : neighbors[v])
				if(!neighbors[v].contains(Nv))
					throw new RuntimeException(v+"->"+Nv+" but not vice versa");
		}
	}
	
	static ArrayList<Integer> getTrues(boolean[] bools){
		ArrayList<Integer> res = new ArrayList<>();
		for(int i=0; i<bools.length; i++)
			if(bools[i])
				res.add(i);
		return res;
	}
	
	//After reductions are all done, solve what's left the slow way
	static boolean[] solveCore() {
		if(numNonz() > 0) {
			ArrayList<int[]> pairList = makeK2List();
			MinimumCoverInfo mci = new MinimumCoverInfo(N, pairList, null, null, null);
			boolean[] res = new MinimumCover().solve(mci);
			return res;
//			throw new RuntimeException("We don't actually solve the core yet");
		} else {
			//nothing in the graph, return empty
			boolean[] res = new boolean[N];
			return res;
		}
	}
	
	static ArrayList<int[]> makeK2List() {
		ArrayList<int[]> res = new ArrayList<>();
		for(int i=0; i<N; i++)
			for(int j : neighbors[i])
				if(j > i)
					res.add(new int[]{i,j});
		return res;
	}
	
	static void clearVertex(int v) {
		for(int ov : neighbors[v])
			neighbors[ov].remove((Integer)v);
		neighbors[v].clear();
	}
	
	static int numNonz() {
		int degGTz = 0;
		for(int v=0; v<N; v++)
			if(neighbors[v].size() > 0)
				degGTz++;
		return degGTz;
	}
	
	//"hash" a set of integers for quick twin-checking
	static long hashInts(ArrayList<Integer> neighbors2) {
		long res = neighbors2.size();
		for(int a : neighbors2) {
			long aL = a;
			res ^= 0x123456789ABCDEFL * aL * (1+aL) * (100000+aL);
		}
		return res;
	}
	
	static void dump() {
		System.out.println("{");
		for(int i=0; i<N; i++)
			for(int j : neighbors[i])
				if(i<j)
					System.out.println("{"+i+","+j+"},");
		System.out.println("}");
	}
	
	//stores edges as Longs: 32bits for the first vertex, 32 for the second vertex.
//	
//	//get/set edges from the packed long format.
//	static int edgeA(long l) {
//		return (int) (l&0xFFFFFFFFL);
//	}
//	static int edgeB(long l) {
//		return (int) ((l>>32)&0xFFFFFFFFL);
//	}
//	static long toEdgeAB(long a, long b) {
//		return (a<<32) | b;
//	}
}
