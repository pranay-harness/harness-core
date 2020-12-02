package io.harness.executionplan.stepsdependency.service;

import io.harness.exception.InvalidArgumentsException;
import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.executionplan.stepsdependency.StepDependencyInstructor;
import io.harness.executionplan.stepsdependency.StepDependencyResolver;
import io.harness.executionplan.stepsdependency.StepDependencyResolverContext;
import io.harness.executionplan.stepsdependency.StepDependencyService;
import io.harness.executionplan.stepsdependency.StepDependencySpec;
import io.harness.executionplan.utils.StepDependencyInfoUtils;
import io.harness.pms.sdk.core.plan.PlanNode.PlanNodeBuilder;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.List;
import java.util.Optional;

public class StepDependencyServiceImpl implements StepDependencyService {
  @Inject @Named("RefObjectResolver") private StepDependencyResolver refObjectResolver;
  @Inject @Named("ExpressionResolver") private StepDependencyResolver expressionResolver;

  @Override
  public void attachDependency(
      StepDependencySpec spec, PlanNodeBuilder planNodeBuilder, ExecutionPlanCreationContext context) {
    List<StepDependencyInstructor> instructorsList = StepDependencyInfoUtils.getInstructorsList(context);

    // Select the right providers.
    boolean found = false;
    for (StepDependencyInstructor instructor : instructorsList) {
      if (instructor.supports(spec, context)) {
        found = true;
        instructor.attachDependency(spec, planNodeBuilder, context);
      }
    }
    if (!found) {
      throw new InvalidArgumentsException("No Step Dependency Provider found.");
    }
  }

  @Override
  public void registerStepDependencyInstructor(
      StepDependencyInstructor instructor, ExecutionPlanCreationContext context) {
    StepDependencyInfoUtils.addInstructor(instructor, context);
  }

  @Override
  public <T> Optional<T> resolve(StepDependencySpec spec, StepDependencyResolverContext resolverContext) {
    Optional<T> refResolve = refObjectResolver.resolve(spec, resolverContext);
    if (refResolve.isPresent()) {
      return refResolve;
    }
    return expressionResolver.resolve(spec, resolverContext);
  }
}
