package io.harness.cdng.pipeline.executions.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonTypeName("parallel")
public class ParallelStageExecutionSummary implements StageExecutionSummary {
  List<StageExecutionSummary> stageExecutionSummaries;
}
