package io.harness.event;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@OwnedBy(PIPELINE)
@Data
@Builder
@FieldNameConstants(innerTypeName = "ExecutionStatsKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExecutionStats {
  long nReturned;
  long executionTimeMillis;
  long totalDocsExamined;
  InputStage executionStages;
}
