package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.beans.EdgeList;
import io.harness.beans.Graph;
import io.harness.beans.GraphVertex;
import io.harness.beans.OrchestrationAdjacencyList;
import io.harness.beans.OrchestrationGraphInternal;
import io.harness.beans.Subgraph;
import io.harness.serializer.KryoRegistrar;

public class OrchestrationVisualizationKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(Graph.class, 3301);
    kryo.register(GraphVertex.class, 3302);
    kryo.register(Subgraph.class, 3303);
    kryo.register(OrchestrationGraphInternal.class, 3304);
    kryo.register(OrchestrationAdjacencyList.class, 3305);
    kryo.register(EdgeList.class, 3306);
  }
}
