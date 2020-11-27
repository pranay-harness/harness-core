package io.harness.engine.outcomes;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.Outcome;
import io.harness.resolvers.Resolver;

import java.util.List;
import lombok.NonNull;

@OwnedBy(CDC)
public interface OutcomeService extends Resolver<Outcome> {
  List<Outcome> findAllByRuntimeId(String planExecutionId, String runtimeId);

  List<Outcome> fetchOutcomes(List<String> outcomeInstanceIds);

  Outcome fetchOutcome(@NonNull String outcomeInstanceId);
}
