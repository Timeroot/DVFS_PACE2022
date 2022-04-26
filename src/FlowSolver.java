import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;

public class FlowSolver implements Solver {
	Graph g0;
	AGraph g;
	int N;
	int E_tot;
	int[] E_offset;
	double[] V_flow;
	double[] V_capacity;
	
	@Override
	public ArrayList<Integer> solve(Graph g0_) {
		g0 = g0_;
		init(new AGraph(g0));
		
		solveInternalLP();
		for(int r=0; r<25; r++) {
			shrinkCapacity(0.3);
			solveInternalLP();
		}
		dumpR();
		
		ArrayList<Integer> res = chooseUsingR();
		System.out.println(res);
		return res;
	}
	
	public static int flowHeuristic(ReducedGraph g_) {
		FlowSolver obj = new FlowSolver();
		g_.condense();
		obj.init(new AGraph(g_));

		obj.solveInternalLP();
		for(int r=0; r<6; r++) {
			obj.shrinkCapacity(0.6);
			obj.solveInternalLP();
		}
		
		double bestVal = -1;
		int bestVert = -1;
		for(int i=0; i<g_.N; i++) {
			double val = 1/obj.V_capacity[i];
			if(val > bestVal) {
				bestVal = val;
				bestVert = i;
			}
		}
		return bestVert;
	}
	
	void init(AGraph g) {
		this.g = g;
		N = g.N;
		
		E_tot = 0;
		E_offset = new int[N];//indices for edge-indexed data
		for(int i=0; i<N; i++) {
			E_offset[i] = E_tot;
			E_tot += g.outDeg[i];
		}

		V_flow = new double[N];
		V_capacity = new double[N];
		for(int i=0; i<N; i++) {
			V_capacity[i] = 1.0f;
		}
	}
	
	//rate should in [0.0, 1.0], exclusive.
	void shrinkCapacity(double rate) {
		for(int v=0; v<N; v++) {
			double cap = V_capacity[v];
			double flow = V_flow[v];
			double update = 1.0 - rate*(flow/cap);
			double new_cap = Math.min(1.0, cap*update);
			System.out.println(cap+", "+flow+" -> "+new_cap);
			
			V_capacity[v] = new_cap;
		}
	}
	
	ArrayList<Integer> chooseUsingR() {
		ReducedGraph rg = ReducedGraph.fromGraph(g0);
		
		while(rg.real_N() > 0) {

			System.out.println("Choosing from "+rg.real_N());
			
			//Choose based on highest degree-product vertex
			double bestVal = -1;
			int bestVert = -1;
			for(int i=0; i<rg.N; i++) {
				if(rg.dropped[i])
					continue;
				double val = (rg.real_N() > 10 ? 1/V_capacity[rg.invMap[i]] : (1+rg.inDeg[i]) * (1+rg.outDeg[i]));
				if(val > bestVal) {
					bestVal = val;
					bestVert = i;
				}
			}
			if(bestVert == -1)
				throw new RuntimeException("No choice?");
			
			int v0 = bestVert;
			System.out.println("Take out "+v0+", score="+bestVal);
			
			//Just add a self-loop. The pruner will add it to the FVS and go from there!
			rg.eList[v0].add(v0); rg.backEList[v0].add(v0); rg.inDeg[v0]++; rg.outDeg[v0]++;
			rg.prune();
		}
		
		return rg.transformSolution(new ArrayList<Integer>());
	}
	
	static final boolean USE_QUADRATIC = true;
	static final boolean ECHO = true;
	
