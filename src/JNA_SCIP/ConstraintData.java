package JNA_SCIP;

public abstract class ConstraintData<Self extends ConstraintData<Self>> {
	public abstract Self copy();
	public abstract void delete();
}
