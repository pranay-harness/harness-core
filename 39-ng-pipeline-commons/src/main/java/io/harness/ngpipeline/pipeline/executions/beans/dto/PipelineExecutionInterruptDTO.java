package io.harness.ngpipeline.pipeline.executions.beans.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.harness.ngpipeline.pipeline.executions.beans.PipelineExecutionInterruptType;
import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel("pipelineExecutionInterrupt")
@JsonInclude(Include.NON_NULL)
public class PipelineExecutionInterruptDTO {
  String id;
  @NonNull PipelineExecutionInterruptType type;
  @NonNull String planExecutionId;
}
