package io.harness.engine.outputs;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SweepingOutput;
import io.harness.resolvers.Resolver;

@OwnedBy(CDC)
public interface ExecutionSweepingOutputService extends Resolver<SweepingOutput> {}
