package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.beans.Graph;
import io.harness.beans.GraphVertex;
import io.harness.beans.Subgraph;
import io.harness.serializer.KryoRegistrar;

public class OrchestrationVisualizationKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(Graph.class, 3301);
    kryo.register(GraphVertex.class, 3302);
    kryo.register(Subgraph.class, 3303);
  }
}
