package io.harness.pms.sdk.io;

import java.util.HashMap;

public class MapStepParameters extends HashMap<String, Object> {
  public MapStepParameters() {}

  public MapStepParameters(String key, Object value) {
    super();
    put(key, value);
  }
}
