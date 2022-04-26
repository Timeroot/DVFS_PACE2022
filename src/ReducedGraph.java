import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;
import java.util.stream.Collectors;

public class ReducedGraph {
	int N;
	HashSet<Integer>[] eList;
	HashSet<Integer>[] backEList;
	int[] inDeg, outDeg;
	
	final int Norig;
	int[] invMap;
	boolean[] dropped;
	int dropped_Size = 0;
	ArrayList<Integer> mustFVS;
	
	static final boolean CHECK = false;
	
	public ReducedGraph(int N_, HashSet<Integer>[] eList_, HashSet<Integer>[] backEList_,
			int[] inDeg_, int[] outDeg_) {
		
		Norig = N = N_;
		eList = eList_;
		backEList = backEList_;
		inDeg = inDeg_;
		outDeg = outDeg_;
		
		invMap = new int[N];
		for(int i=0; i<N; i++)
			invMap[i] = i;
		dropped = new boolean[N];
		dropped_Size = 0;
		mustFVS = new ArrayList<>();
	}
	
	public static ReducedGraph fromGraph(Graph g) {
		return new ReducedGraph(g.copy());
	}
	
	//Uses the same underlying arrays as g, so should only be used with g.copy() if will be edited.
	private ReducedGraph(Graph g) {
		this(g.N, g.eList, g.backEList, g.inDeg, g.outDeg);
	}
	
	private ReducedGraph(int N_, HashSet<Integer>[] eList_, HashSet<Integer>[] backEList_,
			int[] inDeg_, int[] outDeg_, int Norig_, int[] invMap_, boolean[] dropped_, 
			int dropped_Size_, ArrayList<Integer> mustFVS_) {
		Norig = N = N_;
		eList = eList_;
		backEList = backEList_;
		inDeg = inDeg_;
		outDeg = outDeg_;
		
		invMap = invMap_;
		dropped = dropped_;
		dropped_Size = dropped_Size_;
		mustFVS = mustFVS_;
	}
	
	void prune() {
		PruneReduced.prune(this);
	}
	
	ArrayList<Integer> transformSolution(ArrayList<Integer> reducedSol) {
		if(reducedSol == null) //the "no solution" case
			return null;
		
		for(int i=0; i<reducedSol.size(); i++) {
			int v = reducedSol.get(i);
			reducedSol.set(i, invMap[v]);
		}
		reducedSol.addAll(mustFVS);
		return reducedSol;
	}
	
	final void checkConsistency() {
		if(!CHECK)
			return;
		
		int Ef=0, Eb=0;
		for(int i=0; i<N; i++) {
			Ef+=eList[i].size();
			Eb+=backEList[i].size();
			if(eList[i].size() != outDeg[i])
				throw new RuntimeException("Error @ "+i);
			if(backEList[i].size() != inDeg[i])
				throw new RuntimeException("Error @ "+i);
			if(dropped[i]) {
				if(eList[i].size() != 0)
					throw new RuntimeException("Error @ "+i);
				if(backEList[i].size() != 0)
					throw new RuntimeException("Error @ "+i);
			}
			for(int v0 : eList[i])
				if(dropped[v0])
					throw new RuntimeException("Error @ "+i+"->"+v0);
			for(int v0 : backEList[i])
				if(dropped[v0])
					throw new RuntimeException("Error @ "+i+"<-"+v0);
		}
		if(Ef != Eb)
			throw new RuntimeException("Error in consistency");
	}
	
	//Clear all edges from a vertex v.
	//Destructive, obviously.
	void clearVertex(int v) {
		dropped[v] = true;
		dropped_Size++;
		
		for(int vo : eList[v]){
			backEList[vo].remove(v);
			inDeg[vo]--;
		}
		for(int vo : backEList[v]) {
			eList[vo].remove(v);
			outDeg[vo]--;
		}
		eList[v].clear();
		backEList[v].clear();
		inDeg[v] = outDeg[v] = 0;
	}
	
	//"Condense" the graph by re-indexing to remove any dropped vertices from the enumeration.
	//This is a bit slow, but once many vertices have been dropped, it pays off to ... defragment.
	
