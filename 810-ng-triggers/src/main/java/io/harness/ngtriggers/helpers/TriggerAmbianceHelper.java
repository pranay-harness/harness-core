package io.harness.ngtriggers.helpers;

import io.harness.pms.contracts.ambiance.Ambiance;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TriggerAmbianceHelper {
  public String getEventPayload(Ambiance ambiance) {
    return ambiance.getMetadata().getTriggerPayload().getJsonPayload();
  }
}
