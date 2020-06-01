package io.harness.state.io;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.Outcome;
import io.harness.exception.FailureType;
import io.harness.execution.status.Status;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

@OwnedBy(CDC)
@Value
@Builder(toBuilder = true)
@Redesign
public class StepResponse {
  @NonNull Status status;
  @Singular Collection<StepOutcome> stepOutcomes;
  FailureInfo failureInfo;

  @Value
  @Builder
  public static class FailureInfo {
    String errorMessage;
    @Builder.Default EnumSet<FailureType> failureTypes = EnumSet.noneOf(FailureType.class);
  }

  @Value
  @Builder
  public static class StepOutcome {
    String group;
    @NonNull String name;
    Outcome outcome;
  }

  public Map<String, Outcome> outcomeMap() {
    Map<String, Outcome> stringOutcomeMap = new HashMap<>();
    if (isEmpty(stepOutcomes)) {
      return stringOutcomeMap;
    }
    for (StepOutcome stepOutcome : stepOutcomes) {
      stringOutcomeMap.put(stepOutcome.getName(), stepOutcome.getOutcome());
    }
    return stringOutcomeMap;
  }
}
