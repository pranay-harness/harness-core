package io.harness.cdng.pipeline.executions.beans.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonTypeName("parallel")
public class ParallelStageExecutionSummaryDTO implements StageExecutionSummaryDTO {
  List<StageExecutionSummaryDTO> stageExecutions;

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public ParallelStageExecutionSummaryDTO(List<StageExecutionSummaryDTO> sections) {
    this.stageExecutions = sections;
  }
}
