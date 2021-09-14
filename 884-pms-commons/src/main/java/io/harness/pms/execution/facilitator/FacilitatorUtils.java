/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.execution.facilitator;

import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import java.time.Duration;

public class FacilitatorUtils {
  @Inject private KryoSerializer kryoSerializer;

  public Duration extractWaitDurationFromDefaultParams(byte[] parameters) {
    Duration waitDuration = Duration.ofSeconds(0);
    if (parameters != null && parameters.length > 0) {
      DefaultFacilitatorParams facilitatorParameters = (DefaultFacilitatorParams) kryoSerializer.asObject(parameters);
      waitDuration = facilitatorParameters.getWaitDurationSeconds();
    }
    return waitDuration;
  }
}
