import java.util.ArrayList;
import java.util.HashSet;

public interface Solver {
	ArrayList<Integer> solve(Graph g);
	ArrayList<Integer> solve(ReducedGraph g);
}
