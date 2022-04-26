import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Collectors;

public class PruneReduced {
	
	static final boolean VERY_VERBOSE = false;
	
	static void prune(ReducedGraph g) {
		
		int N=g.N;
		HashSet<Integer>[] eList = g.eList;
		HashSet<Integer>[] backEList = g.backEList;
		int[] inDeg = g.inDeg, outDeg = g.outDeg;
		int[] invMap = g.invMap;
		ArrayList<Integer> mustFVS = g.mustFVS;
		boolean[] dropped = g.dropped;

		//Drop anything with indegree *or* outdegree 1 or less.
		//And when you've done that, repeat.
		ArrayDeque<Integer> toDrop = new ArrayDeque<Integer>();
		for(int i=0; i<N; i++) {
			if(!dropped[i] && (inDeg[i] <= 1 || outDeg[i] <= 1 || eList[i].contains(i))) {
				toDrop.add(i);
			}
		}
		if(VERY_VERBOSE) System.out.println("Initial list "+toDrop);
		while(toDrop.size() > 0) {
			int ni = toDrop.pollFirst();
			if(VERY_VERBOSE) System.out.println("Pop "+ni);
			if(dropped[ni])
				continue;
			
			if(VERY_VERBOSE) System.out.println("Work with "+ni);
			
			//Self loop, must keep.
			if(eList[ni].contains(ni)) {
				if(VERY_VERBOSE) System.out.println("Selfloop "+ni);
//				if(mustFVS[invMap[ni]]) {
//					throw new RuntimeException(""+ni);
//				}
				mustFVS.add(invMap[ni]);
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
				if(VERY_VERBOSE) System.out.println("In0 "+ni);
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
				if(VERY_VERBOSE) System.out.println("Out0 "+ni);
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
				if(VERY_VERBOSE) System.out.println("In1 "+ni);
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
				if(VERY_VERBOSE) System.out.println("Out1 "+ni);
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
				if(VERY_VERBOSE) System.out.println("Skip "+ni);
//				System.out.println(ni+" no longer valid to prune: "+inDeg[ni]+", "+outDeg[ni]);
				continue;
			}
			dropped[ni] = true;
			g.dropped_Size++;
			g.checkConsistency();
		}
		
		//Final check that we didn't miss anything
		if(ReducedGraph.CHECK) {
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
		
		g.check_condense();
	}
	
}
