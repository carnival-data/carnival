package carnival.core.cli



/** 
 * An enumeration of command categories.
 *
 */
enum CommandCategory {
    STATUS('Status'), 
    GRAPH_MODIFICATION('Graph Modification'), 
    REPORT('Report'),
    PATIENT_REPORT('Patent Report'), 
    ENCOUNTER_REPORT('Encounter Report'), 
    INVENTORY_REPORT('Inventory Report'),
    DRIVETRAIN_UPDATE('Drivetrain Update'),
    PDS_UPDATE('Penn Data Store Update'),
    APP('Application Navigation'),
    COHORT_MANIPULATION('Patient Cohort Manipulation'),
    SETTINGS('App Settings'),
    REAPERS('Reapers'),
    CONCLUSIONATORS('Conclusionators'),
    REASONERS('Reasoners')

    final String label

    private CommandCategory(String label) {
        this.label = label
    }
}
