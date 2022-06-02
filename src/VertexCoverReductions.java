import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;

public class VertexCoverReductions {
	//reduces pure vertex cover problems using some branching rules,
	//especially those described at https://epubs.siam.org/doi/pdf/10.1137/1.9781611976472.13
	// and https://www.sciencedirect.com/science/article/pii/S0304397512008729

	static int N;
	static ArrayList<Integer>[] neighbors;
	
	//this data saves things needed to save the reduction.
	//degree 2 foldings. For a deg-2 pattern a-b-c, this stores {0, b,a,c}.
	//for a funnel u-v-{N(v)-u}, this saves {1, u,v, ...N(v)\N[u]... }.
	static ArrayList<int[]> deg2Folds_and_funnels;
	static ArrayList<Integer> includes;//forced inclusions from e.g. domination, deg-1..
	
	static final boolean CHECK_KFUNNEL = true;
	static final boolean CHECK_TWINS = false;//only partially implemented
	static final boolean CHECK_CONFINE = true;
	static final boolean CHECK_DESKS = true;
	static final boolean CHECK_GEN_DESKS = false;//not implemented
	static final boolean VERIFY = true;
	static final boolean LOG = false && Main_Load.TESTING;
	
	@SuppressWarnings("unchecked")
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
			System.out.println("Initial VertexCover problem: N = "+numNonz());
		}
		
		boolean progress = true;
		while(progress) {
			checkConsistency();
			progress = false;
			vLoop: for(int v=0; v<N; v++) {
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
//					if(LOG) System.out.println("Remove degone "+v+"-"+other);
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
//						if(LOG) System.out.println("Remove simplicial degtwo {"+v+","+oA+","+oB+"}");
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
//						if(LOG) System.out.println("Fold degtwo {"+v+","+oA+","+oB+"}");
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
						if(deg2 > deg)//n1 can't be dominated by v if it has more other neighbors
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
//						if(LOG) System.out.println("Include dominating vertex of deg "+deg+" ("+v+")");
						includes.add(v);
						clearVertex(v);
						progress = true;
						continue;
					}
				}
				//3-funnel
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
						resolveFunnel(v, u);
						//we progressed
						if(LOG) System.out.println("Funnel found at "+v+" with "+u+" special");
						progress = true;
						continue;
					}
				}//end 3-funnel
				
				//TODO desks?
			}
			
			if(!progress) {
				if(LOG) System.out.println("Trying hard checks now");
			}
			
			//we failed to progress. So try slower checks this time, like:
			//larger funnels (than just deg 3)
			if(!progress) {
				progress = checkKfunnel();
			}
			//twins (nodes with identical neighborhoods)
			if(!progress) {
				progress = checkTwins();
			}
			//unconfined vertices
			if(!progress) {
				progress = checkConfinement();
			}
			//desks (chordless 4-cycle of deg-3 or deg-4 vertices)
			if(!progress) {
				progress = checkDesks();
			}
			//gen desks (chordless 4-cycle of deg-3 or deg-4 vertices)
			if(!progress) {
				progress = checkGenDesks();
			}
			
			if(LOG) System.out.println("End pass");
		}
		
		if(Main_Load.TESTING){
			System.out.println(numNonz()+" nonempty vertices in reduced VC");
//			if(numNonz() > 0) {
//				dump();
//				System.out.println("FLAGFLAG");
//			}
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
			} else if(type == 2) {//desk
				int a = projData[1];
				int b = projData[2];
				int c = projData[3];
				int d = projData[4];
				boolean hasAll_nv_sub_nu = true;
				for(int i=5; i<projData.length; i++) {
					if(!solution[projData[i]]) {
						hasAll_nv_sub_nu = false; break;
					}
				}
				if(hasAll_nv_sub_nu) {//ALL of N(v)\N(u) is in, include b+c
					solution[b] = true;
					solution[c] = true;
				} else {//ALL of N(u)\N(v) is in, include a+d
					solution[a] = true;
					solution[d] = true;
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
		if(Main_Load.VERBOSE) {
			int size = 0;
			for(int i=0; i<N; i++) if(solution[i]) size++;
			System.out.println("Verified solution of size "+size);
		}
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
			boolean[] res = new ILP_MinimumCover().solve(mci);
			return res;
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
	
	static int kFunnelV = -1;
	static int kFunnelNv = -1;
	
	private static boolean checkKfunnel() {
		if(!CHECK_KFUNNEL)
			return false;
		
		boolean progress = false;
		vLoop: for(int v=0; v<N; v++) {
			int deg = neighbors[v].size();
			if(deg <= 3) {
				//should already be checked
				continue;
			}
			//the degrees of each neighbor in the neighborhood N[v]
//			int[] Nv_deg = new int[deg];

			int totalDeg = 0;//how many edges total in the neighborhood (x2)
			//which vertex (as an index into neighbors[v]) has the degree < N-1?
			//is only one, because if we see two, then v doesn't have a k-funnel,
			//so we can just continue.
			int lowDegNvi = -1;
			int lowDegVal = -1;
			//in case there's just one edge missing, we use this one. It has deg == N-1.
			int lowishDegNvi = -1;
			
			for(int nvi=0; nvi<deg; nvi++) {
				int nv = neighbors[v].get(nvi);
				int nv_deg = 0;
				for(int nnv : neighbors[nv]) {
					if(neighbors[v].contains(nnv)) {
						nv_deg++;
						totalDeg++;
//						System.out.println("+1: "+v+" -> "+nv+" -> "+nnv);
					}
				}
				if(nv_deg < deg-2) {
					//this is missing at least two neighbors, if there is a funnel, it must be this one.
					if(lowDegNvi >= 0) {
						//there was already one! Since both must be, there can't be a funnel.
//						if(LOG) System.out.println("kFunnel: "+v+" had both "+nv+" and "+neighbors[v].get(lowDegNvi)+" as too-low neighbors, so skip.");
						continue vLoop;
					} else {
						lowDegNvi = nvi;
						lowDegVal = nv_deg;
					}
				} else if(nv_deg < deg-1) {
					lowishDegNvi = nvi;//can have multiple. Just take the most recent, whatever.
				}
			}
//			if(LOG) System.out.println("kFunnel: "+v+" has "+totalDeg+", "+lowDegNvi+","+lowDegVal+", "+lowishDegNvi);
			if(lowDegNvi >= 0) {
				//only one too-low was found. Check that the number of missing edges is correct.
				int expectedTotalDeg = (deg*(deg-1)) - 2*(deg-1 - lowDegVal);
//				if(LOG) System.out.println("Expected = "+expectedTotalDeg+", Actual = "+totalDeg);
				if(totalDeg == expectedTotalDeg) {
					//Great!
					int nv = neighbors[v].get(lowDegNvi);
					if(LOG) System.out.println("k-funnel at "+v+" and "+nv);
					resolveFunnel(v, nv);
					progress = true;
					continue vLoop;
//					return true;
				} else {
//					if(LOG) System.out.println("Degree counts don't match, continue.");
					continue vLoop;
				}
			} else {
				if(totalDeg == deg*(deg-1)) {
					//This neighborhood graph is complete, this is simplicial! Clearly we're making enough progress
					//we should just go back out and do the cheap checks for a bit.
					return progress;
				} else if(totalDeg != deg*(deg-1) - 2) {
//					if(LOG) System.out.println("Missing "+(deg*(deg-1)-totalDeg)+" means several pairs missing, continue.");
					continue vLoop;
				} else {
					//great, there's exactly one edge missing, which means that it's a funnel with either one.
					int nv = neighbors[v].get(lowishDegNvi);
					if(LOG) System.out.println("k-funnel at "+v+" and "+nv);
					resolveFunnel(v, nv);
					progress = true;
					continue vLoop;
//					return true;
				}
			}
		}
		return progress;
	}
	
	//check for "desks"
	private static boolean checkDesks() {
		if(!CHECK_DESKS)
			return false;
		
		//desk is a chordless 4-cycle of degree-3 or degree-4 vertices, etc.
		boolean progress = false;
		vLoop: for(int a=0; a<N; a++) {
			int degA = neighbors[a].size();
			if(degA < 3 || degA > 4)
				continue;//not appropriate degree
			//consider each pair of neighbors (b,c) to a,
			//that are both degrees 3 and 4.
			//Can restrict to a < b < c.
			for(int b : neighbors[a]) {
				if(b <= a)
					continue;//can order better
				int degB = neighbors[b].size();
				if(degB < 3 || degB > 4)
					continue;//not appropriate degree
				
				for(int c : neighbors[a]) {
					if(c <= b)
						continue;//can order better
					int degC = neighbors[c].size();
					if(degC < 3 || degC > 4)
						continue;//not appropriate degree
					if(neighbors[b].contains(c))
						continue;//must be chordless
					
					//find a vertex d in N(b) and N(c), but not N(a)
					//TODO: choose whether to loop over N(b) or N(c) based
					// on which is smaller.
					//by permutation, we can assume d > a
					for(int d : neighbors[b]) {
						if(d <= a)
							continue;
						int degD = neighbors[d].size();
						if(degD < 3 || degD > 4)
							continue;//not appropriate degree
						if(neighbors[a].contains(d))
							continue;//chordless
						if(!neighbors[c].contains(d))
							continue;//need d to be in N(c)
						
						if(LOG) System.out.println("Desk? "+a+"-"+b+"-"+c+"-"+d);
						
						//External neighborhoods
						HashSet<Integer> Nad = new HashSet<Integer>(neighbors[a]);
						Nad.addAll(neighbors[d]);
						Nad.remove(b); Nad.remove(c);

						if(Nad.size() == 0)
							throw new RuntimeException("No deg3 though?");
						if(Nad.size() > 2){
							if(LOG) System.out.println("Nad too large, "+Nad);
							continue;
						}
						
						HashSet<Integer> Nbc = new HashSet<Integer>(neighbors[b]);
						Nbc.addAll(neighbors[c]);
						Nbc.remove(a); Nbc.remove(d);
						
						if(Nbc.size() == 0)
							throw new RuntimeException("No deg3 though?");
						if(Nbc.size() > 2){
							if(LOG) System.out.println("Nbc too large, "+Nbc);
							continue;
						}
						
						if(LOG) System.out.println("Desk? "+a+"-"+b+"-"+c+"-"+d);
						
						//differences in neighborhoods
						HashSet<Integer> Nv_sub_Nu = new HashSet<Integer>(Nad);
						Nv_sub_Nu.removeAll(Nbc);
						//other difference
						HashSet<Integer> Nu_sub_Nv = new HashSet<Integer>(Nbc);
						Nu_sub_Nv.removeAll(Nad);
						//intersection of neighborhoods
						HashSet<Integer> Nu_cap_Nv = new HashSet<Integer>(Nbc);
						Nu_cap_Nv.removeAll(Nu_sub_Nv);
						
						if(LOG) System.out.println("Intersection is "+Nu_cap_Nv);
						
						//I believe this is not needed.
//						if(Nu_cap_Nv.size() > 0)
//							continue;
						
						//remove 4-cycle from the graph
						clearVertex(a);
						clearVertex(b);
						clearVertex(c);
						clearVertex(d);
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
						int[] deskData = new int[5 + Nv_sub_Nu.size()];
						deskData[0] = 2;//type=desk
						deskData[1] = a;
						deskData[2] = b;
						deskData[3] = c;
						deskData[4] = d;
						{int dest = 5;
						for(int x : Nv_sub_Nu)
							deskData[dest++] = x;//all of N(A)\N(B)
						}
						deg2Folds_and_funnels.add(deskData);
						if(LOG) System.out.println("Desk! nnz="+numNonz()+", "+Arrays.toString(deskData));
						
						progress = true;
						continue vLoop;
					}
				}
			}
		}
		return progress;
	}

	//check for "generalized desks"
	private static boolean checkGenDesks() {
		if(!CHECK_GEN_DESKS)
			return false;
		boolean progress = false;
		vLoop: for(int a=0; a<N; a++) {
			int degA = neighbors[a].size();
			if(degA < 3 || degA > 4)
				continue;//not appropriate degree
			//consider each pair of neighbors (b,c) to a,
			//that are both degrees 3 and 4.
			//Can restrict to a < b < c.
			for(int b : neighbors[a]) {
				if(b <= a)
					continue;//can order better
				int degB = neighbors[b].size();
				if(degB < 3)
					continue;//not appropriate degree
				
				for(int c : neighbors[a]) {
					if(c <= b)
						continue;//can order better
					int degC = neighbors[c].size();
					if(degC < 3)
						continue;//not appropriate degree
					if(neighbors[b].contains(c))
						continue;//must be chordless
					
					//find a vertex d in N(b) and N(c), but not N(a)
					//TODO: choose whether to loop over N(b) or N(c) based
					// on which is smaller.
					//by permutation, we can assume d > a
					for(int d : neighbors[b]) {
						if(d <= a)
							continue;
						int degD = neighbors[d].size();
						if(degD < 3 || degD > 4)
							continue;//not appropriate degree
						if(neighbors[a].contains(d))
							continue;//chordless
						if(!neighbors[c].contains(d))
							continue;//need d to be in N(c)
						
						System.out.println("Desk? "+a+"-"+b+"-"+c+"-"+d);
						
						//External neighborhoods
						HashSet<Integer> Nad = new HashSet<Integer>(neighbors[a]);
						Nad.addAll(neighbors[d]);
						Nad.remove(b); Nad.remove(c);

						if(Nad.size() == 0)
							throw new RuntimeException("No deg3 though?");
						if(Nad.size() > 2){
							System.out.println("Nad too large, "+Nad);
						}
						
						HashSet<Integer> Nbc = new HashSet<Integer>(neighbors[b]);
						Nbc.addAll(neighbors[c]);
						Nbc.remove(a); Nbc.remove(d);
						
						if(Nbc.size() == 0)
							throw new RuntimeException("No deg3 though?");
						if(Nbc.size() > 2){
							System.out.println("Nbc too large, "+Nbc);
						}
						
						System.out.println("Desk "+a+"-"+b+"-"+c+"-"+d);
						
						//differences in neighborhoods
						HashSet<Integer> Nv_sub_Nu = new HashSet<Integer>(Nad);
						Nv_sub_Nu.removeAll(Nbc);
						//other difference
						HashSet<Integer> Nu_sub_Nv = new HashSet<Integer>(Nbc);
						Nu_sub_Nv.removeAll(Nad);
						//intersection of neighborhoods
						HashSet<Integer> Nu_cap_Nv = new HashSet<Integer>(Nbc);
						Nu_cap_Nv.removeAll(Nu_sub_Nv);
						
						System.out.println("Intersection is "+Nu_cap_Nv);
						
						//I believe this is not needed.
//						if(Nu_cap_Nv.size() > 0)
//							continue;
						
						//remove 4-cycle from the graph
						clearVertex(a);
						clearVertex(b);
						clearVertex(c);
						clearVertex(d);
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
						int[] deskData = new int[5 + Nv_sub_Nu.size()];
						deskData[0] = 2;//type=desk
						deskData[1] = a;
						deskData[2] = b;
						deskData[3] = c;
						deskData[4] = d;
						{int dest = 5;
						for(int x : Nv_sub_Nu)
							deskData[dest++] = x;//all of N(A)\N(B)
						}
						deg2Folds_and_funnels.add(deskData);
//						throw new RuntimeException("Desk! nnz="+numNonz()+", "+Arrays.toString(deskData));
						System.out.println("Desk! nnz="+numNonz()+", "+Arrays.toString(deskData));
						
						progress = true;
						continue vLoop;
					}
				}
			}
		}
		return progress;
	}
	
	//Check for "k-Twins", as in described in "Confining sets and avoiding bottleneck cases"
	private static boolean checkTwins() {
		if(!CHECK_TWINS)
			return false;
		
//		long[] hashes = new long[numNonz()];
//		int dest = 0;
		HashMap<Long, ArrayList<Integer>> hashes = new HashMap<>();
		for(int v=0; v<N; v++) {
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
		}
		for(ArrayList<Integer> list : hashes.values()) {
			if(list.size() == 1)
				continue;
			if(LOG) System.out.println("identical neighborhoods found in "+list);
			//TODO check they're indeed twins
			int numTwins = list.size();
			int v0 = list.get(0);
			int deg = neighbors[v0].size();
			if(deg <= numTwins) {
				throw new RuntimeException("Found a crown!");
			} else if(deg == numTwins - 1) {
				throw new RuntimeException("Found twins!");
			} else {
				if(LOG) System.out.println("Not useful twins.");
			}
		}
		return false;
	}

	//Based on the confinement described in "Confining sets and avoiding bottlenecks"
	private static boolean checkConfinement() {
		if(!CHECK_CONFINE)
			return false;
		
		boolean progress = false;
		
		HashSet<Integer> S = new HashSet<>();
		HashSet<Integer> NS = new HashSet<>();
		HashSet<Integer> W = new HashSet<>();
		
		vLoop: for(int v=0; v<N; v++) {
			if(neighbors[v].size() == 0)
				continue;
//			if(LOG) System.out.println("Checking confinement on "+v);
		
			S.clear();
			S.add(v);

			//neighbors N[S] of S
			NS.clear();
			NS.add(v);
			NS.addAll(neighbors[v]);
			
			Sloop: do{
				
				//extending vertices
				W.clear();
				boolean has_empty_nu_not_ns = false;//is there a u in N(S) such that N(u) - N[S] is empty?
				for(int u : NS) {
					if(S.contains(u))//u is only from N(S) not N[S]
						continue;
					int neighbors_in_S = 0;
					for(int nu : neighbors[u]) {
						if(S.contains(nu))
							neighbors_in_S++;
					}
					if(neighbors_in_S == 0)
						throw new RuntimeException("What? "+u+", "+S+", "+NS+", "+neighbors[u]);
					if(neighbors_in_S > 1)
						continue;//not a child
					ArrayList<Integer> nu_not_ns = new ArrayList<>();
					for(int nu : neighbors[u]) {
						if(!NS.contains(nu))
							nu_not_ns.add(nu);
					}
					boolean u_extending = (nu_not_ns.size() == 1);
					if(u_extending) {
//						if(LOG) System.out.println("extending vertex "+u+", add "+nu_not_ns);
						W.addAll(nu_not_ns);
					}
					if(nu_not_ns.size() == 0) {
//						if(LOG) System.out.println("Killing vertex "+u);
						has_empty_nu_not_ns = true;
					}
				}
				if(has_empty_nu_not_ns) {
					if(LOG) System.out.println("Unconfined: has_empty="+has_empty_nu_not_ns+", for W="+W);
					break Sloop;
				}
				if(W.size() == 0) {// |N(u) - N[S]| >= 2 for all u in N(S)
					//we found the set that confines v.
					if(S.size() > 1) {
//						if(LOG) System.out.println("Confining set for "+v+" is "+S);
					}
					continue vLoop;
				}
				//W is built. check if it's an independent set
				boolean W_independent = true;
				wLoop: for(int w : W) {
					for(int nw : neighbors[w]) {
						if(W.contains(nw)) {
							W_independent = false;
							break wLoop;
						}
					}
				}
				if(W_independent) {
					//add W and repeat
					S.addAll(W);
					for(int w : W) {
						NS.add(w);
						NS.addAll(neighbors[w]);
					}
//					if(LOG) System.out.println("Confining set for "+v+" grew to "+S);
					continue Sloop;
				} else {
					if(LOG) System.out.println("Unconfined: "+W_independent+", "+has_empty_nu_not_ns+", for W="+W);
					break Sloop;
				}
			}while(true);
			//v is unconfined, let's include it
			if(LOG) System.out.println("Unconfined! "+v);
			includes.add(v);
			clearVertex(v);
			progress = true;
		}
		if(LOG) System.out.println("Confinement progress="+progress);
			
		return progress;
	}
	
	private static void resolveFunnel(int v, int u) {
		HashSet<Integer> Nv = new HashSet<Integer>(neighbors[v]);
		HashSet<Integer> Nu = new HashSet<Integer>(neighbors[u]);
		//differences in neighborhoods
		HashSet<Integer> Nv_sub_Nu = new HashSet<Integer>(Nv);
		Nv_sub_Nu.removeAll(Nu);
		Nv_sub_Nu.remove(u);
		//other difference
		HashSet<Integer> Nu_sub_Nv = new HashSet<Integer>(Nu);
		Nu_sub_Nv.removeAll(Nv);
		Nv_sub_Nu.remove(v);
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
			funnelData[dest++] = x;//all of N(v)\N[u]
		}
		deg2Folds_and_funnels.add(funnelData);
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
