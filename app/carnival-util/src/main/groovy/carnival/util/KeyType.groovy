package carnival.util



/**
 * An enumeration of key types.
 *
 * KeyType is used throughout Carnival.  However, its inception and primary
 * utility was for interoperability between data matrices.  With the move to a 
 * central graph representation of data, KeyType seems less relevant. This enum
 * will likely be factored out of the code at some point in the future.
 * 
 *
 */
public enum KeyType {
	// -- biobank encounter identifiers --
	/**
	* PMBB Collection Packet UUID or CGI subject_id (which is really an encounter identifier).
	* It is generated when the collection packet is created.
	* Every PMBB/CGI encounter has a unique ENCOUNTER_PACK_ID.
	*/
	ENCOUNTER_PACK_ID,

	/**
	* This keytype is used in code in a hard-coded way... which is bad.  This is a synonym for
	* ENCOUNTER_PACK_ID.  The code should be migrated to use ENCOUNTER_PACK_ID, not PACKET_ID.
	*/
	//PACKET_ID,

	
	/**
	* PMBB lab ID or CGI subject_id (which is really an encounter identifier).
	* For PMBB, it is generated when specimens are processed in the lab; if no specimens are 
	*	recieved, an ENCOUNTER_LAB_ID is not generated.
	*/
	ENCOUNTER_LAB_ID, 
	CGI_ENCOUNTER_ID, // CGI subject_id (which is really an encounter identifier)

	/**
	 * An encounter identifier
	 *
	 */
	ENCOUNTER_ID,

	// -- healthcare encounter identifiers --
	/**
	* PDS internal patient healthcare encounter identifier.  .  
	* This value is not persistant over time, so it should be generated and used within the same query 
	*	context... i.e. DO NOT generate a list of PK_PATIENT_ENCOUNTER_IDs and then use those values to query PDS 
	*	several weeks later.
	*/
	PK_PATIENT_ENCOUNTER_ID,


	// -- patient identifiers --
	/**
	* PDS generated patient identifier.  
    * Should be unique to a person.  Should not change over time.
	*/
	EMPI,

	/**
	* PDS internal patient identifier.  
	* A person can have multiple PK_PATIENT_IDs.  
	* This value is not persistant over time, so it should be generated and used within the same query 
	*	context... i.e. DO NOT generate a list of PK_PATIENT_IDs and then use those values to query PDS 
	*	several weeks later.
	*/
	PK_PATIENT_ID,

	/**
	* An MRN is provided on the CGI/PMBB CRF form at the time of consent and is a primary means of connecting 
	* to a PDS record.
	* A person can have multiple MRNs, and they can be assigned from different systems (HUP, etc.)
	*/
	MRN,

	/**
	* A node id in a graph.
	*/
	NODE_ID,

	/**
	* Social Security Number.
	* Should be unique to a person.  Should not change over time.
	*/
	SSN,

	/**
	* Placeholder for a generic patient identifier.
	* A usage example is a data table where a particular field could have one of several types of identifiers, 
	* and where the identifier type is listed in a different field.
	*/
	GENERIC_PATIENT_ID,

	/** */
	GENERIC_CODE,

	/**
	 * A specimen id.
	 *
	 */
	SPECIMEN_ID,

	/** tumor registry id */
	TUMOR_ID,

	/**
	 * A generic alphanumeric identifier.
	 */
	GENERIC_STRING_ID,

	/**
	 * A generic numeric identifier.
	 */
	GENERIC_NUMERIC_ID,

	/**
	 * A patient identifier tied to genomic data
	 */
	GENO_ID,

}