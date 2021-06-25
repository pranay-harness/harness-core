package io.harness;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PIPELINE)
public class OrchestrationEventsFrameworkConstants {
  public static final String SDK_RESPONSE_EVENT_CONSUMER = "SDK_RESPONSE_EVENT_CONSUMER";
  public static final String SDK_INTERRUPT_EVENT_NOTIFY_CONSUMER = "SDK_INTERRUPT_EVENT_NOTIFY_CONSUMER";
}
