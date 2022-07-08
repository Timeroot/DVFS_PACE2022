A list of items that would (probably) improve the solver.

== Phase joining ==
Currently the reductions are applied in several "phases":
 -Reduce the DFVS graph, and split into SCCs.
 -Do all graph>cycle transformations possible
 -Do minimum cover reductions
  -As part of this, the graph may be simplified somewhat. Currently:
  -Inclusions can lead to a vertex being removed from the graph
  -Vertices with indeg/outdeg zero are dropped
  -Alternatives can lead to vertices being replicated in the graph
  -If the graph is improved by these, inter-SCC edges are dropped as well.

This is messy and somewhat inefficient. Some of the graph>cycle transformations
might become useful again after the graph is reduced. Keeping chunks apart
during the transformation might enable more reductions later. And there's some
code duplication as a result. Ideally these would be joined together more.

== K2-aware cycle finding ==
The 'last resort' for graph>cycle transformation, and the reoptimization
cycle "digging", both just search chunks for cycles. They don't pay attention
to K2s that are already present as constraint. Having K2-aware search could
help the 'last resort' terminate faster, but using them optimally is NP-hard.
For the digging, valid solutions should always enforce the K2s already, but
the initial dig could make use of it.

== Merging XxxCoverReductions ==
Currently VertexCoverReductions, CycleCoverReductions, and ChunkCoverReductions
duplicate a lot of code. This was useful during development when it was not so
clear always immediately how and when the VC reductions applied in general. But
now it would probably better if they were merged. This would, ultimately, slow
things down a *tiny* bit because it would mean checking if vertices belonged to
chunks or big cycles even when doing a vanilla VC reduction. But, it would
reduce code duplication, the chance of bugs, and development effort to add new
reduction rules.

== New Reduction Rules ==
Some reduction rules to implement:
 * Twins. Currently the code to find them is there but disabled, and there's no
   code to actually do the reduction.
 * Generalized desks. The "desks" described in literature are unnecessarily
   restrictive in definition; generalized desks might add new benefits. Need to
   work out their conditions in the presence of cycles + chunks.
 * Rules from "What is Known About Vertex Cover Kernelization?" by Fellows,
   Jaffke, Király, Rosamond, and Weller.
   https://bora.uib.no/bora-xmlui/bitstream/handle/1956/21701/1811.09429.pdf
   These would all need to be carefuly adapted to the case with cycles/chunks.
   * Rule R.8 is similar to funnels, funnels have |C2|=1, but don't need to
     have requirement (ii). Implementing R.8 for |C2|>=2 would be good.
   * Rule R.9 would finish solving degree 3 vertices. But, we don't actually
     hit those often, oddly.
   * Rules R.10-R.12 would handle a decent number of degree 4 cases.
      
The Nemahuse-Trotter reduction involving the LP relaxation is useful on VC
   problems. Putting it to any good use here would require two things:
   * Understanding how it interacts with (at least) large cycles, ideally chunks
     as well. For this, referring to the original paper is probably best:
     https://link.springer.com/content/pdf/10.1007/BF01580222.pdf
   * A fast implementation. For pure VC there are approaches that don't require
     a full LP solver, using more matching-type algorithms. Might be too slow
     to be useful otherwise.
Regarding the question of applying N-T in the presence of larger cycles, there
is the method proposed by Hong Xu et al in "The Nemhauser-Trotter Reduction
and Lifted Message Passing for the Weighted CSP", and the method of CCG
generation in "A Framework for Hybrid Tractability Results in Boolean WCSPs"
by T.K. Satish Kumar. The idea is that any soft constraint on vertices can be
transformed into a weighted vertex cover problem; and the hard constraint of
covering big cycles is equivalent to a soft constraint with weight 1 (as we can
always "spend" that penalty to add any vertex in to cover it.) Then, since 
N-T is a safe reduction for *weighted* VC (not just plain VC), it can be used
to reduce the problem. Besides the general CCG/polynomial method of
transforming big cycles into VC, there's another method discussed in the next
section that might be more efficient here.

