package io.harness.serializer.spring.converters.graphlayout;

import io.harness.pms.contracts.plan.GraphLayoutInfo;
import io.harness.serializer.spring.ProtoReadConverter;

public class LayoutNodeInfoReadConverter extends ProtoReadConverter<GraphLayoutInfo> {
  public LayoutNodeInfoReadConverter() {
    super(GraphLayoutInfo.class);
  }
}
