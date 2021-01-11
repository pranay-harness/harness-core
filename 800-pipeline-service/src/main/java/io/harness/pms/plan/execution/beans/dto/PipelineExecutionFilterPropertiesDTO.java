package io.harness.pms.plan.execution.beans.dto;

import static io.harness.filter.FilterConstants.PIPELINE_FILTER;

import io.harness.filter.FilterType;
import io.harness.filter.dto.FilterPropertiesDTO;
import io.harness.filter.entity.FilterProperties;
import io.harness.ng.core.mapper.TagMapper;
import io.harness.pms.contracts.plan.ExecutionTriggerInfo;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.pms.plan.execution.entity.PipelineExecutionFilterProperties;

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
public class PipelineExecutionFilterPropertiesDTO extends FilterPropertiesDTO {
  private ExecutionStatus status;
  private String pipelineName;

  @Override
  public FilterProperties toEntity() {
    ModelMapper modelMapper = new ModelMapper();
    PipelineExecutionFilterProperties filterProperties = modelMapper.map(this, PipelineExecutionFilterProperties.class);
    filterProperties.setType(getFilterType());
    filterProperties.setTags(TagMapper.convertToList(getTags()));
    return filterProperties;
  }

  @Override
  public FilterType getFilterType() {
    return FilterType.PIPELINE;
  }
}
