package io.harness.pms.sdk.core.resolver.outcome.mapper;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.pms.sdk.core.data.Outcome;
import io.harness.pms.serializer.persistence.DocumentOrchestrationUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.bson.Document;

@UtilityClass
public class PmsOutcomeMapper {
  public String convertOutcomeValueToJson(Outcome outcome) {
    return DocumentOrchestrationUtils.convertToDocumentJson(outcome);
  }

  public Outcome convertJsonToOutcome(String json) {
    return json == null ? null : DocumentOrchestrationUtils.convertFromDocumentJson(json);
  }

  public List<Outcome> convertJsonToOutcome(List<String> outcomesAsJsonList) {
    if (isEmpty(outcomesAsJsonList)) {
      return Collections.emptyList();
    }
    List<Outcome> outcomes = new ArrayList<>();
    for (String jsonOutcome : outcomesAsJsonList) {
      outcomes.add(DocumentOrchestrationUtils.convertFromDocumentJson(jsonOutcome));
    }
    return outcomes;
  }

  public List<Document> convertJsonToDocument(List<String> outcomeAsJsonList) {
    if (isEmpty(outcomeAsJsonList)) {
      return Collections.emptyList();
    }
    List<Document> outcomes = new ArrayList<>();
    for (String jsonOutcome : outcomeAsJsonList) {
      outcomes.add(DocumentOrchestrationUtils.convertToDocumentFromJson(jsonOutcome));
    }
    return outcomes;
  }

  public List<Outcome> convertFromDocumentToOutcome(List<Document> outcomeDocuments) {
    if (isEmpty(outcomeDocuments)) {
      return Collections.emptyList();
    }
    List<Outcome> outcomes = new ArrayList<>();
    for (Document document : outcomeDocuments) {
      outcomes.add(DocumentOrchestrationUtils.convertFromDocument(document));
    }
    return outcomes;
  }

  public List<Document> convertOutcomesToDocumentList(List<Outcome> outcomes) {
    if (isEmpty(outcomes)) {
      return Collections.emptyList();
    }
    List<Document> outcomeDocuments = new ArrayList<>();
    for (Outcome outcome : outcomes) {
      outcomeDocuments.add(DocumentOrchestrationUtils.convertToDocument(outcome));
    }
    return outcomeDocuments;
  }
}
