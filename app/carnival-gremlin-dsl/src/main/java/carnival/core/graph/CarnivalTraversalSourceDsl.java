package carnival.core.graph;



import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;

import org.apache.commons.configuration.Configuration;
import org.apache.tinkerpop.gremlin.process.computer.Computer;
import org.apache.tinkerpop.gremlin.process.computer.GraphComputer;
import org.apache.tinkerpop.gremlin.process.remote.RemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.Bindings;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalStrategies;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.step.map.AddVertexStartStep;
import org.apache.tinkerpop.gremlin.process.traversal.step.map.GraphStep;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.DefaultGraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.neo4j.structure.*;

import static org.apache.tinkerpop.gremlin.neo4j.process.traversal.LabelP.of;
import static org.apache.tinkerpop.gremlin.process.traversal.P.within;



/**
 *
 *
 */
public class CarnivalTraversalSourceDsl extends GraphTraversalSource {

    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    ///////////////////////////////////////////////////////////////////////////

    /**
     *
     *
     */
    public CarnivalTraversalSourceDsl(final Graph graph) {
        super(graph);
    }


    /**
     *
     *
     */
    public CarnivalTraversalSourceDsl(final Graph graph, final TraversalStrategies strategies) {
        super(graph, strategies);
    }


    ///////////////////////////////////////////////////////////////////////////
    // IDENTIFIERS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * 
     *
     */
    public GraphTraversal<Vertex, Vertex> identifiers(String... referentClassNames) {
        GraphTraversalSource clone = this.clone();

        // Manually add a "start" step for the traversal in this case the equivalent of V(). GraphStep is marked
        // as a "start" step by passing "true" in the constructor.
        clone.getBytecode().addStep(GraphTraversal.Symbols.V);
        GraphTraversal<Vertex, Vertex> traversal = new DefaultGraphTraversal<>(clone);
        traversal.asAdmin().addStep(new GraphStep<>(traversal.asAdmin(), Vertex.class, true));

        traversal = traversal.hasLabel("Identifier").as("a");
        if (referentClassNames.length > 0) traversal = traversal.out("refers_to").hasLabel("Class").has("name", P.within(referentClassNames));
        traversal = traversal.select("a");

        return traversal;
    }  
    


    ///////////////////////////////////////////////////////////////////////////
    // PATIENTS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * All verticies with the 'patient' label
     *
     */
    public GraphTraversal<Vertex, Vertex> patients() {
        GraphTraversalSource clone = this.clone();

        // Manually add a "start" step for the traversal in this case the equivalent of V(). GraphStep is marked
        // as a "start" step by passing "true" in the constructor.
        clone.getBytecode().addStep(GraphTraversal.Symbols.V);
        GraphTraversal<Vertex, Vertex> traversal = new DefaultGraphTraversal<>(clone);
        traversal.asAdmin().addStep(new GraphStep<>(traversal.asAdmin(), Vertex.class, true));

        traversal = traversal.hasLabel("Patient");
        return traversal;
    }


    /**
     * 
     *
     */
    public GraphTraversal<Vertex, Vertex> patientsIdentifiedByGenericId(String idCreationFacility, String identifierValue) {
        // set up a GraphTraversalSource
        GraphTraversalSource clone = this.clone();

        GraphTraversal<Vertex, Vertex> traversal = clone
            .V().hasLabel("Patient")
            .match(   
                __.as("a").out("is_identified_by").hasLabel("Identifier").as("b"),
                __.as("b").out("is_instance_of").hasLabel("IdentifierClass").has("name", "generic_patient_id"),
                __.as("b").out("was_created_by").has("name", idCreationFacility),
                __.as("b").has("value", identifierValue)
            ).select("a");

        return traversal;
    }


    /**
     * 
     *
     */
    public GraphTraversal<Vertex, Vertex> patientsIdentifiedByGenericId(String idCreationFacility, Collection<Object> identifierValues) {
        // collect the identifier values into a collection of strings
        Set<String> idvals = Util.toIdValues(identifierValues);

        // set up a GraphTraversalSource
        GraphTraversalSource clone = this.clone();

        GraphTraversal<Vertex, Vertex> traversal = clone
            .V().hasLabel("Patient")
            .match(   
                __.as("a").out("is_identified_by").hasLabel("Identifier").as("b"),
                __.as("b").out("is_instance_of").hasLabel("IdentifierClass").has("name", "generic_patient_id"),
                __.as("b").out("was_created_by").has("name", idCreationFacility),
                __.as("b").has("value", within(idvals))
            ).select("a");

        return traversal;
    }



