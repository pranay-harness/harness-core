package io.harness.cdng.pipeline.executions.beans.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonTypeName("parallel")
public class ParallelStageExecutionDTO implements StageExecutionDTO {
  List<StageExecutionDTO> stageExecutions;

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public ParallelStageExecutionDTO(List<StageExecutionDTO> sections) {
    this.stageExecutions = sections;
  }
}
