import java.util.Arrays;

public class GreedyHeuristics {

	//Choose based on highest degree-product vertex
	static final float DEG_SHIFT = 1.1f;
	public static int degreeHeuristic(ReducedGraph g) {
		float bestVal = -1;
		int bestVert = -1;
		for(int i=0; i<g.N; i++) {
			if(g.dropped[i])
				continue;
			float val = (DEG_SHIFT+g.inDeg[i]) * (DEG_SHIFT+g.outDeg[i]);
			if(val > bestVal) {
				bestVal = val;
				bestVert = i;
			}
		}
		if(bestVert == -1)
			throw new RuntimeException("No choice?");
		return bestVert;
	}
	
	//Choose based on some eigenvalue
	static final float EIGEN_SELF_WEIGHT = 0.1f;
	static final boolean NORMALIZE_OUTDEGREE = true;
	public static int eigenHeuristic(ReducedGraph g) {
		
		float[] vPrev = new float[g.N];
		float[] vNew = new float[g.N];
		//start with the constant vector
		int V = g.N - g.dropped_Size;
		for(int i=0; i<g.N; i++) {
			vPrev[i] = 1.0f/V;
		}
		//do a ... few? iterations
		for(int iter=0; iter<9; iter++) {
			if(NORMALIZE_OUTDEGREE) {
				for(int i=0; i<g.N; i++) {
//					vPrev[i] /= Math.sqrt(g.outDeg[i]);
//					vPrev[i] /= Math.sqrt(g.inDeg[i]);
//					vPrev[i] /= g.inDeg[i];
					vPrev[i] /= g.outDeg[i];
				}
			}
			
			for(int i=0; i<g.N; i++) {
				if(g.dropped[i])
					continue;
				float acc = EIGEN_SELF_WEIGHT * vPrev[i];
				for(int i_prev : g.backEList[i]) {
					float val_prev = vPrev[i_prev];
					acc += val_prev;
				}
				vNew[i] = acc;
			}
			//swap
			float[] tmp = vNew;
			vNew = vPrev;
			vPrev = tmp;
		}
		
		float bestVal = -1;
		int bestVert = -1;
		for(int i=0; i<g.N; i++) {
			if(g.dropped[i])
				continue;
			float val = vPrev[i];
			if(val > bestVal) {
				bestVal = val;
				bestVert = i;
			}
		}
		if(bestVert == -1)
			throw new RuntimeException("No choice?");
		return bestVert;
	}
	
	//Compute a Sinkhorn-balance score
	//based on the technique at https://math.nist.gov/mcsd/Seminars/2014/2014-06-10-Shook-presentation.pdf
	public static int sinkhornHeuristic(ReducedGraph g, int max_iter) {
		//We'll keep a number for each edge (+ a self-edge for each vertex)
		//That will be in a float array.
		//To index edge -> array quickly, we'll keep track of an array offset for each vertex.
		//the kth edge from vi (to some other vo) is at index E_offset[vi+1+k].
		//The self edge from vi to vi is at index E_offset[vi].
		int E_tot = 0;
		int[] E_offset = new int[g.N];
		for(int i=0; i<g.N; i++) {
			E_offset[i] = E_tot;
			E_tot += 1 + g.outDeg[i];
		}
		float[] E_value = new float[E_tot];
		for(int i=0; i<E_tot; i++) {
			E_value[i] = 1.0f;
		}
		for(int vi=0; vi<g.N; vi++) {
			if(g.dropped[vi])
				continue;
			final int offset = E_offset[vi];
			
			int e=1;
			E_value[offset] = 0.6f;
			for(int vo : g.eList[vi]) {
				E_value[offset+e] = 1.0f; // / (float)Math.pow((1 + g.outDeg[vi] ) * (1 + g.inDeg[vo]), 0.2);
				e++;
			}
		}
		
		//When normalizing the indegree, this is where we aggregate the values
		float[] inflow = new float[g.N];
		
		for(int iter=0; iter<max_iter; iter++) {
			//Zero out inflow if needed
			if(iter > 0) {
				for(int i=0; i<g.N; i++)
					inflow[i] = 0;
			}
			
			//Normalize the out degree
			for(int vi=0; vi<g.N; vi++) {
				if(g.dropped[vi])
					continue;
				float acc=0;
				final int offset = E_offset[vi];
				for(int e=0; e <= g.outDeg[vi]; e++) {
					acc += E_value[offset+e];
				}
				final float norm = (float)(Math.pow(1/acc, 1.5f));
				
				E_value[offset] *= norm;
				inflow[vi] += E_value[offset];//self inflow
				
				int e=1;
				for(int vo : g.eList[vi]) {
					float val = E_value[offset+e];
					val *= norm;
					E_value[offset+e] = val;//write back
					inflow[vo] += val;//write to the destination
					e++;
				}
			}
			
			//Normalize the in degree
			for(int vi=0; vi<g.N; vi++) {
				if(g.dropped[vi])
					continue;
				
				int offset = E_offset[vi];
				if(iter == 0 || inflow[vi] > 1.1 || inflow[vi] < 0.9)
					E_value[offset] /= inflow[vi] * Math.sqrt(inflow[vi]);//inflow[vi];//(Math.pow(inflow[vi], 1.5f));
				else
					E_value[offset] *= 4.375f - 5.25f*inflow[vi] + 1.875*inflow[vi]*inflow[vi];
				
				if(iter == max_iter - 1)
					continue;
				
				int e=1;
				for(int vo : g.eList[vi]) {
					float inf = inflow[vo];
					if(iter == 0 || inf > 1.1 || inf < 0.9)
						E_value[offset+e] /= inf * Math.sqrt(inf);//inf;//(Math.pow(inf, 1.5f));
					else
						E_value[offset+e] *= 4.375f - 5.25f*inf + 1.875*inf*inf;
					e++;
				}
			}
		}

		
		float bestVal = 2;
		int bestVert = -1;
		for(int i=0; i<g.N; i++) {
			if(g.dropped[i])
				continue;
			float val = E_value[E_offset[i]];
			if(val < bestVal) {
				bestVal = val;
				bestVert = i;
			}
		}
		if(bestVert == -1)
			throw new RuntimeException("No choice?");
		return bestVert;
	}
}
