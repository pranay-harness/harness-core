package io.harness.pms.sdk.core.resolver.outcome.mapper;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PIPELINE)
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
}
