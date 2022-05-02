package JNA_SCIP.Examples;

import static JNA_SCIP.SCIP_PARAMEMPHASIS.*;
import static JNA_SCIP.SCIP_VARTYPE.*;

import JNA_SCIP.*;

public class MILP_Example {

	public static void main(String[] args) {
		JSCIP.create();
		JSCIP.includeDefaultPlugins();
		
		JSCIP.infoMessage(null, "SCIP Loaded, \"%s\"\n", "Hello world");

		System.out.println("Verblevel = "+JSCIP.getVerbLevel());
		
		JSCIP.setEmphasis(SCIP_PARAMEMPHASIS_OPTIMALITY, false);
		
		JSCIP.createProbBasic("test");
		double inf = JSCIP.infinity();
		
		//obj = 40x + 30y
		//cons_1: x + 3y <= 12
		//cons_2: 3x + y <= 16
		//x >= 0, y >= 0
		//x is an integer
		SCIP_VAR x = JSCIP.createVarBasic("x", 0, inf, -40.0, SCIP_VARTYPE_INTEGER);
		JSCIP.addVar(x);
		
		SCIP_VAR y = JSCIP.createVarBasic("y", 0, inf, -30.0, SCIP_VARTYPE_CONTINUOUS);
		JSCIP.addVar(y);
		
		SCIP_CONS cons_1 = JSCIP.createConsBasicLinear("cons1", 2, new SCIP_VAR[]{x,y}, new double[]{1,3}, -inf, 12);
		JSCIP.addCons(cons_1);

		SCIP_CONS cons_2 = JSCIP.createConsBasicLinear("cons2", 2, new SCIP_VAR[]{x,y}, new double[]{3,1}, -inf, 16);
		JSCIP.addCons(cons_2);

		JSCIP.releaseVar(x);
		JSCIP.releaseVar(y);
		JSCIP.releaseCons(cons_1);
		JSCIP.releaseCons(cons_2);
		
		JSCIP.printOrigProblem(null, "cip", false);
		
		JSCIP.solve();
		
		JSCIP.printSol(JSCIP.getBestSol(), null, false);
		
		JSCIP.free();
	}
}