import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;

public class PruneLowdeg {
	
	Graph g;
	public PruneLowdeg(Graph g_) {
		g = g_;
		prune();
	}
	
	//Reduce the graph by pruning low-degree vertices.
	//Changes the N and E values (and everything else), and just saves a vertex map.
	//If pruneRemap[i] == -1, then it's not in the core. (Check mustFVS). Otherwise, it's got the index
	//of the "simplified" graph ID to check.
	int[] invMap;
	boolean[] mustFVS;
	
	int Norig;
	
	void prune() {
		
		int N=g.N;
		HashSet<Integer>[] eList = g.eList;
		HashSet<Integer>[] backEList = g.backEList;
		int[] inDeg = g.inDeg, outDeg = g.outDeg;

		mustFVS = new boolean[N];
		
		//Drop anything with indegree *or* outdegree 1 or less.
		//And when you've done that, repeat.
		ArrayDeque<Integer> toDrop = new ArrayDeque<Integer>();
		boolean[] dropped = new boolean[N];
		for(int i=0; i<N; i++) {
			if(inDeg[i] <= 1 || outDeg[i] <= 1 || eList[i].contains(i)) {
				toDrop.add(i);
			}
		}
		
		if(toDrop.size() == 0) {
			invMap = new int[N];
			for(int i=0; i<N; i++)
				invMap[i] = i;
			mustFVS = new boolean[N];
			Norig = N;
			return;
		}
		
		while(toDrop.size() > 0) {
			int ni = toDrop.pollFirst();
			if(dropped[ni])
				continue;
			
			//Self loop, must keep.
			if(eList[ni].contains(ni)) {
				mustFVS[ni] = true;
				//Remove it from the graph.
				for(int no : eList[ni]) {
					inDeg[no]--;
					backEList[no].remove(ni);
					if(inDeg[no] <= 1) {
						toDrop.add(no);
					}
				}
				for(int no : backEList[ni]) {
					outDeg[no]--;
					eList[no].remove(ni);
					if(outDeg[no] <= 1) {
						toDrop.add(no);
					}
				}
				outDeg[ni] = inDeg[ni] = 0;
				eList[ni].clear();
				backEList[ni].clear();
				
			} else if(inDeg[ni] == 0) {
				//remove it completely from the graph
				for(int no : eList[ni]) {
					inDeg[no]--;
					backEList[no].remove(ni);
					if(inDeg[no] <= 1) {
						toDrop.add(no);
					}
				}
				outDeg[ni] = 0;
				eList[ni].clear();
				
			} else if(outDeg[ni] == 0) {
				//Remove it completely from the graph
				for(int no : backEList[ni]) {
					outDeg[no]--;
					eList[no].remove(ni);
					if(outDeg[no] <= 1) {
						toDrop.add(no);
					}
				}
				inDeg[ni] = 0;
				backEList[ni].clear();
				
			} else if(inDeg[ni] == 1) {
				//stitch the predecessor to all successors
				int pred = -1;
				for(int npre : backEList[ni]) {
					if(!dropped[npre]) {
						pred = npre; break;
					}
				}
				if(pred == -1)
					throw new RuntimeException("Error while pruning (1)"+ni);
				eList[pred].addAll(eList[ni]);
				eList[pred].remove(ni);
				outDeg[pred] = eList[pred].size();
				if(outDeg[pred] <= 1 || eList[pred].contains(pred)) {
					toDrop.add(pred);
				}
				
				for(int no : eList[ni]) {
					backEList[no].remove(ni);
					backEList[no].add(pred);
					inDeg[no] = backEList[no].size();
					if(inDeg[no] <= 1 || backEList[no].contains(no)) {
						toDrop.add(no);
					}
				}
				inDeg[ni] = outDeg[ni] = 0;
				eList[ni].clear();
				backEList[ni].clear();
				
			} else if(outDeg[ni] == 1) {
				//stitch the successor to all predecessors
				int succ = -1;
				for(int nsucc : eList[ni]) {
					if(!dropped[nsucc]) {
						succ = nsucc; break;
					}
				}
				if(succ == -1)
					throw new RuntimeException("Error while pruning (6)"+ni);
				backEList[succ].addAll(backEList[ni]);
				backEList[succ].remove(ni);
				inDeg[succ] = backEList[succ].size();
				if(inDeg[succ] <= 1 || backEList[succ].contains(succ)) {
					toDrop.add(succ);
				}
				
				for(int no : backEList[ni]) {
					eList[no].remove(ni);
					eList[no].add(succ);
					outDeg[no] = eList[no].size();
					if(outDeg[no] <= 1 || eList[no].contains(no)) {
						toDrop.add(no);
					}
				}
				inDeg[ni] = outDeg[ni] = 0;
				eList[ni].clear();
				backEList[ni].clear();
				
			} else {
//				System.out.println(ni+" no longer valid to prune: "+inDeg[ni]+", "+outDeg[ni]);
				continue;
			}
			dropped[ni] = true;
			g.checkConsistency();
		}
		
		//Final check that we didn't miss anything
		if(Graph.CHECK) {
			for(int i=0; i<N; i++) {
				if(dropped[i])
					continue;
				for(int no : eList[i]) {
					if(dropped[no])
						throw new RuntimeException("Error while pruning (7)"+i+"/"+no);
					if(!backEList[no].contains(i))
						throw new RuntimeException("Error while pruning (9)"+i+"/"+no);
				}
				for(int no : backEList[i]) {
					if(dropped[no])
						throw new RuntimeException("Error while pruning (8)"+i+"/"+no);
					if(!eList[no].contains(i))
						throw new RuntimeException("Error while pruning (10)"+i+"/"+no);
				}
				if(inDeg[i]<=1 || outDeg[i]<=1 || eList[i].contains(i))
					throw new RuntimeException("Error while pruning (11)"+i+": "+inDeg[i]+", "+outDeg[i]);
			}
		}
		
		//Re-index!
		int[] pruneRemap = new int[N];
		int di = 0;
		for(int i=0; i<N; i++) {
			if(dropped[i]) {
				pruneRemap[i] = -1;
			} else {
				pruneRemap[i] = di++;
			}
		}
		Norig = N;
		N = g.N = di;
//		System.out.println("Pruned from "+Norig+" to N="+N);
		
		invMap = new int[N];
		di = 0;
		for(int i=0; i<Norig; i++) {
			if(dropped[i]) {
			} else {
				invMap[di++] = i;
			}
		}
		
		//Edit eList and backEList
		
		HashSet<Integer>[] newEList = new HashSet[N];
		HashSet<Integer>[] newBackEList = new HashSet[N];
		for(int i=0; i<N; i++) {
			int oi = invMap[i];
			HashSet<Integer> li = new HashSet<Integer>();
			for(int ono : eList[oi])
				li.add(pruneRemap[ono]);
			newEList[i] = li;
			HashSet<Integer> bli = new HashSet<Integer>();
			for(int ono : backEList[oi])
				bli.add(pruneRemap[ono]);
			newBackEList[i] = bli;
		}
		g.eList = eList = newEList;
		g.backEList = backEList = newBackEList;
		
		g.inDeg = inDeg = new int[N];
		g.outDeg = outDeg = new int[N];
		for(int i=0; i<N; i++) {
			inDeg[i] = backEList[i].size();
			outDeg[i] = eList[i].size();
		}
	}
	
	void transformSolution(ArrayList<Integer> reducedSol){
		if(reducedSol == null)
			return;
		
		for(int i=0; i<reducedSol.size(); i++) {
			int v = reducedSol.get(i);
			reducedSol.set(i, invMap[v]);
		}
		for(int i=0; i<Norig; i++)
			if(mustFVS[i])
				reducedSol.add(i);
	}
	
}
