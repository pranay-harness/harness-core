package io.harness.ngtriggers.functor;

import io.harness.exception.InvalidRequestException;
import io.harness.expression.LateBindingValue;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.yaml.utils.JsonPipelineUtils;

import java.io.IOException;
import java.util.HashMap;

public class EventPayloadFunctor implements LateBindingValue {
  private final Ambiance ambiance;

  public EventPayloadFunctor(Ambiance ambiance) {
    this.ambiance = ambiance;
  }

  @Override
  public Object bind() {
    try {
      return JsonPipelineUtils.read(AmbianceHelper.getEventPayload(ambiance), HashMap.class);
    } catch (IOException e) {
      throw new InvalidRequestException("Event payload could not be converted to a hashmap");
    }
  }
}
