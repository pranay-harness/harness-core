package io.harness.pipeline.plan.scratch.lib.io;

import io.harness.state.io.StepParameters;

import java.util.HashMap;

public class MapStepParameters extends HashMap<String, Object> implements StepParameters {
  public MapStepParameters() {}

  public MapStepParameters(String key, Object value) {
    super();
    put(key, value);
  }
}
