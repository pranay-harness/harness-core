package io.harness.pms.execution;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.events.SdkResponseEventRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
public class SdkResponseEventInternal {
  SdkResponseEventType sdkResponseEventType;
  SdkResponseEventRequest sdkResponseEventRequest;
}
