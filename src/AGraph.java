import java.util.Arrays;
import java.util.HashSet;

//Like a "Graph", but instead of using HashSet<Integer> for adjacency lists (for fast edge membership),
//it uses arrays (for fast iteration and easier indexing). The arrays are sorted so that Arrays.binarySearch
//can still be used for decent edge membership checking.
public class AGraph {
	int N;
	int[][] eList;
	int[][] backEList;
	int[] inDeg, outDeg;
	
	public AGraph(int N_, HashSet<Integer>[] eList_, HashSet<Integer>[] backEList_,
			int[] inDeg_, int[] outDeg_) {
		this(N_, new int[N_][], new int[N_][], inDeg_, outDeg_);
		for(int i=0; i<N_; i++) {
			eList[i] = new int[eList_[i].size()];
			int s=0;
			for(Integer v : eList_[i])
				eList[i][s++] = v;
			
			backEList[i] = new int[backEList_[i].size()];
			s=0;
			for(Integer v : backEList_[i])
				backEList[i][s++] = v;
			
			Arrays.sort(eList[i]);
			Arrays.sort(backEList[i]);
		}
	}
	
	public AGraph(int N_, int[][] eList_, int[][] backEList_,
			int[] inDeg_, int[] outDeg_) {
		N = N_;
		eList = eList_;
		backEList = backEList_;
		inDeg = inDeg_;
		outDeg = outDeg_;
	}
	
	public AGraph(Graph g) {
		this(g.N, g.eList, g.backEList, g.inDeg, g.outDeg);
	}
	
	public AGraph(ReducedGraph g) {
		this(g.N, g.eList, g.backEList, g.inDeg, g.outDeg);
	}
	
	AGraph copy() {
		int[][] newEList = new int[N][];
		int[][] newBackEList = new int[N][];
		for(int i=0; i<N; i++) {
			newEList[i] = Arrays.copyOf(eList[i], eList[i].length);
			newBackEList[i] = Arrays.copyOf(backEList[i], backEList[i].length);
		}
		return new AGraph(N, newEList, newBackEList, Arrays.copyOf(inDeg,N), Arrays.copyOf(outDeg,N));
	}
}
