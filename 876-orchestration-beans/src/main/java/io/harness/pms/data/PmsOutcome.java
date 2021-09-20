package io.harness.pms.data;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public class PmsOutcome extends OrchestrationMap {
  public PmsOutcome(Map<String, Object> map) {
    super(map);
  }

  public static PmsOutcome parse(String json) {
    if (json == null) {
      return null;
    }
    return new PmsOutcome(RecastOrchestrationUtils.fromJson(json));
  }

  public static PmsOutcome parse(Map<String, Object> map) {
    if (map == null) {
      return null;
    }

    return new PmsOutcome(map);
  }

  public static Map<String, PmsOutcome> convertJsonToOrchestrationMap(Map<String, String> outcomeAsJsonList) {
    if (EmptyPredicate.isEmpty(outcomeAsJsonList)) {
      return Collections.emptyMap();
    }
    Map<String, PmsOutcome> outcomes = new LinkedHashMap<>();
    for (Map.Entry<String, String> entry : outcomeAsJsonList.entrySet()) {
      outcomes.put(entry.getKey(), entry.getValue() == null ? null : PmsOutcome.parse(entry.getValue()));
    }
    return outcomes;
  }
}
