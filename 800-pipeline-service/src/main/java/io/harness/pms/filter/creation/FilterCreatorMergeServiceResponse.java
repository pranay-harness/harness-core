/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.filter.creation;

import io.harness.pms.contracts.plan.GraphLayoutNode;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FilterCreatorMergeServiceResponse {
  Map<String, String> filters;
  Map<String, GraphLayoutNode> layoutNodeMap;
  int stageCount;
  List<String> stageNames;
}
