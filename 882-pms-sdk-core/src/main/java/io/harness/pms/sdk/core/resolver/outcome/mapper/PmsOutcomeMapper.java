package io.harness.pms.sdk.core.resolver.outcome.mapper;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.pms.sdk.core.data.Outcome;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;
import org.bson.Document;

@UtilityClass
public class PmsOutcomeMapper {
  public String convertOutcomeValueToJson(Outcome outcome) {
    return RecastOrchestrationUtils.toJson(outcome);
  }

  public Outcome convertJsonToOutcome(String json) {
    return json == null ? null : RecastOrchestrationUtils.fromJson(json, Outcome.class);
  }

  public List<Outcome> convertJsonToOutcome(List<String> outcomesAsJsonList) {
    if (isEmpty(outcomesAsJsonList)) {
      return Collections.emptyList();
    }
    List<Outcome> outcomes = new ArrayList<>();
    for (String jsonOutcome : outcomesAsJsonList) {
      outcomes.add(RecastOrchestrationUtils.fromJson(jsonOutcome, Outcome.class));
    }
    return outcomes;
  }

  public Map<String, Document> convertJsonToDocument(Map<String, String> outcomeAsJsonList) {
    if (isEmpty(outcomeAsJsonList)) {
      return Collections.emptyMap();
    }
    Map<String, Document> outcomes = new LinkedHashMap<>();
    for (Map.Entry<String, String> entry : outcomeAsJsonList.entrySet()) {
      outcomes.put(entry.getKey(),
          entry.getValue() == null ? null : new Document(RecastOrchestrationUtils.fromJson(entry.getValue())));
    }
    return outcomes;
  }

  public List<Outcome> convertFromDocumentToOutcome(List<Map<String, Object>> outcomeMaps) {
    if (isEmpty(outcomeMaps)) {
      return Collections.emptyList();
    }
    List<Outcome> outcomes = new ArrayList<>();
    for (Map<String, Object> map : outcomeMaps) {
      outcomes.add(RecastOrchestrationUtils.fromMap(map, Outcome.class));
    }
    return outcomes;
  }

  public List<Map<String, Object>> convertOutcomesToDocumentList(List<Outcome> outcomes) {
    if (isEmpty(outcomes)) {
      return Collections.emptyList();
    }
    List<Map<String, Object>> outcomeMaps = new ArrayList<>();
    for (Outcome outcome : outcomes) {
      outcomeMaps.add(RecastOrchestrationUtils.toMap(outcome));
    }
    return outcomeMaps;
  }
}
