package io.harness.pms.plan.execution.beans.dto;

import io.harness.pms.contracts.execution.ExecutionErrorInfo;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.pms.pipeline.ExecutionTriggerInfo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import org.bson.Document;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel("PipelineExecutionSummary")
public class PipelineExecutionSummaryDTO {
  String pipelineIdentifier;
  String planExecutionId;
  String name;

  ExecutionStatus status;

  Map<String, String> tags;

  ExecutionTriggerInfo executionTriggerInfo;
  ExecutionErrorInfo executionErrorInfo;

  Map<String, Document> moduleInfo;
  Map<String, GraphLayoutNodeDTO> layoutNodeMap;
  String startingNodeId;

  Long startTs;
  Long endTs;
  Long createdAt;

  long successfulStagesCount;
  long runningStagesCount;
  long failedStagesCount;
  long totalStagesCount;
}
