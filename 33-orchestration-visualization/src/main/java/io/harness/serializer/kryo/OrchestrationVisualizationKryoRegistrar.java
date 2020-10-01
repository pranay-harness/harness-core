package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.esotericsoftware.kryo.Kryo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.GraphVertex;
import io.harness.beans.OrchestrationGraph;
import io.harness.beans.internal.EdgeListInternal;
import io.harness.beans.internal.OrchestrationAdjacencyListInternal;
import io.harness.serializer.KryoRegistrar;

@OwnedBy(CDC)
public class OrchestrationVisualizationKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(GraphVertex.class, 3302);
    kryo.register(OrchestrationGraph.class, 3304);
    kryo.register(OrchestrationAdjacencyListInternal.class, 3305);
    kryo.register(EdgeListInternal.class, 3306);
  }
}
