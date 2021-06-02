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
    Document document = RecastOrchestrationUtils.toDocument(outcome);
    return document == null ? null : document.toJson();
  }

  public Outcome convertJsonToOutcome(String json) {
    return json == null ? null : RecastOrchestrationUtils.fromDocumentJson(json, Outcome.class);
  }

  public List<Outcome> convertJsonToOutcome(List<String> outcomesAsJsonList) {
    if (isEmpty(outcomesAsJsonList)) {
      return Collections.emptyList();
    }
    List<Outcome> outcomes = new ArrayList<>();
    for (String jsonOutcome : outcomesAsJsonList) {
      outcomes.add(RecastOrchestrationUtils.fromDocumentJson(jsonOutcome, Outcome.class));
    }
    return outcomes;
  }

  public Map<String, Document> convertJsonToDocument(Map<String, String> outcomeAsJsonList) {
    if (isEmpty(outcomeAsJsonList)) {
      return Collections.emptyMap();
    }
    Map<String, Document> outcomes = new LinkedHashMap<>();
    for (Map.Entry<String, String> entry : outcomeAsJsonList.entrySet()) {
      outcomes.put(entry.getKey(), RecastOrchestrationUtils.toDocumentFromJson(entry.getValue()));
    }
    return outcomes;
  }

  public List<Outcome> convertFromDocumentToOutcome(List<Document> outcomeDocuments) {
    if (isEmpty(outcomeDocuments)) {
      return Collections.emptyList();
    }
    List<Outcome> outcomes = new ArrayList<>();
    for (Document document : outcomeDocuments) {
      outcomes.add(RecastOrchestrationUtils.fromDocument(document, Outcome.class));
    }
    return outcomes;
  }

  public List<Document> convertOutcomesToDocumentList(List<Outcome> outcomes) {
    if (isEmpty(outcomes)) {
      return Collections.emptyList();
    }
    List<Document> outcomeDocuments = new ArrayList<>();
    for (Outcome outcome : outcomes) {
      outcomeDocuments.add(RecastOrchestrationUtils.toDocument(outcome));
    }
    return outcomeDocuments;
  }
}
