package io.harness.serializer.json;

import io.harness.pms.contracts.plan.GraphLayoutInfo;

public class LayoutNodeInfoSerializer extends ProtoJsonSerializer<GraphLayoutInfo> {
  public LayoutNodeInfoSerializer() {
    super(GraphLayoutInfo.class);
  }
}
