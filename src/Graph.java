import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;

public class Graph {
	int N;
	HashSet<Integer>[] eList;
	HashSet<Integer>[] backEList;
	int[] inDeg, outDeg;
	
	static final boolean CHECK = true;
	
	public Graph(int N_, HashSet<Integer>[] eList_, HashSet<Integer>[] backEList_,
			int[] inDeg_, int[] outDeg_) {
		N = N_;
		eList = eList_;
		backEList = backEList_;
		inDeg = inDeg_;
		outDeg = outDeg_;
	}

	public Graph(int N_, HashSet<Integer>[] eList_, HashSet<Integer>[] backEList_) {
		N = N_;
		eList = eList_;
		backEList = backEList_;
		inDeg = new int[N];
		outDeg = new int[N];
		for(int i=0; i<N; i++) {
			inDeg[i] = backEList_[i].size();
			outDeg[i] = eList_[i].size();
		}
	}
	
	//Sets graph to be a copy of given reduced graph. Returns int[] mapping back to vertices
	static Graph fromReducedGraph(ReducedGraph rg){
		rg.condense();
		int N = rg.N;
		return new Graph(N, cloneList(rg.eList), cloneList(rg.backEList),
				Arrays.copyOf(rg.inDeg,N), Arrays.copyOf(rg.outDeg, N));
	}
	
	void checkConsistency() {
		if(!CHECK)
			return;
		
		int Ef=0, Eb=0;
		for(int i=0; i<N; i++) {
			Ef+=eList[i].size();
			Eb+=backEList[i].size();
			if(eList[i].size() != outDeg[i])
				throw new RuntimeException("Error while pruning (2)"+i);
			if(backEList[i].size() != inDeg[i])
				throw new RuntimeException("Error while pruning (3)"+i);
		}
		if(Ef != Eb)
			throw new RuntimeException("Error while pruning (4)");
	}
	
	//Clear all edges from a vertex v.
	//Destructive, obviously.
	void clearVertex(int v) {
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
	
	void clearEdge(int v1, int v2) {
		eList[v1].remove(v2);
		backEList[v2].remove(v1);
		outDeg[v1]--;
		inDeg[v2]--;
	}

	@SuppressWarnings("unchecked")
	private static HashSet<Integer>[] cloneList(HashSet<Integer>[] arr){
		int N = arr.length;
		HashSet<Integer>[] newArr = new HashSet[N];
		for(int i=0; i<N; i++) {
			newArr[i] = (HashSet<Integer>) arr[i].clone();
		}
		return newArr;
	}
	
	Graph copy() {
		HashSet<Integer>[] newEList = cloneList(eList);
		HashSet<Integer>[] newBackEList = cloneList(backEList);
		return new Graph(N, newEList, newBackEList, Arrays.copyOf(inDeg,N), Arrays.copyOf(outDeg,N));
	}

	private boolean findCycleHelper(int i, boolean[] visited, boolean[] inPath, ArrayDeque<Integer> path) {

		if (inPath[i]) {
			// made it back to "i" which we visited previously.
			// to get a cycle, trim off the stuff leading up to i.
			while (path.peek() != i)
				path.pollFirst();
			return true;
		}

		if (visited[i])
			return false;

		visited[i] = true;
		inPath[i] = true;
		path.addLast(i);

		for (Integer c : eList[i])
			if (findCycleHelper(c, visited, inPath, path))
				return true;

		inPath[i] = false;
		path.pollLast();
		return false;
	}

	ArrayDeque<Integer> findCycleDFS() {
		boolean[] visited = new boolean[N];
		boolean[] recStack = new boolean[N];
		ArrayDeque<Integer> path = new ArrayDeque<Integer>();

		for (int i = 0; i < N; i++)
			if (findCycleHelper(i, visited, recStack, path))
				return path;

		return null;
	}

	private boolean hasCycleHelper(int i, boolean[] visited, boolean[] inPath) {
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

	//Similar to findCycleDFS but just finding one
	boolean hasCycle() {
        boolean[] visited = new boolean[N];
        boolean[] recStack = new boolean[N];        
        for (int i = 0; i < N; i++)
            if(hasCycleHelper(i, visited, recStack))
                return true;
        return false;
    }
	
	public int E() {
		checkConsistency();
		int tot = 0;
		for(int i=0; i<N; i++)
			tot += outDeg[i];
		return tot;
	}
	
	//remove all edges
	void clear() {
		for(int i=0; i<N; i++) {
			eList[i].clear();
			backEList[i].clear();
			inDeg[i] = outDeg[i] = 0;
		}
		checkConsistency();
	}
	
	//How many vertices have degree > 0, the 'effective' N?
	int nonZeroDegN() {
		int res = 0;
		for(int i=0; i<N; i++)
			if(inDeg[i] > 0 || outDeg[i] > 0)
				res++;
		return res;
	}
	
	String dumpS() {
		String res = "";
		res += "{";
		boolean firstRow = true;
		for(int i=0; i<N; i++) {
			if(inDeg[i] ==0 && outDeg[i] == 0)
				continue;
			if(!firstRow) {
				res += ",";
			} else {
				firstRow = false;
			}
			res += "{";
			boolean first = true;
			for(int vo : eList[i]) {
				if(!first)
					res += ", ";
				res += i+" -> "+vo;
				first = false;
			}
			res += "}\n";
		}
		res += "}\n";
		return res;
	}

	void dump() {
		System.out.println(dumpS());
	}
}
