import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;

public class Graph {
	int N;
	HashSet<Integer>[] eList;
	HashSet<Integer>[] backEList;
	int[] inDeg, outDeg;
	
	static final boolean CHECK = false;
	
	public Graph(int N_, HashSet<Integer>[] eList_, HashSet<Integer>[] backEList_,
			int[] inDeg_, int[] outDeg_) {
		N = N_;
		eList = eList_;
		backEList = backEList_;
		inDeg = inDeg_;
		outDeg = outDeg_;
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
	Graph copy() {
		HashSet<Integer>[] newEList = new HashSet[N];
		HashSet<Integer>[] newBackEList = new HashSet[N];
		for(int i=0; i<N; i++) {
			newEList[i] = (HashSet<Integer>) eList[i].clone();
			newBackEList[i] = (HashSet<Integer>) backEList[i].clone();
		}
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
}
