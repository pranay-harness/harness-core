package io.harness.pms.sdk.core;

import io.harness.pms.plan.GraphLayoutNode;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FilterCreatorMergeServiceResponse {
  Map<String, String> filters;
  Map<String, GraphLayoutNode> layoutNodeMap;
  int stageCount;
  String startingNodeId;
}
