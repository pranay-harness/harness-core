package io.harness.pms.plan.execution.beans.dto;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.dto.FailureInfoDTO;
import io.harness.pms.contracts.execution.ExecutionErrorInfo;
import io.harness.pms.contracts.execution.run.NodeRunInfo;
import io.harness.pms.contracts.execution.skip.SkipInfo;
import io.harness.pms.execution.ExecutionStatus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.bson.Document;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel("GraphLayoutNode")
@OwnedBy(PIPELINE)
@Schema(name = "GraphLayoutNode", description = "This is the view of the Graph for execution of the Pipeline.")
public class GraphLayoutNodeDTO {
  String nodeType;
  String nodeGroup;
  String nodeIdentifier;
  String name;
  String nodeUuid;
  ExecutionStatus status;
  String module;
  Map<String, Document> moduleInfo;
  private Long startTs;
  private Long endTs;
  EdgeLayoutListDTO edgeLayoutList;
  SkipInfo skipInfo;
  NodeRunInfo nodeRunInfo;
  Boolean barrierFound;
  @Deprecated ExecutionErrorInfo failureInfo;
  FailureInfoDTO failureInfoDTO;
}
