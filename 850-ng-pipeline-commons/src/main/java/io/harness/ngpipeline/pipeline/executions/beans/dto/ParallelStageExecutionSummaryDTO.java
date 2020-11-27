package io.harness.ngpipeline.pipeline.executions.beans.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;

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
