import java.util.ArrayList;
import java.util.Stack;

public class StrongArticulationPoints {

	ReducedGraph rg;
	static void articulations(ReducedGraph rg) {
		StrongArticulationPoints sap = new StrongArticulationPoints();
		sap.rg = rg;
		int n = rg.N;
		sap.dfs_numbering = new int[n];
		sap.reverse_dfn_numbering = new int[n];
		sap.dfs_parent = new int[n];
		sap.best = new int[n];
		sap.idom = new int[n];
		sap.sdom = new int[n];
		
		int r = 0;
		while(rg.dropped[r])
			r++;
		System.out.println("Root = "+r);
		sap.semiNca(n, r);
	}

	int[] dfs_numbering;
	int[] reverse_dfn_numbering;
	int[] dfs_parent;
	int[] best;
	
	int[] idom, sdom;

	int tick;

	void dfs(int start) {
		Stack<Integer> todo = new Stack<>();
		todo.push(start);
		
		while(!todo.isEmpty()) {
			int u = todo.pop();
			if(dfs_numbering[u] > 0)
				continue;
			
			dfs_numbering[u] = tick;
			reverse_dfn_numbering[tick++] = u;
			for (int v : rg.eList[u]) {
				if (dfs_numbering[v] < 0) {
					dfs_parent[v] = u;
					todo.push(v);
				}
			}
		}
	}

	int eval(int v, int cur) {
		if (dfs_numbering[v] <= cur)
			return v;
		int u = dfs_parent[v], r = eval(u, cur);
		if (dfs_numbering[best[u]] < dfs_numbering[best[v]])
			best[v] = best[u];
		return dfs_parent[v] = r;
	}
	
	void dumpDFS() {
		System.out.println("DFS DUMP: ");
		for(int v=0; v<rg.N; v++) {
			System.out.println("dfs["+v+"]="+dfs_numbering[v]+", rdfs["+v+"]="+reverse_dfn_numbering[v]+", par[v]="+dfs_parent[v]);
		}
	}
	
	void dumpIDom() {
		System.out.println("IDOM DUMP: {");
		for(int v=0; v<rg.N; v++) {
//			System.out.println("v="+v+": "+idom[v]);
			System.out.print((v>0?",":"")+(1+idom[v])+"->"+(1+v));
		}
		System.out.println();
		System.out.println("}");
	}

	void semiNca(int n, int r) {
//		fill_n(idom, n, -1); // delete if unreachable nodes are not needed
		
		for(int i=0; i<n; i++) {
			dfs_numbering[i] = -1;
			best[i] = i;
		}

		tick = 0;
		dfs(r);
//		System.out.println("Final tick="+tick+", n="+n);
		dumpDFS();
		
		for (int i = tick; --i>0; ) {
			int v = reverse_dfn_numbering[i];
//			System.out.println("Loop i="+i+", v="+v+", opts="+new ArrayList(rg.backEList[v]));
			sdom[v] = v;
			for (int u : rg.backEList[v]) {
//				System.out.println("now u="+u+", dfn[u]="+dfs_numbering[u]);
//				if (dfs_numbering[u] != -1) {
//					System.out.println("Eval u="+u+", v="+v+", tick: i="+i);
					eval(u, i);
					if (dfs_numbering[best[u]] < dfs_numbering[sdom[v]])
						sdom[v] = best[u];
//				}
			}
			best[v] = sdom[v];
			idom[v] = dfs_parent[v];
		}
		for (int i = 1; i < tick; i++) {
			int v = reverse_dfn_numbering[i];
			while (dfs_numbering[idom[v]] > dfs_numbering[sdom[v]])
				idom[v] = idom[idom[v]];
		}
		dumpIDom();
	}

	// based on http://maskray.me/blog/2020-12-11-dominator-tree
/*
6 7
2
3 4 6
5
5
2



7 15
2 3
3
1 4 6 7
5
2 4 7
5 7
5 6
	 */
}
