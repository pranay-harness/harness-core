package io.harness.ngpipeline.pipeline.executions.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.util.List;

@Data
@Builder
@FieldNameConstants(innerTypeName = "ParallelStageExecutionSummaryKeys")
@JsonTypeName("parallel")
public class ParallelStageExecutionSummary implements StageExecutionSummary {
  List<StageExecutionSummary> stageExecutionSummaries;
}
