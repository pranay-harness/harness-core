package io.harness.pms.sdk.io;

import io.harness.pms.sdk.beans.StepParameters;
import io.harness.serializer.JsonUtils;

import java.util.HashMap;

public class MapStepParameters extends HashMap<String, Object> implements StepParameters {
  public MapStepParameters() {}

  public MapStepParameters(String key, Object value) {
    super();
    put(key, value);
  }

  @Override
  public String toJson() {
    return JsonUtils.asJson(this);
  }
}
