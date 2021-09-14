/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.GraphVertex;
import io.harness.beans.OrchestrationGraph;
import io.harness.beans.RepresentationStrategy;
import io.harness.beans.internal.EdgeListInternal;
import io.harness.beans.internal.OrchestrationAdjacencyListInternal;
import io.harness.dto.GraphDelegateSelectionLogParams;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;
import org.bson.Document;

@OwnedBy(CDC)
public class OrchestrationVisualizationKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(GraphVertex.class, 3302);
    kryo.register(OrchestrationGraph.class, 3304);
    kryo.register(OrchestrationAdjacencyListInternal.class, 3305);
    kryo.register(EdgeListInternal.class, 3306);
    kryo.register(Document.class, 3307);
    kryo.register(GraphDelegateSelectionLogParams.class, 3308);

    kryo.register(RepresentationStrategy.class, 35012);
  }
}
