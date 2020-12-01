package io.harness.executionplan.stepsdependency.resolvers;

import io.harness.executionplan.stepsdependency.StepDependencyResolverContext;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.state.io.ResolvedRefInput;
import io.harness.state.io.StepInputPackage;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StepDependencyResolverContextImpl implements StepDependencyResolverContext {
  StepInputPackage stepInputPackage;
  StepParameters stepParameters;
  Ambiance ambiance;
  Map<String, List<ResolvedRefInput>> refKeyToInputParamsMap;
}
