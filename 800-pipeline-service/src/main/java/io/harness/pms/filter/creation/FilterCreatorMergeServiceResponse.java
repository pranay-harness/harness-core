package io.harness.pms.filter.creation;

import io.harness.pms.contracts.plan.GraphLayoutNode;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FilterCreatorMergeServiceResponse {
  Map<String, String> filters;
  Map<String, GraphLayoutNode> layoutNodeMap;
  int stageCount;
}
