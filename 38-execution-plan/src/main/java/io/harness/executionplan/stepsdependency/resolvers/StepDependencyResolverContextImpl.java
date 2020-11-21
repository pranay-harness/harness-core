package io.harness.executionplan.stepsdependency.resolvers;

import io.harness.ambiance.Ambiance;
import io.harness.executionplan.stepsdependency.StepDependencyResolverContext;
import io.harness.state.io.ResolvedRefInput;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepParameters;

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
