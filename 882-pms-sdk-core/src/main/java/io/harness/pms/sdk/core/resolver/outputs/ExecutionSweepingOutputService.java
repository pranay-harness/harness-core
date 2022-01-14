package io.harness.pms.sdk.core.resolver.outputs;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.refobjects.RefObject;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.resolver.Resolver;

import java.util.List;

@OwnedBy(CDC)
public interface ExecutionSweepingOutputService extends Resolver<ExecutionSweepingOutput> {
  OptionalSweepingOutput resolveOptional(Ambiance ambiance, RefObject refObject);
  List<OptionalSweepingOutput> listOutputsWithGivenNameAndSetupIds(
      Ambiance ambiance, String name, List<String> nodeIds);
}
