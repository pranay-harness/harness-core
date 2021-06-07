package io.harness.ngtriggers.expressions.functors;

import static io.harness.ngtriggers.Constants.EVENT_PAYLOAD;
import static io.harness.ngtriggers.Constants.PAYLOAD;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.expression.LateBindingValue;
import io.harness.ngtriggers.helpers.TriggerAmbianceHelper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.yaml.utils.JsonPipelineUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public class TriggerPayloadFunctor implements LateBindingValue {
  private final Ambiance ambiance;
  private final String payload;

  public TriggerPayloadFunctor(Ambiance ambiance, String payload) {
    this.ambiance = ambiance;
    this.payload = payload;
  }

  @Override
  public Object bind() {
    Map<String, Object> jsonObject = TriggerAmbianceHelper.buildJsonObjectFromAmbiance(ambiance);
    jsonObject.put(EVENT_PAYLOAD, payload);
    // payload
    try {
      jsonObject.put(PAYLOAD, JsonPipelineUtils.read(payload, HashMap.class));
    } catch (IOException e) {
      throw new InvalidRequestException("Event payload could not be converted to a hashmap");
    }
    return jsonObject;
  }
}
