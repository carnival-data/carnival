package carnival.core.graph


/** 
 * An element constraint that puts limitations on graph elements,which may be
 * a vertex or an edge.
 */
interface ElementConstraint {

	/** The element label to which the constraint applies */
	String getLabel()

	/** The namespace to which thie constraint applies */
	String getNameSpace()

	/** 
	 * If true, this constraint is meant to be applied regardless of namespace. 
	 */
	boolean isGlobal()
}