    /**
     * 
     *
     */
    public GraphTraversal<Vertex, Vertex> patientsIdentifiedByMrn(String idCreationFacility, Integer identifierValue) {
        // set up a GraphTraversalSource
        GraphTraversalSource clone = this.clone();

        GraphTraversal<Vertex, Vertex> traversal = clone
            .V().hasLabel("Patient")
            .match(   
                __.as("a").out("is_identified_by").hasLabel("Identifier").as("b"),
                __.as("b").out("is_instance_of").hasLabel("IdentifierClass").has("name", "mrn"),
                __.as("b").out("was_created_by").has("name", idCreationFacility),
                __.as("b").has("value", String.valueOf(identifierValue))
            ).select("a");

        return traversal;
    }


    /**
     * 
     *
     */
    public GraphTraversal<Vertex, Vertex> patientsIdentifiedByMrn(String idCreationFacility, Collection<Object> identifierValues) {
        // collect the identifier values into a collection of strings
        Set<String> idvals = Util.toIdValues(identifierValues);

        // set up a GraphTraversalSource
        GraphTraversalSource clone = this.clone();

        GraphTraversal<Vertex, Vertex> traversal = clone
            .V().hasLabel("Patient")
            .match(   
                __.as("a").out("is_identified_by").hasLabel("Identifier").as("b"),
                __.as("b").out("is_instance_of").hasLabel("IdentifierClass").has("name", "mrn"),
                __.as("b").out("was_created_by").has("name", idCreationFacility),
                __.as("b").has("value", within(idvals))
            ).select("a");

        return traversal;
    }



    /**
     * 
     *
     */
    public GraphTraversal<Vertex, Vertex> patientsIdentifiedByEmpi(Object... identifierValues) {
        return patientsIdentifiedByEmpi(Util.toSet(identifierValues));
    }


    /**
     * 
     *
     */
    public GraphTraversal<Vertex, Vertex> patientsIdentifiedByEmpi(Collection<Object> identifierValues) {
        // collect the identifier values into a collection of strings
        Set<String> idvals = Util.toIdValues(identifierValues);

        // set up a GraphTraversalSource
        GraphTraversalSource clone = this.clone();

        GraphTraversal<Vertex, Vertex> traversal = clone
            .V().hasLabel("Patient")
            .match(   
                __.as("a").out("is_identified_by").hasLabel("Identifier").as("b"),
                __.as("b").out("is_instance_of").hasLabel("IdentifierClass").has("name", "empi"),
                __.as("b").has("value", within(idvals))
            ).select("a");

        return traversal;

        // ALL THE withSideEffect stuff below... we could not get it to work.

        /*for (Object strat: clone.getStrategies().toList()) {
            System.out.println("strat 1: " + strat.toString());
        }*/

        // set up a side effect on the traversal source (this)
        /*clone = clone.withSideEffect("ivs", idvals);
        for (Object strat: clone.getStrategies().toList()) {
            System.out.println("strat 2: " + strat.toString());
        }*/

        // Manually add a "start" step for the traversal in this case the equivalent of V(). GraphStep is marked
        // as a "start" step by passing "true" in the constructor.
        /*clone.getBytecode().addStep(GraphTraversal.Symbols.V);
        GraphTraversal<Vertex, Vertex> traversal = new DefaultGraphTraversal<>(clone);
        traversal.asAdmin().addStep(new GraphStep<>(traversal.asAdmin(), Vertex.class, true));*/

        // GraphTraversal<Vertex, Vertex> traversal = clone.withSideEffect("ivs", idvals);

        //String[] idvArray = (String[])idvals.toArray();

        /*
        String k = "a";
        GraphTraversal<Vertex, Vertex> traversal = clone
            //.withSideEffect(k, idvals.toArray())
            .V().hasLabel("Patient").as("p")
            .out("is_identified_by")
            .has("value", within(idvals))
            //.values("value").where(within(k))
            //.empis().values("value").where(within("a"))
            .select("p");
        */

        /*def patsA = g
            .patients().empis().values('value').where(within('a'))
            .toList()*/

        /*
        traversal = traversal.match(   
            __.as("a").out("is_identified_by").hasLabel("Identifier").as("b"),
            __.as("b").out("is_instance_of").hasLabel("IdentifierClass").has("name", identifierClassName),
            __.as("b").values("value").where(P.within("ivs"))
        ).select("a");
        */
    }    


    /**
     * 
     *
     */
    public GraphTraversal<Vertex, Vertex> patientsIdentifiedByPacketId(Object... identifierValues) {
        return patientsIdentifiedByPacketId(Util.toSet(identifierValues));
    }


