package io.harness.pms.sdk.core.resolver.outcome.mapper;

import static io.harness.data.structure.HasPredicate.hasNone;

import io.harness.pms.sdk.core.data.Outcome;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    if (hasNone(outcomesAsJsonList)) {
      return Collections.emptyList();
    }
    List<Outcome> outcomes = new ArrayList<>();
    for (String jsonOutcome : outcomesAsJsonList) {
      outcomes.add(RecastOrchestrationUtils.fromDocumentJson(jsonOutcome, Outcome.class));
    }
    return outcomes;
  }

  public List<Document> convertJsonToDocument(List<String> outcomeAsJsonList) {
    if (hasNone(outcomeAsJsonList)) {
      return Collections.emptyList();
    }
    List<Document> outcomes = new ArrayList<>();
    for (String jsonOutcome : outcomeAsJsonList) {
      outcomes.add(RecastOrchestrationUtils.toDocumentFromJson(jsonOutcome));
    }
    return outcomes;
  }

  public List<Outcome> convertFromDocumentToOutcome(List<Document> outcomeDocuments) {
    if (hasNone(outcomeDocuments)) {
      return Collections.emptyList();
    }
    List<Outcome> outcomes = new ArrayList<>();
    for (Document document : outcomeDocuments) {
      outcomes.add(RecastOrchestrationUtils.fromDocument(document, Outcome.class));
    }
    return outcomes;
  }

  public List<Document> convertOutcomesToDocumentList(List<Outcome> outcomes) {
    if (hasNone(outcomes)) {
      return Collections.emptyList();
    }
    List<Document> outcomeDocuments = new ArrayList<>();
    for (Outcome outcome : outcomes) {
      outcomeDocuments.add(RecastOrchestrationUtils.toDocument(outcome));
    }
    return outcomeDocuments;
  }
}
