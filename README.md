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

# Writeup

The algorithm and approach is documented at https://arxiv.org/pdf/2208.01119v1.pdf, the offficial writeup to accompany this submission.

# License

DVFS is released under the MIT License, see LICENSE.txt. However, be aware that the necessary components
SCIP and JNA are released under different licenses. SCIP is (as of the time of this writing) only available
under the ZIB license, and JNA is available under the Apache-2.0 or LGPL-2.1 licenses.
