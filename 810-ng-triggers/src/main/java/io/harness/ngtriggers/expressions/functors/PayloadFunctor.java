/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ngtriggers.expressions.functors;

import io.harness.exception.InvalidRequestException;
import io.harness.expression.LateBindingValue;
import io.harness.yaml.utils.JsonPipelineUtils;

import java.io.IOException;
import java.util.HashMap;

public class PayloadFunctor implements LateBindingValue {
  private String payload;

  public PayloadFunctor(String payload) {
    this.payload = payload;
  }

  @Override
  public Object bind() {
    try {
      return JsonPipelineUtils.read(payload, HashMap.class);
    } catch (IOException e) {
      throw new InvalidRequestException("Event payload could not be converted to a hashmap");
    }
  }
}
