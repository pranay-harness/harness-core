package io.harness.executionplan.stepsdependency;

import io.harness.ambiance.Ambiance;
import io.harness.executionplan.stepsdependency.resolvers.StepDependencyResolverContextImpl;
import io.harness.executionplan.stepsdependency.resolvers.StepDependencyResolverContextImpl.StepDependencyResolverContextImplBuilder;
import io.harness.state.io.ResolvedRefInput;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepParameters;

import java.util.List;
import java.util.Map;

/**
 * This class is input for step dependency resolver.
 */
public interface StepDependencyResolverContext {
  /** Get the step parameters inside the workflow engine step. */
  StepParameters getStepParameters();
  /** Inputs given to the workflow engine step. */
  StepInputPackage getStepInputPackage();
  Ambiance getAmbiance();
  Map<String, List<ResolvedRefInput>> getRefKeyToInputParamsMap();

  static StepDependencyResolverContextImplBuilder defaultBuilder() {
    return StepDependencyResolverContextImpl.builder();
  }
}
