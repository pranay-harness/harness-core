package io.harness.pms.pipeline.resource;

import io.harness.pms.execution.Status;
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
@ApiModel("PlanExecutionSummary")
public class PlanExecutionSummaryDTO {
  String pipelineId;
  String name;

  Status status;

  Long createdAt;
  Long duration;

  ExecutionTriggerInfo executionTriggerInfo;

  Map<String, GraphLayoutNodeDTO> layoutNodeMap;
  String startingNodeId;

  Map<String, Document> moduleInfo;
  Map<String, String> tags;
}
