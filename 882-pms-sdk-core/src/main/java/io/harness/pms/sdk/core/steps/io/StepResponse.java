package io.harness.pms.sdk.core.steps.io;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.UnitProgress;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.sdk.core.data.Outcome;

import java.util.Collection;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.Wither;

@OwnedBy(CDC)
@Value
@Builder(toBuilder = true)
public class StepResponse {
  @NonNull Status status;
  @Wither @Singular Collection<StepOutcome> stepOutcomes;

  FailureInfo failureInfo;
  List<UnitProgress> unitProgressList;

  @Value
  @Builder
  public static class StepOutcome {
    String group;
    @NonNull String name;
    Outcome outcome;
  }
}
