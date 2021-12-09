package io.harness;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PIPELINE)
public class OrchestrationEventsFrameworkConstants {
  public static final String SDK_RESPONSE_EVENT_CONSUMER = "SDK_RESPONSE_EVENT_CONSUMER";
  public static final int SDK_RESPONSE_EVENT_BATCH_SIZE = 1;

  public static final String PARTIAL_PLAN_EVENT_CONSUMER = "PARTIAL_PLAN_EVENT_RESPONSE_CONSUMER";
  public static final int PARTIAL_PLAN_EVENT_BATCH_SIZE = 1;
}
