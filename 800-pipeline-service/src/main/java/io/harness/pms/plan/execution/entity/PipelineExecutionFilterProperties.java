package io.harness.pms.plan.execution.entity;

import static io.harness.filter.FilterConstants.PIPELINE_FILTER;

import io.harness.filter.dto.FilterPropertiesDTO;
import io.harness.filter.entity.FilterProperties;
import io.harness.ng.core.mapper.TagMapper;
import io.harness.pms.contracts.plan.ExecutionTriggerInfo;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.pms.plan.execution.beans.dto.PipelineExecutionFilterPropertiesDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModel;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.modelmapper.ModelMapper;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel("PipelineExecutionFilterProperties")
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(PIPELINE_FILTER)
@NoArgsConstructor
@AllArgsConstructor
public class PipelineExecutionFilterProperties extends FilterProperties {
  private ExecutionStatus status;
  private String pipelineName;
}