	//Finds maximal flow given the vertex capacities.
	void solveInternalLP() {
		
		String lpName = "/tmp/circuit.lp";
		String outName = "/tmp/circuit.out";
		PrintStream scipProblem;
		try {
			scipProblem = new PrintStream(new FileOutputStream(lpName));
		} catch (FileNotFoundException e2) {
			throw new RuntimeException("Couldn't open SCIP LP for writing", e2);
		}
		
		//lp vars: ef (Eflow), vf (Vflow).
		
		//Objective
		scipProblem.println("Minimize");
		for(int e=0; e<E_tot; e++) {
			if(USE_QUADRATIC)
				scipProblem.print((e==0?"":" + ")+" [nf"+e+"^2 ]/2 ");
			else
				scipProblem.print("  -ef"+e+" ");
		}
		scipProblem.println();
		
		//Constraints
		scipProblem.println("st");{//such that
			//Conservation of vertex out
			for(int v=0; v<N; v++) {
				scipProblem.print("-vf"+v);
				for(int ei = 0; ei < g.outDeg[v]; ei++) {
					int e = E_offset[v] + ei;
					scipProblem.print("+ ef"+e);
				}
				scipProblem.println(" == 0");
			}
			
			//Conservation of vertex in
			for(int v=0; v<N; v++) {
				scipProblem.print("-vf"+v);
				for(int vi : g.backEList[v]) {
					int ei = Arrays.binarySearch(g.eList[vi], v);
					int e = E_offset[vi] + ei;
					scipProblem.print("+ ef"+e);
				}
				scipProblem.println(" == 0");
			}
			
			//Capacity is properly capped
			for(int v=0; v<N; v++) {
				scipProblem.println("vf"+v+" <= "+V_capacity[v]);
			}
			//And for each edge, a capacity
			for(int vi=0; vi<N; vi++) {
				for(int ei = 0; ei < g.outDeg[vi]; ei++) {
					int e = E_offset[vi] + ei;
					int vo = g.eList[vi][ei];
					double e_cap = Math.min(V_capacity[vi] / g.outDeg[vi], V_capacity[vo] / g.inDeg[vo]);
					scipProblem.println("ef <= "+e_cap);
					
					//Residual flow
					scipProblem.println("nf"+e+" + ef"+e+" == "+e_cap);
				}
			}
			
			
		}
		
		scipProblem.close();
		
		//Invoke SCIP
		Process scipProc;
		try {
			ProcessBuilder pb = new ProcessBuilder(
					"./extra_bin/scip",
					"-c", "read "+lpName,
					"-c", "set limits time 10",
					"-c", "optimize",
					"-c", "write solution "+outName,
					"-c", "quit"
					);
			if(ECHO)
					pb = pb.inheritIO();
			scipProc = pb.start();
		} catch (IOException e1) {
			throw new RuntimeException("SCIP Failed to launch.", e1);
		}
		try {
			int retcode = scipProc.waitFor(); 
			if(retcode != 0) {
				throw new RuntimeException("SCIP gave retcode "+retcode+" for "+lpName);
			}
		} catch (InterruptedException e) {
			System.err.println("Interrupted while waiting for SCIP:");
			e.printStackTrace();
		}
		
		//Read in solution from SCIP
		try {
			BufferedReader scipSolReader = new BufferedReader(new FileReader(outName));
			//skip first two lines
			String line1 = scipSolReader.readLine();
			if(line1.contentEquals("solution status: infeasible")) {
				throw new RuntimeException("SCIP Reported infeasible problem in "+lpName);
			} else if(line1.contentEquals("solution status: optimal solution found")) {
//					scip_status = SCIP_STATUS.OPTIMAL;
			} else if(line1.contentEquals("solution status: node limit reached")) {
//					scip_status = SCIP_STATUS.NODE_LIMIT;
			} else  if(line1.contentEquals("solution status: time limit reached")) {
//					scip_status = SCIP_STATUS.TIME_LIMIT;
			} else {
				System.out.println("SCIP gave "+line1);
				throw new RuntimeException();
			}
			scipSolReader.readLine();
			
			//Save the variables
			for(String line = scipSolReader.readLine(); line != null; line = scipSolReader.readLine()) {
				System.out.println("LINE=["+line+"]");
				String[] parts = line.split(" ");
				if(parts[0].equals("quadobjvar"))
					continue;
				String type = parts[0].substring(0,2);
				int ind = Integer.valueOf(parts[0].substring(2));
				
				String valStr = parts[parts.length-2];
				double val = (valStr.equals("+infinity") ? Double.MAX_VALUE : Double.valueOf(valStr));
				switch(type) {
				case "ef":
//					System.out.println("ind = "+ind);
					int eSrc = Arrays.binarySearch(E_offset, ind);
//					System.out.println("eSrc = "+eSrc);
					if(eSrc < 0)
						eSrc = -(eSrc+2);
//					System.out.println("eSrc = "+eSrc+", E_off = "+E_offset[eSrc]);
					int eDst = g.eList[eSrc][ind-E_offset[eSrc]];
//					System.out.println("EFlow "+ind+" is "+val+", "+eSrc+"->"+eDst);
					System.out.println(eSrc+"->"+eDst);
//					Eflow[ind] = val; 
					break;
				case "vf":
					V_flow[ind] = val;
					break;
				case "nf":
					break;//residual flow, skip
				default:
					throw new RuntimeException("Couldn't understand line "+line);
				}
			}

		} catch (IOException e) {
			throw new RuntimeException("Couldn't open SCIP LP for writing", e);
		}
	}
	
	void dumpR() {
		for(int v=0; v<N; v++) {
			System.out.println("Vcap["+v+"] = "+(float)V_capacity[v]+",\tV_flow["+v+"] = "+(float)V_flow[v]);
		}
	}

	@Override
	public ArrayList<Integer> solve(ReducedGraph g) {
		throw new RuntimeException();
	}

}
