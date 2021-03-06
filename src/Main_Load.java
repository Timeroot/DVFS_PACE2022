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
	Thread selfkiller;
	
	boolean[] candidateDVFS;
	
	boolean foundSol = false;
	
	static final boolean TESTING = true;
	static final boolean VERBOSE = true && TESTING;
	
	//usually while testing we want to check all our outputs pass.
	//but sometimes we've set some problems to deliberately skip.
	//in this case, you can set this false, and the program won't complain
	//when it fails to solve the problem.
	static final boolean MUST_VERIFY = true;
	
	//Extra assertion checks to make sure everything's valid,
	//but that slow things down significantly
	static final boolean GRAPH_CHECK = false;//do regular checks that the graph data is valid
	static final boolean VERIFY_DVFS = true;//verify the solution from MinimumCoverDescriptor is a DVFS

	//Run heuristic or exact solver
	static final boolean HEURISTIC = false;
	
	static final boolean KILL_SELF = false;
	static final long MAX_TIME = (HEURISTIC?10:30)*60*1000;
	static final long KILL_SELF_TIMEOUT = MAX_TIME;
	
	static volatile boolean is_killed = false;
	static long startT = -1;
	
	@SuppressWarnings({ "unchecked" })
	public Main_Load(BufferedReader reader, PrintStream fileout) throws IOException {
		GraphChunk.biggestChunk = 0;
		startT = System.currentTimeMillis();
		
		//parse in the input
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
		
        ArrayList<Integer> sol;
        
		if(HEURISTIC) {
			//in HEURISTIC mode, we need to be able to dump our current best
			//solution as soon as we get a kill signal. This isn't relevant
			//in the EXACT mode, as there we have no choice to keep running
			//until we solve or we don't.
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
			
	        //We can add our own kill-timeout if we want
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
			
//			Graph g = new Graph(N, eList, backEList, inDeg, outDeg);
//			PruneLowdeg prune = new PruneLowdeg(g);
//			System.out.println("Initial prune: N="+g.N);
//			ArrayList<Integer> sol = prune.solve(new GreedySolver());
	
	//		Graph g = new Graph(N, eList, backEList, inDeg, outDeg);
	//		PruneLowdeg prune = new PruneLowdeg(g);
	//		ArrayList<Integer> sol = prune.solve(new FlowSolver());

			ReducedGraph rg = new ReducedGraph(N, eList, backEList, inDeg, outDeg);

			sol = new Heuristic_DVFS_Aggressive().solve(rg);
		
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
		
		foundSol = true;
		if(TESTING) {
			System.out.println("FVS with size "+sol.size());
			if(check_index >= 0 && check_answers[check_index] != sol.size())
				throw new RuntimeException("Didn't match known answer "+check_answers[check_index]);
			else if(check_index >= 0)
				System.out.println("Matched expectation");
		}
		save(fileout, sol);
		
		if(KILL_SELF) {
			selfkiller.stop();
			is_killed = false;
		}
	}

	//heuristic solver overall score is the product of all the scores.
	//Well, the geometric mean, but same thing. If we just track the product,
	//though, we get overflow, so we track the log.
	static double scoreLogProduct = 1.0;
	void save(PrintStream fileout, ArrayList<Integer> sol) throws FileNotFoundException {
		scoreLogProduct *= Math.log(sol.size());
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

	//Main method for submissions to PACE
	//reads stdin, write answer to stdout
	public static void main_submit(String[] args) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		PrintStream fileout = new PrintStream(System.out);
		
		new Main_Load(reader, fileout);
	}
	
	static final int[] check_answers = new int[] {
		2,674,631,276,450,1917,547,788,364,1137,510,452,273,575,715,2079,277,2179,2679,400,
		2509,521,2837,2666,2043,2086,2628,1948,1955,214,554,2267,3709,513,2092,675,4420,4069,
		5292,4906,4887,745,6254,6253,4928,755,10616,5198,9797,4752,21900,10159,58,8888,12710,
		1741,915,24084,24091,21755,73,20628,22898,1469,20630,19518,1314
	};
	static int check_index = -1;
	
	//Main method for development and debugging.
	//reads problems from files, writes to files, in a loop across many problems
	public static void main_test(String[] args) throws IOException {
		long startT = System.currentTimeMillis();
		int maxTi = -1;
		long maxT = -1;
		String prefix =
				HEURISTIC ? "./heuristic_public/h_" : "./exact_public/e_" ;
//		String prefix = "./exact_other/e_";
		
		int done=0;
		//#133 has a large chunk of {N=1650,E=5134}.
		//#157 is a tricky cycle cover problem -- just barely at time limit.
		//#151 is a pretty hard cycle cover problem.
		for(int i=1; i<=127; i+=2) {
			long t0 = System.currentTimeMillis();
			
			String problem = prefix+"000".substring(Integer.toString(i).length())+i;
			System.out.println("Running on "+problem+", OPTIL #"+(i-1)/2);
			String inName = problem;
			String outName = problem+".out";
			BufferedReader reader = new BufferedReader(new FileReader(inName));
			PrintStream fileout = new PrintStream(new FileOutputStream(outName));
			
			check_index = (i-1)/2;
			if(i%2 == 0 || check_index >= check_answers.length)
				check_index = -1;
			
			Main_Load ml;
			try {
				ml = new Main_Load(reader, fileout);
			} catch(Exception e) {
				System.out.println("Error in i="+i);
				throw e;
			}
			
			if(ml.foundSol) {
				verify(inName, outName);
			} else {
				System.out.println("Didn't verify because no solution found");
				if(MUST_VERIFY)
					throw new RuntimeException();
			}
			done++;
			long thisT = System.currentTimeMillis()-t0;
			System.out.println("Took "+thisT*0.001+"sec");
			System.out.println();
			if(thisT > maxT) {
				maxTi = i;
				maxT = thisT;
			}
		}
		long totalTime = System.currentTimeMillis() - startT; 
		
		if(TESTING && HEURISTIC)
			write_heuristic_settings();
		if(HEURISTIC)
			System.out.println("Geometric mean score: "+Math.exp(scoreLogProduct/done));
		
		System.out.println("Avg time: "+(totalTime/done)*0.001+"sec");
		System.out.println("Max time: "+maxT*0.001+"sec, problem "+maxTi);
	}
	
	//Dump a bunch of the configurations for heuristic solving
	public static void write_heuristic_settings() {
		System.out.println("CLEANUP_AFTER = "+Heuristic_DVFS_Aggressive.CLEANUP_AFTER);
		System.out.println("CLEAN_WHEN_KILLED = "+Heuristic_DVFS_Aggressive.CLEAN_WHEN_KILLED);
		System.out.println("CLEANUP_DIRECTION_FWD = "+Heuristic_DVFS_Aggressive.CLEANUP_DIRECTION_FWD);
		System.out.println("USE_SCC = "+Heuristic_DVFS_Aggressive.USE_SCC);
		System.out.println();
		System.out.println("START_ARTICULATION_CHECK = "+Heuristic_DVFS_Aggressive.START_ARTICULATION_CHECK);
		System.out.println("ARTICULATION_CHECK_FREQ = "+Heuristic_DVFS_Aggressive.ARTICULATION_CHECK_FREQ);
		System.out.println("ARTICULATION_MIN_N = "+Heuristic_DVFS_Aggressive.ARTICULATION_MIN_N);
		System.out.println();
		System.out.println("FAST_SINKHORN_MARGIN = "+Heuristic_DVFS_Aggressive.FAST_SINKHORN_MARGIN);
		System.out.println("DEGREE_HEURISTIC_SWITCH = "+Heuristic_DVFS_Aggressive.DEGREE_HEURISTIC_SWITCH);
		System.out.println("CLEANUP_MARGIN = "+Heuristic_DVFS_Aggressive.CLEANUP_MARGIN);
		System.out.println();
		System.out.println("KILL_SELF = "+KILL_SELF);
		System.out.println("HEURISTIC MODE? "+HEURISTIC);
		System.out.println("MAX_TIME = "+MAX_TIME);
		System.out.println();
	}
	
	//How many milliseconds left before we have to return our answer?
	public static long msRemaining() { 
		long elapsed = System.currentTimeMillis() - startT;
		if(HEURISTIC)
			return MAX_TIME - elapsed;
		else
			return MAX_TIME - elapsed;
	}

}
