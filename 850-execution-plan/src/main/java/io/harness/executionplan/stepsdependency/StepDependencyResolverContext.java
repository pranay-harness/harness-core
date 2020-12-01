package io.harness.executionplan.stepsdependency;

import io.harness.executionplan.stepsdependency.resolvers.StepDependencyResolverContextImpl;
import io.harness.executionplan.stepsdependency.resolvers.StepDependencyResolverContextImpl.StepDependencyResolverContextImplBuilder;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.state.io.ResolvedRefInput;
import io.harness.state.io.StepInputPackage;

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
