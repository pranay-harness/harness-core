package io.harness.facilitator;

import com.google.inject.Inject;

import io.harness.serializer.KryoSerializer;

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
