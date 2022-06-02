import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
	
	//Find the vi that maps to v outside.
	//Returns -1 if not in the chunk.
	public int getPremapping(int v) {
		for(int i=0; i<mapping.length; i++) {
			if(mapping[i] == v)
				return i;
		}
		return -1;
	}
	
	//adds some vertices into the mapping, returns their premapped values.
	//will sort vs, and the returned new indices will map to the reordered order.
	public int[] addToMapping(int[] vs) {
		Arrays.sort(vs);
		int nvs = vs.length;
		int[] ret = new int[nvs];
		boolean[] alreadyIn = new boolean[nvs];
		
		//Check which vs (if any) are already in the mapping, and the needed growth.
		int newSize = mapping.length + nvs;
		for(int vi=0; vi<mapping.length; vi++) {
			int v = mapping[vi];
			int loc = Arrays.binarySearch(vs, vi);
			if(loc >= 0) {
				alreadyIn[loc] = true;
				ret[loc] = vi;
				newSize--;
			}
		}
		int growth = newSize - mapping.length;
		
		if(growth == 0) {
			return ret;
		}
		
		//Expand the graph
		gInner.expandBy(newSize - mapping.length);
		
		//Build the new mapping
		int[] newMapping = Arrays.copyOf(mapping, newSize);
		int dest = mapping.length;
		for(int vi=0; vi<nvs; vi++) {
			if(alreadyIn[vi])
				continue;
			int l = dest++;
			newMapping[l] = vs[vi];
			ret[vi] = l;
		}
		mapping = newMapping;
		
		return ret;
	}
	
	public static GraphChunk nonzeroVerts(Graph g, boolean destructive) {
		int reduced_n = g.nonZeroDegN();
		ArrayList<Integer> verts = new ArrayList<Integer>(reduced_n);
		for(int i=0; i<g.N; i++) {
			if(g.inDeg[i] > 0 || g.outDeg[i] > 0) {
				verts.add(i);
			}
		}
		return new GraphChunk(g, verts, destructive);
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
	
	public String toString() {
		if(gInner.E() < 150)
			return gInner.dumpS();
		return "GraphChunk{N="+gInner.N+",E="+gInner.E()+"}";
	}
}