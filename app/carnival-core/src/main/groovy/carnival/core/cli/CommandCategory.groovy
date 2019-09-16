package carnival.core.cli



/** 
 * An enumeration of command categories.
 *
 */
enum CommandCategory {
    APP('Application Navigation'),
    SETTINGS('App Settings'),
    STATUS('Status'), 
    GRAPH_MODIFICATION('Graph Modification'), 
    REPORT('Report'),
    SOWER('Sow data to external system'),
    CLEAN('Delete out-of-date information'),
    COHORT_MANIPULATION('Patient Cohort Manipulation'),
    REAPERS('Reapers'),
    CONCLUSIONATORS('Conclusionators'),
    REASONERS('Reasoners')

    final String label

    private CommandCategory(String label) {
        this.label = label
    }
}
