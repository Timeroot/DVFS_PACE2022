import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import sun.misc.Signal;
import sun.misc.SignalHandler;

public class Main_Load {
	
	int N;
	HashSet<Integer>[] eList;
	HashSet<Integer>[] backEList;
	int[] inDeg, outDeg;
	boolean sparseOnly = false;
	
	boolean[] candidateDVFS;
	
	static final boolean TESTING = true;
	static final boolean VERBOSE = TESTING && false;
	static final boolean HEURISTIC = false;
	
	static final boolean KILL_SELF = false;
	static final long MAX_TIME = (HEURISTIC?10:30)*60*1000;
	static final long KILL_SELF_TIMEOUT = MAX_TIME;
	
	static volatile boolean is_killed = false;
	static long startT = -1;
	
	@SuppressWarnings({ "unchecked" })
	public Main_Load(BufferedReader reader, PrintStream fileout) throws IOException {
		MinimumCoverSolver.GraphChunk.biggestChunk = 0;
		startT = System.currentTimeMillis();
		
		int E = -1;
		{
			String line;
			int ni = -1;
			while((line = reader.readLine()) != null) {
				if(line.startsWith("%"))
					continue;
				if(ni == -1) {
					N = Integer.parseInt(line.split(" ")[0]);
					E = Integer.parseInt(line.split(" ")[1]);
					ni++;
					eList = new HashSet[N];
					inDeg = new int[N];
					outDeg = new int[N];
					
				} else {
					String[] pts = line.split(" ");
					if(line.length() == 0)
						pts = new String[] {};
					eList[ni] = new HashSet<Integer>(pts.length);
					for(String pt : pts) {
						int no = Integer.parseInt(pt)-1;
						eList[ni].add(no);
						outDeg[ni]++;
						inDeg[no]++;

					}
					//Keep array sorted.
					ni++;
				}
				if(ni == N)
					break;//done
			}
		}

		backEList = new HashSet[N];
		for(int ni=0; ni<N; ni++) {
			backEList[ni] = new HashSet<Integer>(inDeg[ni]);
		}
		for(int ni=0; ni<N; ni++) {
			for(int no : eList[ni]) {
				backEList[no].add(ni);
			}
		}
		
		if(TESTING)
			System.out.println("Loaded. N="+N+", E="+E);
        
		SignalHandler termHandler = new SignalHandler()
        {
            @Override
            public void handle(Signal sig)
            {
            	if(VERBOSE)
            		System.out.println("Early termination (1)");
            	is_killed = true;
            }
        };
        Signal.handle(new Signal("TERM"), termHandler);
		
		Thread selfkiller;
		if(KILL_SELF) {
			selfkiller = new Thread("Killer") {
				public void run() {
					try {
						Thread.sleep(KILL_SELF_TIMEOUT);
					} catch (InterruptedException e) {}
					if(VERBOSE)
            			System.out.println("Early termination (2)");
					is_killed = true;					
				}
			};
			selfkiller.start();
		}
		
        ArrayList<Integer> sol;
        
		if(HEURISTIC) {
//			Graph g = new Graph(N, eList, backEList, inDeg, outDeg);
//			PruneLowdeg prune = new PruneLowdeg(g);
//			System.out.println("Initial prune: N="+g.N);
//			ArrayList<Integer> sol = prune.solve(new GreedySolver());
	
	//		Graph g = new Graph(N, eList, backEList, inDeg, outDeg);
	//		PruneLowdeg prune = new PruneLowdeg(g);
	//		ArrayList<Integer> sol = prune.solve(new FlowSolver());

			ReducedGraph rg = new ReducedGraph(N, eList, backEList, inDeg, outDeg);

			sol = new GreedySolver().solve(rg);
		
		} else { //EXACT
			
			Graph g = new Graph(N, eList, backEList, inDeg, outDeg);
//			PruneLowdeg prune = new PruneLowdeg(g);
//			if(TESTING) System.out.println("Initial prune: N="+g.N);
//			sol = prune.solve(new SCIPSolver());
//			sol = prune.solve(new JNASCIPSolver_Dumb());
//			sol = prune.solve(new JNASCIPSolver());
			sol = ExactSolver.solve(g);
		}
		
		if(sol == null) {
			if(TESTING)
				System.out.println("No solution found :( ");
			return;
		}
		
		if(TESTING)
			System.out.println("FVS with size "+sol.size());
		save(fileout, sol);
		
		if(KILL_SELF) {
			selfkiller.stop();
			is_killed = false;
		}
	}

