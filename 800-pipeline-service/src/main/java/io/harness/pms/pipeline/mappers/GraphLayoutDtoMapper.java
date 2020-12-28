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
        .status(ExecutionStatus.NOT_STARTED)
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