== Big Cycle / Funnel interconversion ==
A big cycle on vertices {v1, v2, .. vn} can be transformed into a VC constraint
on a modified graph. Create new vertices U = {u1, u2, .. un}, and connect them
in a clique. Then connect each pair {vi, ui}. As U is a clique, it must have n
or n-1 vertices in the cover. If all of the vi are absent from the cover, then
all of U must be in the cover (a cost of n), but if any vi is in the cover,
then we can cover with U\{ui}, for a cost of n-1. This is a transformation we
can use to turn big cycle cover problems into vertex cover problems, at the
cost of additional vertices. We could also go the other way: identifying
cliques where each vertex has exactly one outgoing edge, and turning that into
a big cycle instead. Some points to consider here:
 * For doing reductions, going cycle -> VC is probably useful, as VC is in
   general easier to study; we can also do N-T reductions in this form safely.
 * For doing the ILP solving at the end, VC -> cycle is more likely useful, as
   a cycle constraint is compact and "native" to the ILP format and uses fewer
   variables.
 * After the cycle -> VC transformation, there is a funnel (at each ui) that
   would naturally be reduced. This means that 3-cycle (resp k-cycle) actually
   only adds 1 new vertex (resp k-2 new vertices) after the funnel reduction.
   It also means that we won't ever actually see the clique shape naturally: we
   need to recognize its funnelized version. This is a (k-1)-clique that all
   have a set of common neighbors (what was N(vi)), and then exactly one
   neighbor each that the rest of them do not.
 * If some cycles differ by just one vertex e.g. {a,b,c}, {a,b,d}, and {a,b,e},
   then we can transform all of them into VC at once, with a single 3-clique
   that connects to {a}, {b}, {c,d,e}. Conversely, when recognizing a clique to
   transform it back to a cycle, we are allowed one vertex with external degree
   greater than 1. We can even do it with multiple vertices with a large
   external degree, but this can create exponentially many big cycles in
   general. A 4-clique connected to {a,b}, {c,d}, {e,f,g}, {h} can be turned
   into 2x2x3 = 12 cycles of size 4. This only remains valid as long as at
   least one of them has external degree only 1, that is, as long as we have
   a funnel. In this sense, the VC -> cycle transformation is exactly an
   alternative to the funnel reduction. Worth experimenting to see how they
   each perform, and if we should interconvert.

As compared with the CCG polynomial representation above, which takes O(2^n)
new vertices to represent a big cycle, this takes only n-2 vertices.

== SCIP: Reoptimization ==
Reoptimization with SCIP currently seems buggy, see for example
https://stackoverflow.com/questions/72027891/resolving-lps-with-constraint-addition-deletion-in-scip
It would be nice to get it fully working.

== SCIP: Heuristics ==
Currently heuristics are used in ILP_DVFS_*, but not the other ILP_* solvers,
which are what we actually use. It would be good to give SCIP a quick heuristic
solution. This would require writing a heuristic solver that can handle edges,
big cycles, and chunks all at once.

== SCIP: Digging more solutions ==
In the Reopt loop, currently it only finds cycles in the one provided optimal
solution. SCIP often has many solutions in storage though. Iterating through
some of these -- maybe all, maybe just the optimal ones -- and digging each,
could be a good way to reduce needed callback loops.

== SCIP: Conshdlr ==
There's a SCIP Conshdlr-based solver with an in-the-loop constraint handler.
It's not used because, currently, it does not perform as well as the
reoptimization-based solver. It would be good to investigate:
 * What properties of the Conshdlr that keep SCIP from being fast?
 * How can we tune the cut/enforcement priority, and cut generation type,
   to give better performance?
 * Can our conshdlr give a useful constraint propagator?
 * Can our conshdlr give a useful node selector?

== Bipartite Graphs ==
It is well-known that VC can be solved exactly efficiently on bipartite graphs.
It's worth investigating to what degree this could be applied in the presence
of a small number of cycles -- for example, if only 3 vertices are in one extra
cycle, and the rest is bipartite, can we handle that efficiently?

Bipartite VC graph solving is a tricky subject, since some simple reductions,
such as degree-2 folding, can make a bipartite graph non-bipartite.

The N-T reduction subsumes the bipartite case, as bipartite graphs are always
totally unimodular (TUM) and so the LP relaxation is equivalent to the ILP
optimum.
