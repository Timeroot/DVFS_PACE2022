

import java.util.ArrayList;

//If you reduce the DVFS graph to a collection of cycles that you need to cover,
//then this stores the reduced form. Also uses GraphChunks to store any remaining
//graph that couldn't be nicely represented using cycles. The hope is that this
//is relatively small.
//
//MinimumCoverInfo can optionally also store a heuristic solution to help the ILP.
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
		
		if(this.bigCycleList == null)
			this.bigCycleList = new ArrayList<>();
		
		if(this.chunks == null)
			this.chunks = new ArrayList<>();
		
		this.inChunk = new boolean[N];
		for(GraphChunk ch : chunks) {
			for(int v : ch.mapping)
				inChunk[v] = true;
		}
	}
}
