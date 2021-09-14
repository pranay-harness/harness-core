/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.serializer.kryo.serializers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.run.NodeRunInfo;
import io.harness.serializer.kryo.ProtobufKryoSerializer;

@OwnedBy(HarnessTeam.PIPELINE)
public class NodeRunInfoKryoSerializer extends ProtobufKryoSerializer<NodeRunInfo> {
  private static NodeRunInfoKryoSerializer instance;

  public NodeRunInfoKryoSerializer() {}

  public static synchronized NodeRunInfoKryoSerializer getInstance() {
    if (instance == null) {
      instance = new NodeRunInfoKryoSerializer();
    }
    return instance;
  }
}
