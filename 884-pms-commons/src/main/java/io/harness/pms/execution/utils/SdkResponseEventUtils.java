/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.execution.utils;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.AutoLogContext;
import io.harness.logging.AutoLogContext.OverrideBehavior;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;

import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PIPELINE)
public class SdkResponseEventUtils {
  public AutoLogContext obtainLogContext(SdkResponseEventProto sdkResponseEvent) {
    return new AutoLogContext(logContextMap(sdkResponseEvent), OverrideBehavior.OVERRIDE_NESTS);
  }

  private Map<String, String> logContextMap(SdkResponseEventProto sdkResponseEvent) {
    Map<String, String> logContext = new HashMap<>();
    logContext.put("sdkResponseEventType", sdkResponseEvent.getSdkResponseEventType().name());
    logContext.put("nodeExecutionId", sdkResponseEvent.getNodeExecutionId());
    return logContext;
  }
}
