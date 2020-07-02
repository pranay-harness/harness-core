package io.harness.executionplan.stepsdependency;

import java.util.Optional;

public interface StepDependencyResolver {
  <T> Optional<T> resolve(StepDependencySpec spec, StepDependencyResolverContext resolverContext);
}