	static double scoreProduct = 1.0;
	static final double SCORE_PROD_SHIFT = 1000; //divide by this each time, then multiply back in the end. Avoids overflow
	void save(PrintStream fileout, ArrayList<Integer> sol) throws FileNotFoundException {
		scoreProduct *= sol.size() / SCORE_PROD_SHIFT;
		for(Integer i : sol)
			fileout.println(1+i);
		fileout.close();
	}
	
	static void verify(String inName, String outName) throws IOException {
		Process veriProc = new ProcessBuilder(
				"./verifier/verifier",
				inName, outName
				).start();
		int res;
		try {
			res = veriProc.waitFor();
			if(res != 0)
				throw new RuntimeException("Failed verification");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws IOException {
		if(TESTING)
			main_test(args);
		else
			main_submit(args);
	}

	public static void main_submit(String[] args) throws IOException {

		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		PrintStream fileout = new PrintStream(System.out);
		
		new Main_Load(reader, fileout);
	}
	
	public static void main_test(String[] args) throws IOException {
//		write_settings();
		long startT = System.currentTimeMillis();
		String prefix = HEURISTIC ? "./heuristic_public/h_" : "./exact_public/e_";
		int done=0;
		//#37 tricky, #73 requires new cycle.
		//#85 is killer. #93 is hard (~500s)
		//#141 gave MLE, is MIS problem
		for(int i=1; i<=151; i+=2) {
			long t0 = System.currentTimeMillis();
			
			String problem = prefix+"000".substring(Integer.toString(i).length())+i;
			System.out.println("Running on "+problem);
			String inName = problem;
			String outName = problem+".out";
			BufferedReader reader = new BufferedReader(new FileReader(inName));
			PrintStream fileout = new PrintStream(new FileOutputStream(outName));
			
//			Thread selfkiller;
//			if(KILL_SELF) {
//				selfkiller = new Thread("Killer") {
//					public void run() {
//						try {
//							Thread.sleep(KILL_SELF_TIMEOUT);
//						} catch (InterruptedException e) {}
//						Signal.raise(new Signal("TERM"));						
//					}
//				};
//				selfkiller.start();
//			}
			
			Main_Load ml = new Main_Load(reader, fileout);
			
//			if(KILL_SELF) {
//				selfkiller.stop();
//				is_killed = false;
//			}
			
//			verify(inName, outName);
			done++;
			System.out.println("Took "+(System.currentTimeMillis()-t0)*0.001+"sec");
			System.out.println();
		}
		long totalTime = System.currentTimeMillis() - startT; 
//		write_settings();
		if(HEURISTIC)
			System.out.println("Geometric mean score: "+SCORE_PROD_SHIFT*Math.pow(scoreProduct, 1.0/done));
		System.out.println("Avg time: "+(totalTime/done)*0.001+"sec");
	}
	
	public static void write_settings() {
		System.out.println("CLEANUP_AFTER = "+GreedySolver.CLEANUP_AFTER);
		System.out.println("CLEAN_WHEN_KILLED = "+GreedySolver.CLEAN_WHEN_KILLED);
		System.out.println("CLEANUP_DIRECTION_FWD = "+GreedySolver.CLEANUP_DIRECTION_FWD);
		System.out.println("USE_SCC = "+GreedySolver.USE_SCC);
		System.out.println();
		System.out.println("START_ARTICULATION_CHECK = "+GreedySolver.START_ARTICULATION_CHECK);
		System.out.println("ARTICULATION_CHECK_FREQ = "+GreedySolver.ARTICULATION_CHECK_FREQ);
		System.out.println("ARTICULATION_MIN_N = "+GreedySolver.ARTICULATION_MIN_N);
		System.out.println();
		System.out.println("FAST_SINKHORN_MARGIN = "+GreedySolver.FAST_SINKHORN_MARGIN);
		System.out.println("DEGREE_HEURISTIC_SWITCH = "+GreedySolver.DEGREE_HEURISTIC_SWITCH);
		System.out.println("CLEANUP_MARGIN = "+GreedySolver.CLEANUP_MARGIN);
		System.out.println();
		System.out.println("KILL_SELF = "+KILL_SELF);
		System.out.println("HEURISTIC MODE? "+HEURISTIC);
		System.out.println("MAX_TIME = "+MAX_TIME);
		System.out.println();
	}
	
	public static long msRemaining() { 
		long elapsed = System.currentTimeMillis() - startT;
		if(HEURISTIC)
			return MAX_TIME - elapsed;
		else
			return MAX_TIME - elapsed;
	}

}
