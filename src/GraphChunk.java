import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

//Captures a small subgraph, together with data about its inclusion in the original. 
public class GraphChunk {
	static int biggestChunk = -1;
	
	int[] mapping;
	Graph gInner;
	
	@SuppressWarnings("unchecked")
	public GraphChunk(Graph g, ArrayList<Integer> verts, boolean destructive) {
		verts.sort(Integer::compare);
		
		int N = verts.size();
		mapping = new int[N];
		for(int i=0; i<N; i++)
			mapping[i] = verts.get(i);
		HashSet<Integer>[] eList = new HashSet[N];
		HashSet<Integer>[] backEList = new HashSet[N];
		for(int i=0; i<N; i++) {
			eList[i] = new HashSet<Integer>();
			backEList[i] = new HashSet<Integer>();
		}

		for(int i=0; i<N; i++) {
			HashSet<Integer> eL = eList[i];
			int iOrig = mapping[i];
			
			for(Iterator<Integer> jOrigIter = g.eList[iOrig].iterator();
					jOrigIter.hasNext();
					) {
				int jOrig = jOrigIter.next();
				
				int j = Arrays.binarySearch(mapping, jOrig);
				if(j < 0)
					continue;//not in subgraph
				eL.add(j);
				backEList[j].add(i);

				if(destructive) {
//						g.eList[iOrig].remove(jOrig);
					jOrigIter.remove();
					
					g.outDeg[iOrig]--;
					g.backEList[jOrig].remove(iOrig);
					g.inDeg[jOrig]--;
				}
			}
		}
		
		gInner = new Graph(N, eList, backEList);
		g.checkConsistency();
		gInner.checkConsistency();
		biggestChunk = Math.max(biggestChunk, N);
//			if(Main_Load.TESTING)
//				System.out.println("Made chunk of size "+N+", record "+biggestChunk);
	}
	
	private GraphChunk(Graph g) {
		this.gInner = g;
		this.mapping = new int[g.N];
		for(int i=0; i<g.N; i++) {
			mapping[i] = i;
		}
	}
	public static GraphChunk wrap(Graph g) {
		return new GraphChunk(g);
	}
	
	//adds edges in this subgraph back into parent graph
	void addInto(Graph g) {
		for(int i=0; i<gInner.N; i++) {
			int iOrig = mapping[i];
			
			for(int j : gInner.eList[i]) {
				int jOrig = mapping[j];
				g.eList[iOrig].add(jOrig);
			}
			g.outDeg[iOrig] = g.eList[iOrig].size();
			
			for(int j : gInner.backEList[i]) {
				int jOrig = mapping[j];
				g.backEList[iOrig].add(jOrig);
			}
			g.inDeg[iOrig] = g.backEList[iOrig].size();
		}
	}

	
	//copies this induced subgraph back into parent graph
	void copyInto(Graph g) {
		//Clear out old edges
		for(int i=0; i<gInner.N; i++) {
			int iOrig = mapping[i];				
			for(int jOrig : g.eList[iOrig]) {
				if(Arrays.binarySearch(mapping, jOrig) >= 0) {
					//responsibility of this induced subgraph
					g.eList[iOrig].remove(jOrig);
				}
			}
			for(int jOrig : g.backEList[iOrig]) {
				if(Arrays.binarySearch(mapping, jOrig) >= 0) {
					//responsibility of this induced subgraph
					g.backEList[iOrig].remove(jOrig);
				}
			}
		}
		addInto(g);
	}
}