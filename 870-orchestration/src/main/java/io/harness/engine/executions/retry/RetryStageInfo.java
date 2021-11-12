package io.harness.engine.executions.retry;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.execution.ExecutionStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "ResumeStageDetailKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(name = "RetryStageInfo", description = "This is stage level info in Retry Failed Pipeline")
@OwnedBy(HarnessTeam.PIPELINE)
public class RetryStageInfo {
  private String name;
  private String identifier;
  private ExecutionStatus status;
  private Long createdAt;
  private String parentId;
  private String nextId;
}
