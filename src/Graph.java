import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

public class Graph {
	int N;
	HashSet<Integer>[] eList;
	HashSet<Integer>[] backEList;
	int[] inDeg, outDeg;
	
	static final boolean CHECK = Main_Load.GRAPH_CHECK;
	
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
		boolean worked = eList[v1].remove(v2);
		if(!worked)
			throw new RuntimeException("Tried to clear edge "+v1+"->"+v2+" that wasn't in graph.");
		backEList[v2].remove(v1);
		outDeg[v1]--;
		inDeg[v2]--;
	}
	
	void addEdge(int v1, int v2) {
		boolean worked = eList[v1].add(v2);
		if(!worked)
			throw new RuntimeException("Tried to add edge "+v1+"->"+v2+" that was already in graph.");
		backEList[v2].add(v1);
		outDeg[v1]++;
		inDeg[v2]++;
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
	
	static final boolean CHORD_MINIMIZE = true;
	static final boolean VERBOSE_CHORDING = false;
	static long chordTime = 0; 

	@SuppressWarnings("unused")
	private boolean findCycleHelper(int i, boolean[] visited, boolean[] inPath, ArrayDeque<Integer> path, Random r) {

		if (inPath[i]) {
			// made it back to "i" which we visited previously.
			// to get a cycle, trim off the stuff leading up to i.
			while (path.peekFirst() != i)
				path.pollFirst();
			
			if(!CHORD_MINIMIZE)
				return true;
			
			
			if(path.size() > 3) {
				if(Main_Load.TESTING && VERBOSE_CHORDING) {
					chordTime -= System.currentTimeMillis();
					System.out.println("Cycle is "+path);
				}
				
				HashSet<Integer> hsCycle = new HashSet<>(path);
				
				ArrayList<Integer> alCycle = new ArrayList<>();
				for(int v : path)
					alCycle.add(v);
				
				int N = alCycle.size();
				kLoop: for(int k=0; k<N; k++) {
					int vk = alCycle.get(k);
					for(int vkN : eList[vk]) {
						if(vkN == alCycle.get((k+1)%N))
							continue;
						if(hsCycle.contains(vkN)) {
							if(Main_Load.TESTING && VERBOSE_CHORDING)
								System.out.println("Chord found "+vk+" -> "+vkN);
							
							while(path.peekFirst() != vkN)
								path.addLast(path.pollFirst());//rotate vkN to front
							while(path.peekLast() != vk)
								path.pollLast();
							
							if(Main_Load.TESTING && VERBOSE_CHORDING)
								System.out.println("Reduced path is "+path);
							
							//continue loop
							hsCycle.clear();
							hsCycle.addAll(path);
							
							alCycle.clear();
							for(int v : path)
								alCycle.add(v);
							
							N = alCycle.size();
							k=0;
							continue kLoop;
						}
					}
				}

				if(Main_Load.TESTING && VERBOSE_CHORDING) {
					chordTime += System.currentTimeMillis();
					System.out.println("Done, chordTime = "+(chordTime*0.001)+"s");
				}
				
				return true;
			} else {
				if(Main_Load.TESTING && VERBOSE_CHORDING)
					System.out.println("Path is size 3");
				return true;
			}
		}

		if (visited[i])
			return false;

		visited[i] = true;
		inPath[i] = true;
		path.addLast(i);

		if(r != null) {
			//for randomness, do two passes where we might skip over things
			for (Integer c : eList[i]) {
				if(r.nextInt(4) == 0)
					continue;
				if (findCycleHelper(c, visited, inPath, path, r))
					return true;
			}
			for (Integer c : eList[i]) {
				if(r.nextInt(4) == 0)
					continue;
				if (findCycleHelper(c, visited, inPath, path, r))
					return true;
			}
			//everything visited gets marked visited. Our last pass makes sure we hit everything.
		}
		for (Integer c : eList[i]) {
			if (findCycleHelper(c, visited, inPath, path, r))
				return true;
		}

		inPath[i] = false;
		path.pollLast();
		return false;
	}

	ArrayDeque<Integer> findCycleDFS(Random r) {
		boolean[] visited = new boolean[N];
		boolean[] recStack = new boolean[N];
		ArrayDeque<Integer> path = new ArrayDeque<Integer>();

		int shift = (r==null?0:r.nextInt(N));
		for (int i = 0; i < N; i++)
			if (findCycleHelper((i+shift)%N, visited, recStack, path, r))
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
	
	//adds vNew many new vertices
	void expandBy(int vNew) {
		int Nnew = N + vNew;
		inDeg = Arrays.copyOf(inDeg, Nnew);
		outDeg = Arrays.copyOf(outDeg, Nnew);
		eList = Arrays.copyOf(eList, Nnew);
		backEList = Arrays.copyOf(backEList, Nnew);
		for(int i=N; i<Nnew; i++) {
			eList[i] = new HashSet<>();
			backEList[i] = new HashSet<>();
		}
		N += vNew;
	}
	
	String dumpS() {
		String res = "";
		res += "{";
		boolean firstRow = true;
		for(int i=0; i<N; i++) {
			if(outDeg[i] == 0)
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
