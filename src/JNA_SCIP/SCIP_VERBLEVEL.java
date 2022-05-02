package JNA_SCIP;

/**< function not implemented */

public enum SCIP_VERBLEVEL {
	SCIP_VERBLEVEL_NONE,          /**< only error and warning messages are displayed */
    SCIP_VERBLEVEL_DIALOG,          /**< only interactive dialogs, errors, and warnings are displayed */
    SCIP_VERBLEVEL_MINIMA,          /**< only important messages are displayed */
    SCIP_VERBLEVEL_NORMAL,          /**< standard messages are displayed */
    SCIP_VERBLEVEL_HIGH,          /**< a lot of information is displayed */
    SCIP_VERBLEVEL_FULL
}