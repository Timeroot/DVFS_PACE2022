package JNA_SCIP;

/* type_cons.h */
public enum SCIP_LINCONSTYPE {
    SCIP_LINCONSTYPE_EMPTY,
    SCIP_LINCONSTYPE_FREE,
    SCIP_LINCONSTYPE_SINGLETON,
    SCIP_LINCONSTYPE_AGGREGATION,
    SCIP_LINCONSTYPE_PRECEDENCE,
    SCIP_LINCONSTYPE_VARBOUND,
    SCIP_LINCONSTYPE_SETPARTITION,
    SCIP_LINCONSTYPE_SETPACKING,
    SCIP_LINCONSTYPE_SETCOVERING,
    SCIP_LINCONSTYPE_CARDINALITY,
    SCIP_LINCONSTYPE_INVKNAPSACK,
    SCIP_LINCONSTYPE_EQKNAPSACK,
    SCIP_LINCONSTYPE_BINPACKING,
    SCIP_LINCONSTYPE_KNAPSACK,
    SCIP_LINCONSTYPE_INTKNAPSACK,
    SCIP_LINCONSTYPE_MIXEDBINARY,
    SCIP_LINCONSTYPE_GENERAL
}