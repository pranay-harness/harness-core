package io.harness.pms.sdk.core;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PIPELINE)
public class PmsSdkCoreEventsFrameworkConstants {
  public static final String SDK_RESPONSE_EVENT_PRODUCER = "SDK_RESPONSE_EVENT_PRODUCER";
  public static final String PARTIAL_PLAN_RESPONSE_EVENT_PRODUCER = "PARTIAL_PLAN_RESPONSE_EVENT_PRODUCER";
}
