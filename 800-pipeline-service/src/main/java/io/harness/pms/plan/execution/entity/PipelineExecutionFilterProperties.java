package io.harness.pms.plan.execution.entity;

import static io.harness.filter.FilterConstants.PIPELINE_FILTER;

import io.harness.filter.entity.FilterProperties;
import io.harness.pms.execution.ExecutionStatus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModel;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel("PipelineExecutionFilterProperties")
@JsonTypeName(PIPELINE_FILTER)
public class PipelineExecutionFilterProperties extends FilterProperties {
  private ExecutionStatus status;
  private String pipelineName;
}
