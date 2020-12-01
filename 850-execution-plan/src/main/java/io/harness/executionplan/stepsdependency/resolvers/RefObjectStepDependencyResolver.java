package io.harness.executionplan.stepsdependency.resolvers;

import io.harness.executionplan.stepsdependency.KeyAware;
import io.harness.executionplan.stepsdependency.StepDependencyResolver;
import io.harness.executionplan.stepsdependency.StepDependencyResolverContext;
import io.harness.executionplan.stepsdependency.StepDependencySpec;
import io.harness.pms.sdk.core.steps.io.ResolvedRefInput;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class RefObjectStepDependencyResolver implements StepDependencyResolver {
  @Override
  public <T> Optional<T> resolve(StepDependencySpec spec, StepDependencyResolverContext resolverContext) {
    Map<String, List<ResolvedRefInput>> refKeyToInputParamsMap = resolverContext.getRefKeyToInputParamsMap();
    if (spec instanceof KeyAware) {
      KeyAware keyAware = (KeyAware) spec;
      // Accessing value from refKey using keyType.
      List<ResolvedRefInput> resolvedRefInputs = refKeyToInputParamsMap.get(keyAware.getKey());
      return resolvedRefInputs.size() == 1
          ? Optional.of((T) resolvedRefInputs.get(0).getTransput())
          : Optional.of((T) resolvedRefInputs.stream().map(ResolvedRefInput::getTransput).collect(Collectors.toList()));
    }
    return Optional.empty();
  }
}
