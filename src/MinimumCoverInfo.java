

import java.util.ArrayList;

//Reduce the graph to a collection of cycles? This stores the reduced form.
//GraphChunks store the "remaining" graph.
//Optionally also stores heuristic solution.
public class MinimumCoverInfo {

	int N;
	ArrayList<int[]> pairList;
	ArrayList<int[]> bigCycleList;
	ArrayList<GraphChunk> chunks;
	ArrayList<Integer> heurSol;
	
	boolean[] inChunk;
	
	public MinimumCoverInfo(int _N, ArrayList<int[]> _pL,
			ArrayList<int[]> _bCL, ArrayList<GraphChunk> _c, ArrayList<Integer> _heurSol) {
		this.N = _N;
		this.pairList = _pL;
		this.bigCycleList = _bCL;
		this.chunks = _c;
		this.heurSol = _heurSol;
		
		this.inChunk = new boolean[N];
		for(GraphChunk ch : chunks) {
			for(int v : ch.mapping)
				inChunk[v] = true;
		}
	}
}
