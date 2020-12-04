package io.harness.pms.sdk.core.facilitator;

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
