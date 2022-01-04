package io.harness.cvng.beans;

import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CVNGPerpetualTaskDTO {
  @Nullable String delegateId;
  String accountId;

  CVNGPerpetualTaskUnassignedReason cvngPerpetualTaskUnassignedReason;
  CVNGPerpetualTaskState cvngPerpetualTaskState;
}
