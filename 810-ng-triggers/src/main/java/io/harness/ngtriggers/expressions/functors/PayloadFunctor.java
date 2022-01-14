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