	void check_condense() {
		if(dropped_Size/3 > N)
			condense();
	}
	
	int real_N() {
		return N - dropped_Size;
	}
	
	void condense() {
		if(dropped_Size == 0)
			return;
		
		//Re-index!
		int[] pruneRemap = new int[N];
		int di = 0;
		for(int i=0; i<N; i++) {
			if(dropped[i]) {
				pruneRemap[i] = -1;
			} else {
				pruneRemap[i] = di++;
			}
		}
		int Norig = N;
		N = di;
		
		di = 0;
		for(int i=0; i<Norig; i++) {
			if(dropped[i]) {
			} else {
				invMap[di++] = invMap[i];
			}
		}
		for(int i=di; i<Norig; i++) {
			invMap[i] = -1;
		}
		
		//Edit eList and backEList
		HashSet<Integer>[] newEList = new HashSet[N];
		HashSet<Integer>[] newBackEList = new HashSet[N];
		for(int oi=0; oi<Norig; oi++) {
			if(dropped[oi])
				continue;
			int i = pruneRemap[oi];
			HashSet<Integer> li = eList[oi];
			newEList[i] = li.stream().map(x -> pruneRemap[x]).collect(Collectors.toCollection(HashSet::new));

			HashSet<Integer> bli = backEList[oi];
			newBackEList[i] = bli.stream().map(x -> pruneRemap[x]).collect(Collectors.toCollection(HashSet::new));
		}
		eList = newEList;
		backEList = newBackEList;
		
		inDeg = new int[N];
		outDeg = new int[N];
		for(int i=0; i<N; i++) {
			inDeg[i] = backEList[i].size();
			outDeg[i] = eList[i].size();
		}

		dropped = new boolean[N];
		dropped_Size = 0;
	}
	
	public String toString() {
		return "RG[@"+System.identityHashCode(this)+","+eList+","+backEList+","+inDeg+","+outDeg+","+invMap+","+dropped+","+mustFVS+"]";
	}
	
	@SuppressWarnings("unchecked")
	ReducedGraph copy(boolean keepMustFVS) {
		HashSet<Integer>[] newEList = new HashSet[N];
		HashSet<Integer>[] newBackEList = new HashSet[N];
		for(int i=0; i<N; i++) {
			newEList[i] = (HashSet<Integer>) eList[i].clone();
			newBackEList[i] = (HashSet<Integer>) backEList[i].clone();
		}
		if(keepMustFVS)
			return new ReducedGraph(N, newEList, newBackEList, Arrays.copyOf(inDeg,N), Arrays.copyOf(outDeg,N),
				Norig, Arrays.copyOf(invMap,Norig), Arrays.copyOf(dropped,N), dropped_Size, 
				(ArrayList<Integer>)mustFVS.clone());
		else
			return new ReducedGraph(N, newEList, newBackEList, Arrays.copyOf(inDeg,N), Arrays.copyOf(outDeg,N),
				Norig, Arrays.copyOf(invMap,Norig), Arrays.copyOf(dropped,N), dropped_Size, 
				new ArrayList<>());
	}

	private boolean hasCycleHelper(int i, boolean[] visited, boolean[] inPath) {
		if (dropped[i])
			return false;
		if (inPath[i])
			return true;
		if (visited[i])
			return false;
		visited[i] = true;
		inPath[i] = true;
		for (Integer c : eList[i])
			if (hasCycleHelper(c, visited, inPath))
				return true;
		inPath[i] = false;
		return false;
	}

	//Answering: if you ignore the dropped vertices, is the graph acyclic?
	boolean hasCycleWithoutDropped() {
        boolean[] visited = new boolean[N];
        boolean[] recStack = new boolean[N];        
        for (int i = 0; i < N; i++)
            if(hasCycleHelper(i, visited, recStack))
                return true;
        return false;
    }
	
