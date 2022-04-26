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

@SuppressWarnings("restriction")
public class Main_Load {
	
	int N;
	HashSet<Integer>[] eList;
	HashSet<Integer>[] backEList;
	int[] inDeg, outDeg;
	boolean sparseOnly = false;
	
	boolean[] candidateDVFS;
	
	static final boolean TESTING = false;
	static final boolean VERBOSE = TESTING && true;
	static final boolean HEURISTIC = true;
	
	static final boolean KILL_SELF = false;
	static final long MAX_TIME = (HEURISTIC?10:30)*60*1000;
	static final long KILL_SELF_TIMEOUT = MAX_TIME;
	
	static volatile boolean is_killed = false;
	static long startT = -1;
	
	@SuppressWarnings({ "unchecked" })
	public Main_Load(BufferedReader reader, PrintStream fileout) throws IOException {
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
		
		} else {
			Graph g = new Graph(N, eList, backEList, inDeg, outDeg);
			PruneLowdeg prune = new PruneLowdeg(g);
			if(TESTING) System.out.println("Initial prune: N="+g.N);
			sol = prune.solve(new SCIPSolver());
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
	void save(PrintStream fileout, ArrayList<Integer> sol) throws FileNotFoundException {
		scoreProduct *= sol.size();
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
		long startT = System.currentTimeMillis();
		String prefix = "./heuristic_public/h_";
//		String prefix = "./exact_public/e_";
		int done=0;
		for(int i=197; i<=197; i+=2) {
			long t0 = System.currentTimeMillis();
			
			String problem = prefix+"000".substring(Integer.toString(i).length())+i;
			System.out.println("Running on "+problem);
			String inName = problem;
			String outName = problem+".out";
			BufferedReader reader = new BufferedReader(new FileReader(inName));
			PrintStream fileout = new PrintStream(new FileOutputStream(outName));
			
			Thread selfkiller;
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
			
			verify(inName, outName);
			done++;
			System.out.println("Took "+(System.currentTimeMillis()-t0)*0.001+"sec");
			System.out.println();
		}
		long totalTime = System.currentTimeMillis() - startT; 
		System.out.println("Geometric mean score: "+Math.pow(scoreProduct, 1.0/done));
		System.out.println("Avg time: "+(totalTime/done)*0.001+"sec");
	}
	
	public static long msRemaining() { 
		long elapsed = System.currentTimeMillis() - startT;
		if(HEURISTIC)
			return MAX_TIME - elapsed;
		else
			return MAX_TIME - elapsed;
	}

}
