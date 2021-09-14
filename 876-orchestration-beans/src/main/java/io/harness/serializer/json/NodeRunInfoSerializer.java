/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer.json;

import io.harness.pms.contracts.execution.run.NodeRunInfo;

public class NodeRunInfoSerializer extends ProtoJsonSerializer<NodeRunInfo> {
  public NodeRunInfoSerializer() {
    super(NodeRunInfo.class);
  }
}
