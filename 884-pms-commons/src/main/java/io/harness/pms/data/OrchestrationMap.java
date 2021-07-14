package io.harness.pms.data;

import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class OrchestrationMap extends LinkedHashMap<String, Object> implements Map<String, Object> {
  public OrchestrationMap() {}

  private OrchestrationMap(Map<String, Object> map) {
    super(map);
  }

  public static OrchestrationMap parse(String json) {
    if (json == null) {
      return null;
    }
    return new OrchestrationMap(RecastOrchestrationUtils.fromJson(json));
  }

  public static OrchestrationMap parse(Map<String, Object> map) {
    if (map == null) {
      return null;
    }

    return new OrchestrationMap(map);
  }

  public String toJson() {
    return RecastOrchestrationUtils.toJson(this);
  }
}
