package JNA_SCIP;

import com.sun.jna.Pointer;
import com.sun.jna.PointerType;

public class SCIP_VAR extends PointerType {
	public SCIP_VAR() {}
	public SCIP_VAR(Pointer p) {
		super(p);
	}
	
//	public double obj;                /**< objective function value of variable (might be changed temporarily in probing mode)*/
//	public double unchangedobj;       /**< unchanged objective function value of variable (ignoring temporary changes in probing mode) */
//	public double branchfactor;       /**< factor to weigh variable's branching score with */
//	public double rootsol;            /**< last primal solution of variable in root node, or zero */
//	public double bestrootsol;        /**< best primal solution of variable in root node, or zero, w.r.t. root LP value and root reduced cost */
//	public double bestrootredcost;    /**< best reduced costs of variable in root node, or zero, w.r.t. root LP value and root solution value */
//	public double bestrootlpobjval;   /**< best root LP objective value, or SCIP_INVALID, w.r.t. root solution value and root reduced cost */
//	public double relaxsol;           /**< primal solution of variable in current relaxation solution, or SCIP_INVALID */
//	public double nlpsol;             /**< primal solution of variable in current NLP solution, or SCIP_INVALID */
//	public double primsolavg;         /**< weighted average of all values of variable in primal feasible solutions */
//	public double conflictlb;         /**< maximal lower bound of variable in the current conflict */
//	public double conflictub;         /**< minimal upper bound of variable in the current conflict */
//	public double conflictrelaxedlb;  /**< minimal relaxed lower bound of variable in the current conflict (conflictrelqxlb <= conflictlb) */
//	public double conflictrelaxedub;  /**< minimal release upper bound of variable in the current conflict (conflictrelqxlb <= conflictlb) */
//	public double lazylb;             /**< global lower bound that is ensured by constraints and has not to be added to the LP */
//	public double lazyub;             /**< global upper bound that is ensured by constraints and has not to be added to the LP */
//	public double glbdom;             /**< domain of variable in global problem */
//	public double locdom;             /**< domain of variable in current subproblem */
}