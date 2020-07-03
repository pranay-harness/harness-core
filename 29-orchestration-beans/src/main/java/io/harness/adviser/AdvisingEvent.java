package io.harness.adviser;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.Outcome;
import io.harness.execution.status.Status;
import io.harness.state.io.FailureInfo;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.Map;

@OwnedBy(CDC)
@Value
@Builder
@Redesign
public class AdvisingEvent {
  @NonNull Ambiance ambiance;
  Map<String, Outcome> outcomes;
  AdviserParameters adviserParameters;
  Status status;
  FailureInfo failureInfo;
}
