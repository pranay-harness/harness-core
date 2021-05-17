package io.harness.pms.execution;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
public class ProgressNodeExecutionEventData implements NodeExecutionEventData {
  byte[] progressBytes;
}
