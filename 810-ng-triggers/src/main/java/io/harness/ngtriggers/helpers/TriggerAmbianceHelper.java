package io.harness.ngtriggers.helpers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.triggers.ParsedPayload;

import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(PIPELINE)
public class TriggerAmbianceHelper {
  public String getEventPayload(Ambiance ambiance) {
    return ambiance.getMetadata().getTriggerPayload().getJsonPayload();
  }

  public ParsedPayload getParsedPayload(Ambiance ambiance) {
    return ambiance.getMetadata().getTriggerPayload().getParsedPayload();
  }

  public Map<String, String> getHeadersMap(Ambiance ambiance) {
    return ambiance.getMetadata().getTriggerPayload().getHeadersMap();
  }
}
