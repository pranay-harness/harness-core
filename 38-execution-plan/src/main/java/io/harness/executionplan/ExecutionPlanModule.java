package io.harness.executionplan;

import static com.google.inject.Scopes.SINGLETON;

import io.harness.executionplan.core.ExecutionPlanCreatorRegistry;
import io.harness.executionplan.core.impl.ExecutionPlanCreatorRegistryImpl;
import io.harness.executionplan.plancreator.GenericStepPlanCreator;
import io.harness.executionplan.plancreator.ParallelStagePlanCreator;
import io.harness.executionplan.plancreator.ParallelStepPlanCreator;
import io.harness.executionplan.plancreator.StagesPlanCreator;
import io.harness.executionplan.plancreator.StepGroupPlanCreator;
import io.harness.executionplan.service.ExecutionPlanCreatorHelper;
import io.harness.executionplan.service.ExecutionPlanCreatorService;
import io.harness.executionplan.service.impl.ExecutionPlanCreatorServiceImpl;
import io.harness.executionplan.stepsdependency.StepDependencyResolver;
import io.harness.executionplan.stepsdependency.StepDependencyService;
import io.harness.executionplan.stepsdependency.resolvers.ExpressionStepDependencyResolver;
import io.harness.executionplan.stepsdependency.resolvers.RefObjectStepDependencyResolver;
import io.harness.executionplan.stepsdependency.service.StepDependencyServiceImpl;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class ExecutionPlanModule extends AbstractModule {
  private static volatile ExecutionPlanModule instance;

  public static ExecutionPlanModule getInstance() {
    if (instance == null) {
      instance = new ExecutionPlanModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(ExecutionPlanCreatorService.class).to(ExecutionPlanCreatorServiceImpl.class).in(SINGLETON);
    bind(ExecutionPlanCreatorRegistry.class).to(ExecutionPlanCreatorRegistryImpl.class).in(SINGLETON);
    bind(ExecutionPlanCreatorHelper.class).in(SINGLETON);
    bind(ParallelStepPlanCreator.class).in(SINGLETON);
    bind(StagesPlanCreator.class).in(SINGLETON);
    bind(StepGroupPlanCreator.class).in(SINGLETON);
    bind(ParallelStagePlanCreator.class).in(SINGLETON);
    bind(GenericStepPlanCreator.class).in(SINGLETON);

    bind(StepDependencyResolver.class)
        .annotatedWith(Names.named("RefObjectResolver"))
        .to(RefObjectStepDependencyResolver.class)
        .in(SINGLETON);
    bind(StepDependencyResolver.class)
        .annotatedWith(Names.named("ExpressionResolver"))
        .to(ExpressionStepDependencyResolver.class)
        .in(SINGLETON);
    bind(StepDependencyService.class).to(StepDependencyServiceImpl.class).in(SINGLETON);
  }
}
