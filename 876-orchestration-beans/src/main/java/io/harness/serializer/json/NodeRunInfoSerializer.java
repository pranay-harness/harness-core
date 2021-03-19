package io.harness.serializer.json;

import io.harness.pms.contracts.execution.run.NodeRunInfo;

public class NodeRunInfoSerializer extends ProtoJsonSerializer<NodeRunInfo> {
  public NodeRunInfoSerializer() {
    super(NodeRunInfo.class);
  }
}
