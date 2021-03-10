package io.harness.cvng.beans;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CVNGPerpetualTaskDTO {
  String delegateId;
  String accountId;

  CVNGPerpetualTaskUnassignedReason cvngPerpetualTaskUnassignedReason;
  CVNGPerpetualTaskState cvngPerpetualTaskState;
}
