import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;

public class SCC {

	//Tarjan's algorithm for SCC
	//based on https://www.geeksforgeeks.org/tarjan-algorithm-find-strongly-connected-components/

	//The function to do DFS traversal.
	//It uses SCCUtil().
	
	//This array contains a list of SCCs (each given by a stack of vertex indices).
	//It is not always populated. A call to SCC() will try to populate it.
	//If SCC() returns true, then it found a split, and the component lists will be put in sccInfo.
	//If SCC() returns false, it will be cleared.
	
	ArrayList<ArrayList<Integer>> sccInfo = new ArrayList<>();

	boolean doSCC(Graph g) {
		return doSCC(ReducedGraph.wrap(g, true));
	}
	
	boolean doSCC(ReducedGraph g) {
		int SCC_time_temp;
		int[] disc = new int[g.N];
		int[] low = new int[g.N];
		for(int i = 0; i < g.N; i++) {
			disc[i] = -1;
			low[i] = -1;
		}

		boolean[] stackMember = new boolean[g.N];
		ArrayList<Integer> st = new ArrayList<Integer>();
		SCC_time_temp = 0;
		
		sccInfo.clear();
		
		boolean res = false;
		//used for recursive searching
		Stack<Integer> callstack = new Stack<>();
		Stack<Iterator<Integer>> iteratorStack = new Stack<>();
		Stack<Integer> iterNStack = new Stack<>();
		
		for (int i = 0; i < g.N; i++) {
			if(g.dropped[i])
				continue;
			if(disc[i] == -1) {
				callstack.push(i);
				
				callLoop: while(callstack.size() > 0) {
					int u = callstack.peek();
					
					disc[u] = SCC_time_temp;
					low[u] = SCC_time_temp;
					SCC_time_temp += 1;
					stackMember[u] = true;
					st.add(u);
					
					for(Iterator<Integer> iter = g.eList[u].iterator(); iter.hasNext(); ) {
						int n = iter.next();
						if (disc[n] == -1) {
							callstack.push(n);
							iteratorStack.push(iter);
							iterNStack.push(n);
							continue callLoop;
							
						} else if (stackMember[n] == true) {
							low[u] = Math.min(low[u], disc[n]);
						}
					}

					outBLoop: while(true) {
					breakingLoop: while(true) {
						u = callstack.peek();
						int w = -1;
						if (low[u] == disc[u]) {
							if(u==0 && st.size() == g.N-g.dropped_Size) {//found the whole darn graph
								break breakingLoop;
							}
							ArrayList<Integer> one_scc = new ArrayList<Integer>();
							while (w != u) {
								w = (int) st.remove(st.size()-1);
								one_scc.add(w);
								stackMember[w] = false;
							}
							if(one_scc.size() == g.N-g.dropped_Size) {
								break breakingLoop;
							}
							sccInfo.add(one_scc);
							res |= true;
						}
						break breakingLoop;
					}
					
					if(iterNStack.size() == 0) {
						callstack.pop();
						break callLoop;
					}

					Iterator<Integer> iter = iteratorStack.pop();
					callstack.pop();
					u = callstack.peek();
					{
						int n = iterNStack.pop();
						low[u] = Math.min(low[u], low[n]);
					}
					
					for(; iter.hasNext(); ) {
						int n = iter.next();
						if (disc[n] == -1) {
							callstack.push(n);
							iteratorStack.push(iter);
							iterNStack.push(n);
							continue callLoop;
							
						} else if (stackMember[n] == true) {
							low[u] = Math.min(low[u], disc[n]);
						}
					}
					continue outBLoop;
				}
				}
			}
		}
		
		return res;
	}
	
	//Given the SCC data, removes any edges between SCC's.
	int stripInterSCCEdges(ReducedGraph g) {
		int[] sccIDs = new int[g.N];
		int id = 1;
		for (ArrayList<Integer> comp : sccInfo) {
			for(Integer v : comp)
				sccIDs[v] = id;
			//increment
			id += 1;
		}
		
		int sccStripped = 0;
		for (int i = 0; i < g.N; i++) {
			if(g.dropped[i])
				continue;
			for(Iterator<Integer> jIter = g.eList[i].iterator();
					jIter.hasNext();
					) {
				int j = jIter.next();
				if(sccIDs[i] != sccIDs[j]) {
					jIter.remove();
					g.outDeg[i]--;
					g.backEList[j].remove(i);
					g.inDeg[j]--;
					sccStripped++;
				}
			}
		}
		if(Main_Load.TESTING)
			System.out.println("stripSCC removed "+sccStripped+" edges");
		return sccStripped;
	}
	
	int stripInterSCCEdges(Graph g) {
		return stripInterSCCEdges(ReducedGraph.wrap(g, true));
	}
	
}
