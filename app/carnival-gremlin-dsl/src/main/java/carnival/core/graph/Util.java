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

import static org.apache.tinkerpop.gremlin.neo4j.process.traversal.LabelP.of;
import static org.apache.tinkerpop.gremlin.process.traversal.P.within;



/**
 *
 *
 */
public class Util {


    ///////////////////////////////////////////////////////////////////////////
    // STATIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     *
     *
     */
    static public Set<Object> toSet(Object[] identifierValues) {
        Set<Object> idvals = new HashSet<Object>();
        for (int i = 0; i < identifierValues.length; ++i) {
            Object idv = identifierValues[i];
            idvals.add(idv);
        }
        return idvals;
    }


    /**
     *
     *
     */
    static public Set<String> toStringSetNoNullValues(Collection<Object> vals) {
        Set<String> out = new HashSet<String>();
        for (Object o: vals) {
            if (o != null) out.add(String.valueOf(o));
        }
        return out;
    }


    /**
     *
     *
     */
    static public Set<String> toIdValues(Collection<Object> vals) {
        Set<String> out = new HashSet<String>();
        for (Object o: vals) {
            if (o != null) out.add(String.valueOf(o));
        }
        if (out.size() < 1) throw new RuntimeException("no non-null identifier values");
        return out;
    }


}
