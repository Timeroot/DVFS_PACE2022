import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

//This is similar to ChunkCoverReductions, but is built to chunks too.
//It won't really reduce those big chunks themselves, but it will work on the vertex
//cover part (K2s) while respecting the big cycles and chunks.
public class ChunkCoverReductions {
	static int N;
	static ArrayList<Integer>[] neighbors;
	
	static int[] inBigCyc;
	static boolean[] inChunk;
	static LinkedList<int[]> bigCycList;
	static ArrayList<GraphChunk> chunks;
	
	//this data saves things needed to save the reduction.
	//degree 2 foldings. For a deg-2 pattern a-b-c, this stores {0, b,a,c}.
	//for a funnel u-v-{N(v)-u}, this saves {1, u,v, ...N(v)\N[u]... }.
	static ArrayList<int[]> deg2Folds_and_funnels;
	static ArrayList<Integer> includes;//forced inclusions from e.g. domination, deg-1..
	
	//used in shrinkCyclesNewIncludes
	static int lastIncludesUpdate;
	
	static final boolean LOG = true && Main_Load.TESTING;
	
	//check that it works, in case you're afraid of bugs
	static final boolean VERIFY = true;
	//if you're worried that the changes to bigCyc will be what breaks the verify,
	//this makes a copy.
	static final boolean CLONE_BIG_CYC = true;
	static final boolean CHECK_KFUNNEL = true;
	static final boolean CHECK_CONFINE = true;
	
