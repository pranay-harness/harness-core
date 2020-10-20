package io.harness.ngpipeline.pipeline.executions.beans;

import io.harness.ngpipeline.pipeline.executions.ExecutionStatus;
import io.harness.ng.core.environment.beans.EnvironmentType;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class PipelineExecutionSummaryFilter {
  List<String> serviceIdentifiers;
  List<String> envIdentifiers;
  List<String> pipelineIdentifiers;
  List<ExecutionStatus> executionStatuses;
  EnvironmentType environmentTypes;
  Long startTime;
  Long endTime;
  String searchTerm;
}