    /**
     * 
     *
     */
    public GraphTraversal<Vertex, Vertex> patientsIdentifiedByPacketId(Collection<Object> identifierValues) {
        // collect the identifier values into a collection of strings
        Set<String> idvals = Util.toIdValues(identifierValues);

        // set up a GraphTraversalSource
        GraphTraversalSource clone = this.clone();

        GraphTraversal<Vertex, Vertex> traversal = clone
            .V().hasLabel("Patient")
            .match(   
                __.as("a").out("is_identified_by").hasLabel("Identifier").as("b"),
                __.as("b").out("is_instance_of").hasLabel("IdentifierClass").has("name", "encounter_pack_id"),
                __.as("b").has("value", within(idvals))
            ).select("a");

        return traversal;
    }



    ///////////////////////////////////////////////////////////////////////////
    // SINGLETON
    ///////////////////////////////////////////////////////////////////////////

    /**
     * The vertex with the 'IdentifierClass' label and the name 'className'
     *
     */
    public GraphTraversal<Vertex, Vertex> identifierClass(String className) {
        GraphTraversalSource clone = this.clone();

        // Manually add a "start" step for the traversal in this case the equivalent of V(). GraphStep is marked
        // as a "start" step by passing "true" in the constructor.
        clone.getBytecode().addStep(GraphTraversal.Symbols.V);
        GraphTraversal<Vertex, Vertex> traversal = new DefaultGraphTraversal<>(clone);
        traversal.asAdmin().addStep(new GraphStep<>(traversal.asAdmin(), Vertex.class, true));

        traversal = traversal.hasLabel("IdentifierClass").has("name", className);
        return traversal;
    }




    ///////////////////////////////////////////////////////////////////////////
    //
    // BIOBANK
    //
    ///////////////////////////////////////////////////////////////////////////


    ///////////////////////////////////////////////////////////////////////////
    // ENCOUNTERS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * 
     *
     */
    public GraphTraversal<Vertex, Vertex> consentEncounters() {
        GraphTraversalSource clone = this.clone();

        // Manually add a "start" step for the traversal in this case the equivalent of V(). GraphStep is marked
        // as a "start" step by passing "true" in the constructor.
        clone.getBytecode().addStep(GraphTraversal.Symbols.V);
        GraphTraversal<Vertex, Vertex> traversal = new DefaultGraphTraversal<>(clone);
        traversal.asAdmin().addStep(new GraphStep<>(traversal.asAdmin(), Vertex.class, true));

        traversal = traversal.hasLabel("BiobankEncounter").out("has_activity").has("name", "consent");

        return traversal;
    }  


    /**
     * 
     *
     */
    public GraphTraversal<Vertex, Vertex> biobankEncounters(String... protocolNames) {
        GraphTraversalSource clone = this.clone();

        // Manually add a "start" step for the traversal in this case the equivalent of V(). GraphStep is marked
        // as a "start" step by passing "true" in the constructor.
        clone.getBytecode().addStep(GraphTraversal.Symbols.V);
        GraphTraversal<Vertex, Vertex> traversal = new DefaultGraphTraversal<>(clone);
        traversal.asAdmin().addStep(new GraphStep<>(traversal.asAdmin(), Vertex.class, true));

        traversal = traversal.hasLabel("BiobankEncounter");
        if (protocolNames.length > 0) traversal = traversal.has("protocol", P.within(protocolNames));

        return traversal;
    }


    ///////////////////////////////////////////////////////////////////////////
    // PATIENTS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * 
     *
     */
    public GraphTraversal<Vertex, Vertex> patientsIdentifiedByPkPatientId(String idScopeName, Collection<Object> identifierValues) {
        // collect the identifier values into a collection of strings
        Set<String> idvals = Util.toIdValues(identifierValues);

        // set up a GraphTraversalSource
        GraphTraversalSource clone = this.clone();

        GraphTraversal<Vertex, Vertex> traversal = clone
            .V().hasLabel("Patient")
            .match(   
                __.as("a").out("is_identified_by").hasLabel("Identifier").as("b"),
                __.as("b").out("is_instance_of").hasLabel("IdentifierClass").has("name", "pk_patient_id"),
                __.as("b").out("is_scoped_by").has("name", idScopeName),
                __.as("b").has("value", within(idvals))
            ).select("a");

        return traversal;
    }


    /**
     * 
     *
     */
    public GraphTraversal<Vertex, Vertex> patientsIdentifiedByPkPatientId(String idScopeName, String identifierValue) {
        // set up a GraphTraversalSource
        GraphTraversalSource clone = this.clone();

        GraphTraversal<Vertex, Vertex> traversal = clone
            .V().hasLabel("Patient")
            .match(   
                __.as("a").out("is_identified_by").hasLabel("Identifier").as("b"),
                __.as("b").out("is_instance_of").hasLabel("IdentifierClass").has("name", "pk_patient_id"),
                __.as("b").out("is_scoped_by").has("name", idScopeName),
                __.as("b").has("value", identifierValue)
            ).select("a");

        return traversal;
    }



}
