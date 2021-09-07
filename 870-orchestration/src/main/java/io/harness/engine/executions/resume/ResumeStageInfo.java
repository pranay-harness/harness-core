package io.harness.engine.executions.resume;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.execution.ExecutionStatus;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "ResumeStageDetailKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(HarnessTeam.PIPELINE)
public class ResumeStageInfo {
  private String name;
  private String identifier;
  private ExecutionStatus status;
  private Long createdAt;
  private String parentId;
  private String nextId;
}
