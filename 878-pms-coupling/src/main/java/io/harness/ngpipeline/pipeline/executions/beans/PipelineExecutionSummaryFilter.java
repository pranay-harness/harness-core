package io.harness.ngpipeline.pipeline.executions.beans;

import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.pms.execution.ExecutionStatus;

import java.util.List;
import lombok.Builder;
import lombok.Value;

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
