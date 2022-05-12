import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

//based on https://www.geeksforgeeks.org/articulation-points-or-cut-vertices-in-a-graph/
//takes in a directed graph, but processes all edges as bidirectional
public class WeakArticulationPoints {

	//returns the new "time"
	static int APUtil(Graph g, int u, boolean visited[], int disc[], int low[], int time, int parent,
			boolean isAP[]) {
		// Count of children in DFS Tree
		int children = 0;

		// Mark the current node as visited
		visited[u] = true;

		// Initialize discovery time and low value
		disc[u] = low[u] = ++time;

		// Go through all vertices adjacent to this
		for (int dir=0; dir<2; dir++) {
			//gotta go forward and back edges
			for (int v : (dir==0?g.eList:g.backEList)[u]) {
				// If v is not visited yet, then make it a child of u
				// in DFS tree and recur for it
				if (!visited[v]) {
					children++;
					time = APUtil(g, v, visited, disc, low, time, u, isAP);
	
					// Check if the subtree rooted with v has
					// a connection to one of the ancestors of u
					low[u] = Math.min(low[u], low[v]);
	
					// If u is not root and low value of one of
					// its child is more than discovery value of u.
					if (parent != -1 && low[v] >= disc[u])
						isAP[u] = true;
				}
	
				// Update low value of u for parent function calls.
				else if (v != parent)
					low[u] = Math.min(low[u], disc[v]);
			}
		}

		// If u is root of DFS tree and has two or more children.
		if (parent == -1 && children > 1)
			isAP[u] = true;
		return time;
	}

	//prints all articulat points
	static void AP(Graph g) {
		int V = g.N;
		
		boolean[] visited = new boolean[V];
		int[] disc = new int[V];
		int[] low = new int[V];
		boolean[] isAP = new boolean[V];
		int par = -1;

		// Adding this loop so that the
		// code works even if we are given
		// disconnected graph
		for (int u = 0; u < V; u++)
			if (!visited[u])
				APUtil(g, u, visited, disc, low, 0, par, isAP);

		// Printing the APs
		for (int u = 0; u < V; u++)
			if (isAP[u] == true)
				System.out.println("AP at " + u);
	}
	
	//returns the new "time" usually. If an AP was found, returns -(1+v).
	//instead of finding all APs just finds one.
	static int APUtil_One(Graph g, int u, boolean visited[], int disc[], int low[], int time, int parent) {
		// Count of children in DFS Tree
		int children = 0;

		// Mark the current node as visited
		visited[u] = true;

		// Initialize discovery time and low value
		disc[u] = low[u] = ++time;

		// Go through all vertices adjacent to this
		for (int dir=0; dir<2; dir++) {
			//gotta go forward and back edges
			for (int v : (dir==0?g.eList:g.backEList)[u]) {
				// If v is not visited yet, then make it a child of u
				// in DFS tree and recur for it
				if (!visited[v]) {
					children++;
					time = APUtil_One(g, v, visited, disc, low, time, u);
					if(time < 0)
						return time;//found a WAP, bubble
	
					// Check if the subtree rooted with v has
					// a connection to one of the ancestors of u
					low[u] = Math.min(low[u], low[v]);
	
					// If u is not root and low value of one of
					// its child is more than discovery value of u.
					if (parent != -1 && low[v] >= disc[u]) {
						return -(1+u);
					}
				}
	
				// Update low value of u for parent function calls.
				else if (v != parent)
					low[u] = Math.min(low[u], disc[v]);
			}
		}

		// If u is root of DFS tree and has two or more children.
		if (parent == -1 && children > 1) {
			return -(1+u);
		}
		return time;
	}
	
	//tries to find one articulation point, returns it if found.
	//otherwise returns -1.
	static int AP_One(Graph g) {
		int V = g.N;
		
		boolean[] visited = new boolean[V];
		int[] disc = new int[V];
		int[] low = new int[V];
		int par = -1;

		// Adding this loop so that the
		// code works even if we are given
		// disconnected graph
		for (int u = 0; u < V; u++)
			if (!visited[u]) {
				int res = APUtil_One(g, u, visited, disc, low, 0, par);
				if(res < 0) {
//					System.out.println("WAP Success");
//					System.out.println("Disc = "+Arrays.toString(disc));
//					System.out.println("Low = "+Arrays.toString(low));
					return -res-1;
				}
			}
		
//		System.out.println("WAP Failure");
//		System.out.println("Disc = "+Arrays.toString(disc));
//		System.out.println("Low = "+Arrays.toString(low));
		return -1;
	}
}
