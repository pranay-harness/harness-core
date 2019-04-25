package io.harness.testframework.graphql;

import lombok.Builder;
import lombok.Value;

import java.util.LinkedHashMap;

@Value
@Builder
public class QLTestObject {
  private LinkedHashMap map;

  public Object get(String path) {
    return map.get(path);
  }

  public int size() {
    return map.size();
  }

  public QLTestObject sub(String path) {
    return QLTestObject.builder().map((LinkedHashMap) map.get(path)).build();
  }
}
