import java.util.ArrayList;
import java.util.HashSet;

public class VertexCover {
	//tries to solve vertex cover problems using some branching rules,
	//especially those described at https://epubs.siam.org/doi/pdf/10.1137/1.9781611976472.13
	
	//stores edges as Longs: 32bits for the first vertex, 32 for the second vertex.
	public static void solve(ArrayList<int[]> _edges, int N) {
		int[] deg = new int[N];
		int E = _edges.size();
		HashSet<Long> edges = new HashSet<Long>();
		
		for(int i=0; i<E; i++) {
			int[] edge = _edges.get(i);
			int a = edge[0], b = edge[1];
			long ab = toEdgeAB(a,b);
			edges.add(ab);
			deg[a]++; deg[b]++;
		}
	}
	
	//get/set edges from the packed long format.
	static int edgeA(long l) {
		return (int) (l&0xFFFFFFFFL);
	}
	static int edgeB(long l) {
		return (int) ((l>>32)&0xFFFFFFFFL);
	}
	static long toEdgeAB(long a, long b) {
		return (a<<32) | b;
	}
}
