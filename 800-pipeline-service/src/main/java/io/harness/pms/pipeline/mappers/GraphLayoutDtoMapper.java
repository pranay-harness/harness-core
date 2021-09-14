/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.pipeline.mappers;

import io.harness.pms.contracts.plan.EdgeLayoutList;
import io.harness.pms.contracts.plan.GraphLayoutNode;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.pms.plan.execution.beans.dto.EdgeLayoutListDTO;
import io.harness.pms.plan.execution.beans.dto.GraphLayoutNodeDTO;

import lombok.experimental.UtilityClass;

@UtilityClass
public class GraphLayoutDtoMapper {
  public GraphLayoutNodeDTO toDto(GraphLayoutNode graphLayoutNode) {
    return GraphLayoutNodeDTO.builder()
        .nodeIdentifier(graphLayoutNode.getNodeIdentifier())
        .nodeType(graphLayoutNode.getNodeType())
        .nodeUuid(graphLayoutNode.getNodeUUID())
        .status(ExecutionStatus.NOTSTARTED)
        .name(graphLayoutNode.getName())
        .nodeGroup(graphLayoutNode.getNodeGroup())
        .edgeLayoutList(toDto(graphLayoutNode.getEdgeLayoutList()))
        .build();
  }

  private EdgeLayoutListDTO toDto(EdgeLayoutList edgeLayoutList) {
    return EdgeLayoutListDTO.builder()
        .currentNodeChildren(edgeLayoutList.getCurrentNodeChildrenList())
        .nextIds(edgeLayoutList.getNextIdsList())
        .build();
  }
}
