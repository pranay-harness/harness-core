package io.harness.pms.data;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * In order to check database value for this class, use {@link OrchestrationMapTest#shouldTestDeserialization()}
 */
@OwnedBy(HarnessTeam.PIPELINE)
public class OrchestrationMap extends LinkedHashMap<String, Object> implements Map<String, Object> {
  public OrchestrationMap() {}

  protected OrchestrationMap(Map<String, Object> map) {
    super(map);
  }

  public static OrchestrationMap parse(String json) {
    if (EmptyPredicate.isEmpty(json)) {
      return null;
    }
    return new OrchestrationMap(RecastOrchestrationUtils.fromJson(json));
  }

  public static OrchestrationMap parse(Map<String, Object> map) {
    if (EmptyPredicate.isEmpty(map)) {
      return null;
    }

    return new OrchestrationMap(map);
  }

  public String toJson() {
    return RecastOrchestrationUtils.toJson(this);
  }
}
