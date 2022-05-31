# DVFS
## An exact solver for Directed Feedback Vertex Set (for PACE 2022)
DVFS is a solver for DFVS problems. The name DVFS stands for the DFVS-Via-Flattening Solver. This tongue-in-cheek name
refers to the strategy to "flatten" the problem, going from the relatively "rich" structure of DFVS to simpler problems
such as covering problems, where reduction rules are often simpler.

Building requires a copy of the freely available SCIP library: https://www.scipopt.org/
... specifically as a dynamic library. The program expects to find a copy of libscip.so in the working directory.

It also requires a copy of JNA, and JNA-Platform: https://github.com/java-native-access/jna#jna

To build, just `javac` all the .java files (although admittedly, some of them are not currently used).
Then it can be launched with the main class `Main_Load`. It takes input from stdin in the same format
as PACE 2022, and outputs a minimal directed feedback vertex set as a list of vertex IDs.

If you want to use it as a library in your program, you should build a `Graph` and then call `ExactSolver.solve(g)`.
