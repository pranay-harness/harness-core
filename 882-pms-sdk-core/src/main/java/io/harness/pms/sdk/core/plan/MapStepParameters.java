package io.harness.pms.sdk.core.plan;

import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.serializer.json.JsonOrchestrationUtils;

import java.util.HashMap;

public class MapStepParameters extends HashMap<String, Object> implements StepParameters {
  public MapStepParameters() {}

  public MapStepParameters(String key, Object value) {
    super();
    put(key, value);
  }

  @Override
  public String toJson() {
    return JsonOrchestrationUtils.asJson(this);
  }
}
