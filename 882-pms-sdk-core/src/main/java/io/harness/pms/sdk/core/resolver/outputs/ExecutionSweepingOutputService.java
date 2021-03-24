package io.harness.pms.sdk.core.resolver.outputs;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.refobjects.RefObject;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.data.SweepingOutput;
import io.harness.pms.sdk.core.resolver.Resolver;

@OwnedBy(CDC)
public interface ExecutionSweepingOutputService extends Resolver<SweepingOutput> {
  OptionalSweepingOutput resolveOptional(Ambiance ambiance, RefObject refObject);
}
