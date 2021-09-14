/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ngtriggers.expressions.functors;

import static io.harness.ngtriggers.Constants.EVENT_PAYLOAD;
import static io.harness.ngtriggers.Constants.PAYLOAD;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.expression.LateBindingValue;
import io.harness.ngtriggers.helpers.TriggerHelper;
import io.harness.pms.contracts.triggers.TriggerPayload;
import io.harness.yaml.utils.JsonPipelineUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public class TriggerPayloadFunctor implements LateBindingValue {
  private final String payload;
  private final TriggerPayload triggerPayload;

  public TriggerPayloadFunctor(String payload, TriggerPayload triggerPayload) {
    this.payload = payload;
    this.triggerPayload = triggerPayload;
  }

  @Override
  public Object bind() {
    Map<String, Object> jsonObject = TriggerHelper.buildJsonObjectFromAmbiance(triggerPayload);
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