	@SuppressWarnings("unchecked")
	public static ArrayList<Integer> solve(ArrayList<int[]> _edges, LinkedList<int[]> _bigCyc,
			ArrayList<GraphChunk> _chunks, int _N) {
		
		//store big cycle list
		bigCycList = _bigCyc;
		if(CLONE_BIG_CYC) {
			bigCycList = new LinkedList<>();
			for(int[] cyc : _bigCyc)
				bigCycList.add(cyc.clone());
		}
		chunks = _chunks;
		
		lastIncludesUpdate = 0;
		
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
		
		//populate the array isInBigCyc
		inBigCyc = new int[N];
		markIsInBigCyc();
		inChunk = new boolean[N];
		markIsInChunk();
		
		if(Main_Load.VERBOSE){
			System.out.println("Initial CycleCover problem: N = "+N);
			System.out.println(numNonz()+" nonempty vertices");
		}
		
		boolean progress = true;
		while(progress) {
			checkConsistency();
			progress = false;
			for(int v=0; v<N; v++) {
				
				shrinkCyclesNewIncludes();
				markIsInBigCyc();
				
				int deg = neighbors[v].size();
				//deg 0 --> it's not even there
				if(deg == 0) {
					//could be that this has just one big cycles, but then it will be
					//dropped in dropDegZeroBigcycs.
					continue;
				}
				//prune degree 1
				if(deg == 1) {
					if(inChunk[v])
						continue;
					if(inBigCyc[v] > 0){
						//could still prune it if the big cycles all also include the
						//single neighbor, but that's handled by dropImpliedBigCycles.
						continue;
					}
					
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
					if(inChunk[v])
						continue;
					if(inBigCyc[v] > 0)
						continue;
					
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
						//fold
						if(inBigCyc[oA] > 0 || inBigCyc[oB] > 0)
							continue;
						if(inChunk[oA] || inChunk[oB])
							continue;
						
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
				if(inBigCyc[v] == 0 && !inChunk[v]) {
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
				} else {
					//can't be simplicial
				}
				
				//check for vertices we dominate
				if(true){
					ArrayList<Integer> Nv = neighbors[v];
					TreeSet<Integer> Nv_ts = new TreeSet<>(Nv);
					boolean v_dominates = false;
					
					for(int n1 : Nv) {
						int deg2 = neighbors[n1].size();
						if(deg2 >= deg)//n1 can't be dominated by v if it has at least many other neighbors
							continue;
						
						if(inBigCyc[n1] > 0)
							continue;
						if(inChunk[n1])
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
				{
					if(deg == 3) {
						if(inBigCyc[v] > 0)//TODO if v is in big cycle
							continue;
						if(inChunk[v])
							continue;
						
						int oA = neighbors[v].get(0);
						int oB = neighbors[v].get(1);
						int oC = neighbors[v].get(2);
						
						int u = -1;//special one
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

							if(inBigCyc[u] > 0 || inChunk[u]) {
								replaceAlternativeInChunks(u, Nv_sub_Nu);
							}
							
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
			if(!progress) {
				progress = checkKfunnel();
			}
			if(!progress) {
				progress = checkConfinement();
			}
			
			//Now we can look for anything of degree zero that can be dropped / included
			shrinkCyclesNewIncludes();
			markIsInBigCyc();
			markIsInChunk();

			if(dropImpliedBigCycles()) {
				markIsInBigCyc();
				markIsInChunk();
			}
			if(dropDegZeroBigcycs()) {
				markIsInBigCyc();
				markIsInChunk();
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
		
		verify(solution, _edges, _bigCyc);
		
		return getTrues(solution);
//		System.exit(1);
//		return null;
	}
	
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
			if(inBigCyc[v] > 0 || inChunk[v])
				continue;
			
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

		if(inBigCyc[u] > 0 || inChunk[u]) {
			replaceAlternativeInChunks(u, Nv_sub_Nu);
		}
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
	
	//If a vertex has degree zero and is in only one big cycle, we can drop it.
	private static boolean dropDegZeroBigcycs() {
		boolean changed = false;
		for(int v=0; v<N; v++) {
			if(inChunk[v])
				continue;
			int deg = neighbors[v].size();
			if(deg > 0)
				continue;
			if(inBigCyc[v] == 1){//can exclude it
				for(Iterator<int[]> iter = bigCycList.iterator();
						iter.hasNext();) {
					int[] cyc = iter.next();
					boolean has = false;
					for(int a : cyc) {
						if(a == v) {
							has = true; break; 
						}
					}
					if(!has)
						continue;
					//found it.
					if(LOG) System.out.println("Found isolated cycle "+Arrays.toString(cyc)+", v="+v);
					if(cyc.length <= 2) {
						throw new RuntimeException("How'd we get a small 'big' cycle of size "+cyc.length);
					} else if(cyc.length == 3) {
						//drop v from the 3-cycle, add an edge between the other two.
						int vo_1 = cyc[0];
						int vo_2 = cyc[1];
						if(vo_1 == v)
							vo_1 = cyc[2];
						else if(vo_2 == v)
							vo_2 = cyc[2];
						iter.remove();
						neighbors[vo_1].add(vo_2);
						neighbors[vo_2].add(vo_1);
						changed = true;
					} else {
						//strip it
						int[] newCyc = new int[cyc.length-1];
						int dest = 0;
						for(int a : cyc) {
							if(a != v)
								newCyc[dest++] = a;
						}
						iter.remove();
						bigCycList.add(newCyc);
						changed = true;
						//have to defer adding in the new cycle until iterator is done
						break;
					}
				}
			} else {
				//more than one big cycle
				continue;
			}
		}
		return changed;
	}
	
	private static void replaceAlternativeInChunks(int u, HashSet<Integer> replicators) {
		//add in any cycles missing from
		if(inBigCyc[u] > 0) {
			ArrayList<int[]> replacedCycles = new ArrayList<>();
			
			for(Iterator<int[]> iter = bigCycList.iterator();
					iter.hasNext();) {
				int[] cyc = iter.next();
				boolean has = false;
				for(int a : cyc) {
					if(a == u) {
						has = true; break; 
					}
				}
				if(!has)
					continue;
				
				//If the cycle contains any r from the replicators, then we actually actually just
				//remove u from the cycle and call it a day. Because we create a new cycle where
				//we replace u by r, which is already in the cycle; this cycle will be one smaller
				//than all the others, and so dominate all the others. Additionally, if the cycle
				//is size 3, this becomes an edge.
				boolean cycHitsReps = false;
				for(int c : cyc) {
					if(replicators.contains(c)) {
						cycHitsReps = true;
						break;
					}
				}
				if(cycHitsReps) {
					//Don't need to replicate, just reduce.
					if(cyc.length == 3) {
						//drop u from the 3-cycle, add an edge between the other two.
						int vo_1 = cyc[0];
						int vo_2 = cyc[1];
						if(vo_1 == u)
							vo_1 = cyc[2];
						else if(vo_2 == u)
							vo_2 = cyc[2];
						iter.remove();
						neighbors[vo_1].add(vo_2);
						neighbors[vo_2].add(vo_1);
					} else {
						//just shrink and turn it into a smaller one
						int[] newCyc = new int[cyc.length-1];
						int dest = 0;
						for(int a : cyc) {
							if(a != u)
								newCyc[dest++] = a;
						}
						iter.remove();
						replacedCycles.add(newCyc);
					}
					continue;
				}
				
				for(int rep : replicators) {
					int[] newCyc = new int[cyc.length];
					int dest = 0;
					for(int c : cyc) {
						int val = (c==u) ? rep : c;
						newCyc[dest++] = val;
						inBigCyc[val]++;
					}
					replacedCycles.add(newCyc);
				}
				for(int c : cyc) {
					inBigCyc[c]--;
				}
				iter.remove();
			}
			bigCycList.addAll(replacedCycles);
		}
		
		if(inChunk[u]) {
			int[] replicatorArr = replicators.stream().mapToInt(x -> x).toArray();
			for(GraphChunk chunk : chunks) {
				int ui = chunk.getPremapping(u);
				if(ui == -1)
					continue;
				int[] inds = chunk.addToMapping(replicatorArr);
				HashSet<Integer> origElist = chunk.gInner.eList[ui];
				HashSet<Integer> origBackElist = chunk.gInner.backEList[ui];
				for(int ind : inds) {
					chunk.gInner.eList[ind].addAll(origElist);
					chunk.gInner.backEList[ind].addAll(origBackElist);
					for(int fwd : origElist) {
						chunk.gInner.backEList[fwd].add(ind);
					}
					for(int bwd : origBackElist) {
						chunk.gInner.eList[bwd].add(ind);
					}
				}
				//update degrees
				for(int ind : inds) {
					chunk.gInner.outDeg[ind] = chunk.gInner.eList[ind].size();
					chunk.gInner.inDeg[ind] = chunk.gInner.backEList[ind].size();
				}
				for(int ind : origElist) {
					chunk.gInner.outDeg[ind] = chunk.gInner.eList[ind].size();
					chunk.gInner.inDeg[ind] = chunk.gInner.backEList[ind].size();
				}
				for(int ind : origBackElist) {
					chunk.gInner.outDeg[ind] = chunk.gInner.eList[ind].size();
					chunk.gInner.inDeg[ind] = chunk.gInner.backEList[ind].size();
				}
				//drop u
				chunk.gInner.clearVertex(ui);
				chunk.gInner.checkConsistency();
			}
			for(int rep : replicators) {
				inChunk[rep] = true;
			}
			inChunk[u] = false;
		}
	}

	static void verify(boolean[] solution, ArrayList<int[]> edgeList, LinkedList<int[]> bigCycList) {
		if(!VERIFY)
			return;
		
		for(List<int[]> edgeType : new List[] {edgeList, bigCycList}) {
		edgeLoop: for(int[] edge : edgeType) {;
			for(int v : edge)
				if(solution[v])
					continue edgeLoop;
			if(Main_Load.TESTING) {
					System.out.println("FAILURE");
					System.out.println("Dumping bad solution: {");
					for(int i=0; i<N; i++)
						if(solution[i])
							System.out.print(i+",");
					System.out.println("}");
			}
			throw new RuntimeException("Edge "+Arrays.toString(edge)+" unfulfilled!");
		}
		}
		if(Main_Load.TESTING)
			System.out.println("Verified");
	}
	
	private static boolean shrinkCyclesNewIncludes() {
		int nI = includes.size() - lastIncludesUpdate;
		if(nI == 0)
			return false;
		
		int[] newIncludes = new int[nI];
		for(int i=0 ; i<nI; i++) {
			newIncludes[i] = includes.get(lastIncludesUpdate + i);
		}
		lastIncludesUpdate = includes.size();
		Arrays.sort(newIncludes);
		
		if(LOG)
			System.out.println("New includes are "+Arrays.toString(newIncludes));
		
		boolean changed = false;
		for(Iterator<int[]> iter = bigCycList.iterator();
				iter.hasNext();) {
			int[] cyc = iter.next();
			boolean satted = false;
			vLoop: for(int v : cyc) {
				if(Arrays.binarySearch(newIncludes, v) >= 0) {//in there
					satted = true;
					break vLoop;
				}
			}
			if(satted) {
				//remove from bigCycList
				iter.remove();
				if(LOG)
					System.out.println("Dropped cycle "+Arrays.toString(cyc));
				changed = true;
			}
		}
		
		for(GraphChunk chunk : chunks) {
			boolean chunkChanged = false;
			int cN = chunk.gInner.N;
			for(int vi = 0; vi < cN; vi++) {
				int v = chunk.mapping[vi];
				if(Arrays.binarySearch(newIncludes, v) >= 0) {//in there

					if(LOG) {
						System.out.println("Chunk changed, drop "+vi);
						for(int n1 : chunk.gInner.backEList[vi]) {
							System.out.println(n1+" -> "+chunk.gInner.eList[n1]);
						}
						for(int n1 : chunk.gInner.eList[vi]) {
							System.out.println(n1+" <- "+chunk.gInner.backEList[n1]);
						}
					}
					
					chunk.gInner.clearVertex(vi);
					chunk.gInner.checkConsistency();
					changed = true;
					chunkChanged = true;
					
				}
			}
			
			if(chunkChanged) {
				//At least one vertex was cleared. Now repeatedly sweep out vertices
				//with in- or out-deg zero, until no more changes.
				while(chunkChanged) {
					chunkChanged = false;
					for(int vi = 0; vi < cN; vi++) {
						int inDeg = chunk.gInner.inDeg[vi];
						int outDeg = chunk.gInner.outDeg[vi];
						if((inDeg > 0 && outDeg == 0) ||
							(outDeg > 0 && inDeg == 0)) {

							chunk.gInner.clearVertex(vi);
							chunk.gInner.checkConsistency();
							chunkChanged = true;
							if(LOG) {
								System.out.println("Led to clearing "+vi);
							}
						}
					}
				}
				//Since we reduced it, we can do an SCC sweep to try to remove some edges too.
				//TODO this method really shouldn't live in MinimumCoverDescriptor.
				MinimumCoverDescriptor.stripSCC(chunk.gInner);
			}
		}
		
		return changed;
	}
	
	//check for big cycles to remove. Returns true if succeeded.
	private static boolean dropImpliedBigCycles() {
		//need fast neighbor lists
		for(int v=0; v<N; v++) {
			Collections.sort(neighbors[v]);
		}
		int dropped = 0;

		cycLoop: for(Iterator<int[]> iter = bigCycList.iterator();
				iter.hasNext();) {
			int[] cyc = iter.next();
			for(int v1 : cyc) {
				for(int v2 : cyc) {
					if(v2 <= v1)
						continue;
					if(Collections.binarySearch(neighbors[v1], v2) >= 0) {
						iter.remove();
						dropped++;
						continue cycLoop;
					}
				}
			}
		}
		if(LOG)
			System.out.println("CCR: Dropped "+dropped+" out of "+(dropped+bigCycList.size()));
		return (dropped > 0); 
	}
	
	static void markIsInBigCyc() {
		Arrays.fill(inBigCyc, 0);
		for(int[] bigCyc : bigCycList) {
			for(int v : bigCyc) {
				inBigCyc[v]++;
			}
		}
	}
	
	static void markIsInChunk() {
		Arrays.fill(inChunk, false);
		for(GraphChunk chunk : chunks) {
			int cN = chunk.gInner.N;
			for(int vi = 0; vi < cN; vi++) {
				if(chunk.gInner.inDeg[vi] == 0 && chunk.gInner.outDeg[vi] == 0)
					continue;//not really in the chunk, it has no edges.
				//alright, mark it as used by a chunk.
				int v = chunk.mapping[vi];
				inChunk[v] = true;
			}
		}
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
//					if(LOG) System.out.println("Checking confinement on "+v);
		
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
//								if(LOG) System.out.println("extending vertex "+u+", add "+nu_not_ns);
						W.addAll(nu_not_ns);
					}
					if(nu_not_ns.size() == 0) {
//								if(LOG) System.out.println("Killing vertex "+u);
						has_empty_nu_not_ns = true;
					}
				}
				if(has_empty_nu_not_ns) {
//					if(LOG) System.out.println("Unconfined: has_empty="+has_empty_nu_not_ns+", for W="+W);
					break Sloop;
				}
				if(W.size() == 0) {// |N(u) - N[S]| >= 2 for all u in N(S)
					//we found the set that confines v.
					if(S.size() > 1) {
//								if(LOG) System.out.println("Confining set for "+v+" is "+S);
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
//							if(LOG) System.out.println("Confining set for "+v+" grew to "+S);
					continue Sloop;
				} else {
//					if(LOG) System.out.println("Unconfined: "+W_independent+", "+has_empty_nu_not_ns+", for W="+W);
					break Sloop;
				}
			}while(true);
			//check if any of S, NS, or W has a cycle.
//			for(int vo : W) {
//				if(inBigCyc[vo] > 0 || inChunk[vo])
//					continue vLoop;
//			}
//			for(int vo : S) {
//				if(inBigCyc[vo] > 0 || inChunk[vo])
//					continue vLoop;
//			}
			for(int vo : NS) {
				if(inBigCyc[vo] > 0 || inChunk[vo])
					continue vLoop;
			}
			//v is unconfined, let's include it
			if(LOG) System.out.println("Unconfined! "+v);
			includes.add(v);
			clearVertex(v);
			progress = true;
		}
		if(LOG) System.out.println("Confinement progress="+progress);
			
		return progress;
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
			MinimumCoverInfo mci = new MinimumCoverInfo(N, pairList, new ArrayList<>(bigCycList), chunks, null);
			boolean[] res = new boolean[N];
			ArrayList<Integer> sol = new ILP_CoverAndChunks_Reopt().solve(mci);
			for(int v : sol)
				res[v] = true;
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
		for(int v=0; v<N; v++) {
			if(neighbors[v].size() > 0 || inBigCyc[v] > 0 || inChunk[v])
				degGTz++;
		}
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
