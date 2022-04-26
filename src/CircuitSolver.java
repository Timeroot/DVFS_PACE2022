import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;

public class CircuitSolver implements Solver {
	Graph g0;
	AGraph g;
	int N;
	int E_tot;
	int[] E_offset;
	double[] Eflow, U_in, R, U_out, Vflow;
	
	@Override
	public ArrayList<Integer> solve(Graph g0_) {
		g0 = g0_;
		g = new AGraph(g0);
		N = g.N;
		
		E_tot = 0;
		E_offset = new int[N];//indices for edge-indexed data
		for(int i=0; i<N; i++) {
			E_offset[i] = E_tot;
			E_tot += g.outDeg[i];
		}
		
		Eflow = new double[E_tot];
		for(int i=0; i<E_tot; i++) {
			Eflow[i] = 0.0f;
		}
		
		U_in = new double[N];
		U_out = new double[N];
		Vflow = new double[N];
		R = new double[N];
		for(int i=0; i<N; i++) {
			R[i] = 1.0f;
		}
		
		solveInternalLP();
		for(int r=0; r<3; r++) {
			addResistance();
			solveInternalLP();
		}
//		dumpR();
		
		ArrayList<Integer> res = chooseUsingR();
		System.out.println(res);
		return res;
	}
	
	void addResistance() {
		double deltaR = 0, minU=Double.MAX_VALUE, maxU=0;
		for(int v=0; v<N; v++) {
			double update = R[v]*Vflow[v]; 
			update *= update;
			
			R[v] += update;
			deltaR += update;
			minU = Math.min(minU, update);
			maxU = Math.max(maxU, update);
		}
		System.out.println("Added "+(float)deltaR+" resistance. Max="+(float)maxU+", min="+minU);
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
				double val = (rg.real_N() > 200 ? R[rg.invMap[i]] : (1+rg.inDeg[i]) * (1+rg.outDeg[i]));
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
	
	//Solves the circuit by an LP. Kirchoff relationships and Ohmic vertices are linear relationships.
	//The diodes are given by Eflow[e] >= 0, Eflow[e] <= V_out - Vin, maximize Eflow.
	void solveInternalLP() {
		final boolean ECHO = false;
		
		String lpName = "/tmp/circuit.lp";
		String outName = "/tmp/circuit.out";
		PrintStream scipProblem;
		try {
			scipProblem = new PrintStream(new FileOutputStream(lpName));
		} catch (FileNotFoundException e2) {
			throw new RuntimeException("Couldn't open SCIP LP for writing", e2);
		}
		
		//lp vars: ef (Eflow), vf (Vflow), ui (U_in), uo (U_out)
		
		//Objective
		scipProblem.println("Maximize");
		for(int e=0; e<E_tot; e++)
			scipProblem.print((e==0?"":"+") + "  ef"+e+" ");
		scipProblem.println();
		
		//Constraints
		scipProblem.println("st");{//such that
			//Kirchoff for vertex out
			for(int v=0; v<N; v++) {
				scipProblem.print("-vf"+v);
				for(int ei = 0; ei < g.outDeg[v]; ei++) {
					int e = E_offset[v] + ei;
					scipProblem.print("+ ef"+e);
				}
				scipProblem.println(" == 0");
			}
			
			//Kirchoff for vertex in
			for(int v=0; v<N; v++) {
				scipProblem.print("-vf"+v);
				for(int vi : g.backEList[v]) {
					int ei = Arrays.binarySearch(g.eList[vi], v);
					int e = E_offset[vi] + ei;
					scipProblem.print("+ ef"+e);
				}
				scipProblem.println(" == 0");
			}
			
			//Ohm's law for internal current of each vertex
			//V_out - V_in + R[v]*Vflow == 1
			for(int v=0; v<N; v++) {
				scipProblem.println("uo"+v+" - ui"+v+" + "+(float)R[v]+"vf"+v+" == 1");
			}
			
			//Diode modelling -- ohm's law
			for(int vi=0; vi<N; vi++) {
				for(int ei = 0; ei < g.outDeg[vi]; ei++) {
					int vf = g.eList[vi][ei];
					int e = E_offset[vi] + ei;
					
					//Eflow[e] >= 0
					scipProblem.println("ef"+e+" >= 0");
					//V_out[vi] - V_in[vf] - Eflow[e] >= 0
					scipProblem.println("uo"+vi+" -ui"+vf+" -ef"+e+" >= 0");
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
				String[] parts = line.split(" ");
				String type = parts[0].substring(0,2);
				int ind = Integer.valueOf(parts[0].substring(2));
				
				String valStr = parts[parts.length-2];
				double val = (valStr.equals("+infinity") ? Double.MAX_VALUE : Double.valueOf(valStr));
				switch(type) {
				case "ef":
					Eflow[ind] = val; break;
				case "vf":
					Vflow[ind] = val; break;
				case "ui":
					U_in[ind] = val; break;
				case "uo":
					U_out[ind] = val; break;
				default:
					throw new RuntimeException("Couldn't understand line "+line);
				}
			}

		} catch (IOException e) {
			throw new RuntimeException("Couldn't open SCIP LP for writing", e);
		}
	}
	
	void solveInternal(int maxiter) {
		final boolean bwrd = false;
		for(int iter=0; iter<maxiter; iter++) {
//			bwrd ^= true;
			//Alternate between an Ohm sweep and Kirchoff sweep.
			
			//Ohm's law on the internal current of the vertex
			double ohmErr1 = 0;
			for(int v=(bwrd?N-1:0); (bwrd?v>=0:v<N); v+=(bwrd?-1:+1)) {
				double Vdiff1 = U_out[v] - U_in[v];
				double Vdiff2 = 1 - Vflow[v] * R[v];
				double newVdiff = (Vdiff1 + Vdiff2)/2;
				U_out[v] += (newVdiff - Vdiff1)/2;
				U_in[v] -= (newVdiff - Vdiff1)/2;
				Vflow[v] = -(newVdiff-1)/R[v];
				
				double err = Math.abs(Vdiff2 - Vdiff1);
				ohmErr1 += err;
			}
			
			//Ohm's law on each diode wire
			//either V_out[vi] > V_in[vf], and  V_out[vi] - V_in[vf] == Eflow[e] (times R[e], which is 1)
		    //or: V_in[vf] < V_out[vi], and Eflow[e] == 0
			double ohmErr2 = 0;
			for(int vi=(bwrd?N-1:0); (bwrd?vi>=0:vi<N); vi+=(bwrd?-1:+1)) {
				for(int ei = 0; ei < g.outDeg[vi]; ei++) {
					int vf = g.eList[vi][ei];
					int e = E_offset[vi] + ei;
					
					if(U_out[vi] < U_in[vf]) {
						Eflow[e] = 0;
					} else {
						double Vdiff1 = U_out[vi] - U_in[vf];
						double Vdiff2 = Eflow[e];
						double newVdiff = (Vdiff1 + Vdiff2)/2;
						U_out[vi] += (newVdiff - Vdiff1)/2;
						U_in[vf] -= (newVdiff - Vdiff1)/2;
						Eflow[e] = newVdiff;
						
						double err = Math.abs(Vdiff2 - Vdiff1);
						ohmErr2 += err;
					}
					
				}
			}
			
			//Kirchoff's law at each vertex
			double kErr = 0;
			for(int v=(bwrd?N-1:0); (bwrd?v>=0:v<N); v+=(bwrd?-1:+1)) {
				double vFlow1 = Vflow[v];
				
				double vFlow2 = 0;
				for(int ei = 0; ei < g.outDeg[v]; ei++) {
					int e = E_offset[v] + ei;
					vFlow2 += Eflow[e];
				}
					
				double vFlow3 = 0;
				for(int vi : g.backEList[v]) {
					int ei = Arrays.binarySearch(g.eList[vi], v);
					int e = E_offset[vi] + ei;
					vFlow3 += Eflow[e];
				}
				
				double newFlow = (vFlow1 + vFlow2 + vFlow3)/3;
				double err = Math.abs(vFlow1 - newFlow) + Math.abs(vFlow2 - newFlow) + Math.abs(vFlow3 - newFlow);
				kErr += err;
//				
//				if(v==0)
//					System.out.println("iter="+iter+", v="+v+": err="+err+", vF1="+vFlow1+", min"+min_Uin_vf);

				Vflow[v] = newFlow;
				
				if(vFlow2 > newFlow) {
					//scale down
					double ratio = newFlow/vFlow2;
					double diff = (newFlow-vFlow2)/g.outDeg[v];
					for(int ei = 0; ei < g.outDeg[v]; ei++) {
						int e = E_offset[v] + ei;
						Eflow[e] *= ratio;
//						Eflow[e] = Math.max(0, Eflow[e]+diff);
					}
				} else {
					//add flow
					double diff = (newFlow-vFlow2)/g.outDeg[v];
					for(int ei = 0; ei < g.outDeg[v]; ei++) {
						int e = E_offset[v] + ei;
						Eflow[e] += diff;
					}
				}
				

				if(vFlow3 > newFlow) {
					//scale down
					double ratio = newFlow/vFlow3;
					double diff = (newFlow-vFlow3)/g.inDeg[v];
					for(int vi : g.backEList[v]) {
						int ei = Arrays.binarySearch(g.eList[vi], v);
						int e = E_offset[vi] + ei;
						Eflow[e] *= ratio;
//						Eflow[e] = Math.max(0, Eflow[e]+diff);
					}
				} else {
					//add flow
					double diff = (newFlow-vFlow3)/g.inDeg[v];
					for(int vi : g.backEList[v]) {
						int ei = Arrays.binarySearch(g.eList[vi], v);
						int e = E_offset[vi] + ei;
						Eflow[e] += diff;
					}
				}
			}
			
			if(iter % 50 == 0)
				System.out.println("Error values @"+iter+": o1="+(float)ohmErr1+", o2="+(float)ohmErr2+", kErr="+(float)kErr);
		}
		
//		dumpKirchoff();
	}
	
	//Failed attempt at a solver with no wire internal resistance.
	void solveNoInternal() {
		for(int iter=0; iter<200; iter++) {
			double kErr = 0;
			for(int v=0; v<g.N; v++) {
				//VFlow[vi] = (1 + U_in[vi] - min(U_in[vf]))/R[vi]
				//VFlow[v] = Sum out EFlow[e]
				//VFlow[v] = Sum in EFlow[e]
				
				double min_Uin_vf = Float.MAX_VALUE;
				for(int vf : g.eList[v]) {
					min_Uin_vf = Math.min(U_in[vf], min_Uin_vf);
				}
				double vFlow1 = (1 + U_in[v] - min_Uin_vf)/R[v];
				vFlow1 = Math.max(vFlow1, 0);
				
				
				double vFlow2 = 0;
				for(int ei = 0; ei < g.outDeg[v]; ei++) {
					int e = E_offset[v] + ei;
					vFlow2 += Eflow[e];
				}
					
				double vFlow3 = 0;
				for(int vi : g.backEList[v]) {
					int ei = Arrays.binarySearch(g.eList[vi], v);
					int e = E_offset[vi] + ei;
					vFlow3 += Eflow[e];
				}
				
				double newFlow = (vFlow1 + vFlow2 + vFlow3)/3;
				double err = Math.abs(vFlow1 - newFlow) + Math.abs(vFlow2 - newFlow) + Math.abs(vFlow3 - newFlow);
				kErr += err;
//				
				if(v==0)
					System.out.println("iter="+iter+", v="+v+": err="+err+", vF1="+vFlow1+", min"+min_Uin_vf);
				
				double U_in_minus_min = newFlow*R[v]-1;
				U_in[v] = U_in_minus_min + min_Uin_vf;//TODO modify the minimum too?
				
				if(vFlow2 > newFlow) {
					//scale down
					double ratio = newFlow/vFlow2;
					double diff = (newFlow-vFlow2)/g.outDeg[v];
					for(int ei = 0; ei < g.outDeg[v]; ei++) {
						int e = E_offset[v] + ei;
						Eflow[e] *= ratio;
//						Eflow[e] = Math.max(0, Eflow[e]+diff);
					}
				} else {
					//add flow
					double diff = (newFlow-vFlow2)/g.outDeg[v];
					for(int ei = 0; ei < g.outDeg[v]; ei++) {
						int e = E_offset[v] + ei;
						Eflow[e] += diff;
					}
				}
				

				if(vFlow3 > newFlow) {
					//scale down
					double ratio = newFlow/vFlow3;
					double diff = (newFlow-vFlow3)/g.inDeg[v];
					for(int vi : g.backEList[v]) {
						int ei = Arrays.binarySearch(g.eList[vi], v);
						int e = E_offset[vi] + ei;
						Eflow[e] *= ratio;
//						Eflow[e] = Math.max(0, Eflow[e]+diff);
					}
				} else {
					//add flow
					double diff = (newFlow-vFlow3)/g.inDeg[v];
					for(int vi : g.backEList[v]) {
						int ei = Arrays.binarySearch(g.eList[vi], v);
						int e = E_offset[vi] + ei;
						Eflow[e] += diff;
					}
				}
			}
			System.out.println("Kirchoff error @"+iter+": "+kErr);
		}
		
		dumpKirchoff();
	}
	
	void dumpKirchoff() {
		for(int v=0; v<N; v++) {
			for(int ei = 0; ei < g.outDeg[v]; ei++) {
				int vf = g.eList[v][ei];
				int e = E_offset[v] + ei;
				double ef = Eflow[e];
				System.out.println("v"+v+"@"+(float)(U_out[v])+" ---> v"+vf+"@"+(float)U_in[vf]+":  "+(float)ef);
			}
		}
		System.out.println();
	}
	
	void dumpR() {
		for(int v=0; v<N; v++) {
			System.out.println("R["+v+"] = "+(float)R[v]+",\tVflow["+v+"] = "+(float)Vflow[v]);
		}
	}

	@Override
	public ArrayList<Integer> solve(ReducedGraph g) {
		throw new RuntimeException();
	}

}
