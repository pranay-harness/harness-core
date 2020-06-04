package io.harness.engine.services;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.Outcome;
import io.harness.resolvers.Resolver;

import java.util.List;

@OwnedBy(CDC)
public interface OutcomeService extends Resolver<Outcome> {
  List<Outcome> findAllByRuntimeId(String planExecutionId, String runtimeId);
}