	//Uses components in an SCC decomposition to break into two smaller graphs.
	//if passOnMustFVS is true, then marked vertices get passed into the first subgraph.
	//if false, nothing gets them, and they must be collected from original graph by caller.
	ArrayList<ReducedGraph> splitOnSCC(SCC scc, boolean passOnMustFVS) {
		if(scc.sccInfo.size() <= 1)
			throw new RuntimeException("Bad scc, size="+scc.sccInfo.size());
		
		ArrayList<ReducedGraph> res = new ArrayList<>();
		
		if(passOnMustFVS)
			throw new RuntimeException("Not supported");
		
		for(ArrayList<Integer> comp : scc.sccInfo) {
			comp.sort(Integer::compare);
			int N_ = comp.size();
			HashSet<Integer>[] eList_ = new HashSet[N_];
			HashSet<Integer>[] backEList_ = new HashSet[N_];
			
			for(int v_=0; v_<N_; v_++) {
				int v = comp.get(v_);
				
				HashSet<Integer> el = new HashSet<Integer>();
				for(int vo : eList[v]) {
					int vo_ = Collections.binarySearch(comp, vo);
					if(vo_ >= 0)
						el.add(vo_);
				}
				eList_[v_] = el;

				HashSet<Integer> bel = new HashSet<Integer>();
				for(int vo : backEList[v]) {
					int vo_ = Collections.binarySearch(comp, vo);
					if(vo_ >= 0)
						bel.add(vo_);
				}
				backEList_[v_] = bel;
			}
			
			int[] inDeg_ = new int[N_];
			int[] outDeg_ = new int[N_];
			int[] invMap_ = new int[N_];
			for(int v_=0; v_<N_; v_++) {
				inDeg_[v_] = backEList_[v_].size();
				outDeg_[v_] = eList_[v_].size();
				invMap_[v_] = invMap[comp.get(v_)];
			}
			
			ReducedGraph part = new ReducedGraph(N_, eList_, backEList_,
					inDeg_, outDeg_, N_, invMap_, new boolean[N_], 
					0, new ArrayList<>());
			part.checkConsistency();
			res.add(part);
		}
		
		return res;
	}

	//very verbose, slow debugging methods
//	String fullHash() {
//		String res = "";
//		long val = 0;
//		val += N*1300037;
//		val += Norig*1300000039;
//		
//		int countedDropped = 0;
//		int countedMFVS = 0;
//		
//		for(int i=0; i<N; i++) {
//			if(inDeg[i] != backEList[i].size() || outDeg[i] != eList[i].size())
//				throw new RuntimeException("Inconsistency");
//			val *= 1359;
//			if(dropped[i]) {
//				val -= 1; 
//				countedDropped++;
//			} else {
//				val += i*10030001; 
//				for(int vo:eList[i]) {
//					val *= 7000031;
//					val += vo;
//					if(!backEList[vo].contains(i))
//						throw new RuntimeException("Inconsistency");
//				}
//				for(int vo:backEList[i]) {
//					val *= 3000319;
//					val += vo;
//					if(!eList[vo].contains(i))
//						throw new RuntimeException("Inconsistency");
//				}
//			}
//		}
//		res += val;
//		
//		val = 0;
//		for(int i=0; i<Norig; i++) {
//			val *= 7171;
//			val += invMap[i];
//		}
//		res += ","+val;
//		
//		val = 0;
//		for(int i=0; i<Norig; i++) {
//			val *= 191919;
//			if(mustFVS[i]) {
////				System.out.print(" M"+i);
//				val += i;
//				countedMFVS++;
//			}
//		}
//		res += ","+val;
//		
//		if(dropped_Size != countedDropped)
//			throw new RuntimeException("Inconsistency");
//		if(mustFVS_Size != countedMFVS)
//			throw new RuntimeException("Inconsistency ");
//		return res;
//	}
	
//	String dumpMVFS() {
//		String res = "[";
//		for(int i=0; i<Norig; i++) {
//			if(mustFVS[i]) {
//				res += i+",";
//			}
//		}
//		return res+"]";
//	}
	
	void dump() {
		System.out.print("{");
		for(int i=0; i<N; i++) {
			if(dropped[i])
				continue;
			System.out.print("{");
			boolean first = true;
			for(int vo : eList[i]) {
				if(!first)
					System.out.print(", ");
				System.out.print(i+" -> "+vo);
				first = false;
			}
			System.out.println("}" + (i!=N-1?",":"}"));
		}
	}
	
}
