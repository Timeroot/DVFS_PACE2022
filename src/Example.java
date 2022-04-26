import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;
import java.util.stream.Collectors;

public class Example {
	int N; //number of vertices
	HashSet<Integer>[] eList; //adjacency list
	HashSet<Integer>[] backEList; //backwards adjacency list (edge coming to a given vertex)
		
	//This array contains a list of SCCs (each given by a stack of vertex indices).
	//It is not always populated. A call to SCC() will try to populate it.
	//If SCC() returns true, then it found a split, and the component lists will be put in sccInfo.
	//If SCC() returns false, it will be left clear.
	ArrayList<ArrayList<Integer>> sccInfo = new ArrayList<>();
	
	//Original recursive implementation. Encountered stack overflows on graphs with > a few thousand vertices.
	/*
	//Below are some variables shared between SCC() and SCCUtil() for marking discovery times.
	private int SCC_time_temp;
	private int[] low, disc;
	private boolean[] stackMember;
	private ArrayList<Integer> st;
	boolean SCC_rec() {
		disc = new int[N];
		low = new int[N];
		for(int i = 0; i < N; i++) {
			disc[i] = -1;
			low[i] = -1;
		}

		stackMember= new boolean[N];
		st = new ArrayList<Integer>();
		SCC_time_temp = 0;
		
		sccInfo.clear();
		
		boolean res = false;
		for (int i = 0; i < N; i++) {
			if(disc[i] == -1) {
				res |= SCCUtil(i);
			}
		}
		
		//clear references
		disc = low = null;
		stackMember = null;
		st = null;

		return res;
	}
	boolean SCCUtil(int u) {
		disc[u] = SCC_time_temp;
		low[u] = SCC_time_temp;
		SCC_time_temp += 1;
		stackMember[u] = true;
		st.add(u);

		boolean res = false;
		for(int n : eList[u]) {
			if (disc[n] == -1) {
				res |= SCCUtil(n);
				low[u] = Math.min(low[u], low[n]);
			} else if (stackMember[n] == true) {
				low[u] = Math.min(low[u], disc[n]);
			}
		}

		int w = -1;
		if (low[u] == disc[u]) {
			if(u==0 && st.size() == N)//found the whole darn graph
				return false;
			ArrayList<Integer> one_scc = new ArrayList<Integer>();
			while (w != u) {
				w = (int) st.remove(st.size()-1);
				one_scc.add(w);
				stackMember[w] = false;
			}
			if(one_scc.size() == N) {
				return false;//found the whole darn graph
			}
			sccInfo.add(one_scc);
			res |= true;
		}
		return res;
	}*/
	
	//Nonrecursive implementation
	boolean SCC() {
		int SCC_time_temp;
		int[] disc = new int[N];
		int[] low = new int[N];
		for(int i = 0; i < N; i++) {
			disc[i] = -1;
			low[i] = -1;
		}

		boolean[] stackMember = new boolean[N];
		ArrayList<Integer> st = new ArrayList<Integer>();
		SCC_time_temp = 0;
		
		sccInfo.clear();
		
		boolean res = false;
		//used for recursive searching
		Stack<Integer> callstack = new Stack<>();
		Stack<Iterator<Integer>> iteratorStack = new Stack<>();
		Stack<Integer> iterNStack = new Stack<>();
		
		for (int i = 0; i < N; i++) {
			if(disc[i] == -1) {
				callstack.push(i);
				
				callLoop: while(callstack.size() > 0) {
					int u = callstack.peek();
					
					disc[u] = SCC_time_temp;
					low[u] = SCC_time_temp;
					SCC_time_temp += 1;
					stackMember[u] = true;
					st.add(u);
					
					for(Iterator<Integer> iter = eList[u].iterator(); iter.hasNext(); ) {
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
							if(u==0 && st.size() == N) {//found the whole darn graph
								break breakingLoop;
							}
							ArrayList<Integer> one_scc = new ArrayList<Integer>();
							while (w != u) {
								w = (int) st.remove(st.size()-1);
								one_scc.add(w);
								stackMember[w] = false;
							}
							if(one_scc.size() == N) {
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
}
