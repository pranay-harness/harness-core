package io.harness;

import com.google.common.collect.ImmutableSet;
import com.google.inject.multibindings.MapBinder;

import io.harness.engine.expressions.AmbianceExpressionEvaluatorProvider;
import io.harness.executionplan.ExecutionPlanModule;
import io.harness.govern.DependencyModule;
import io.harness.impl.CIPipelineExecutionService;
import io.harness.impl.CIPipelineExecutionServiceImpl;
import io.harness.registrars.ExecutionRegistrar;
import io.harness.registries.RegistryModule;
import io.harness.registries.registrar.StepRegistrar;

import java.util.Set;

public class CIExecutionServiceModule extends DependencyModule {
  private static CIExecutionServiceModule instance;
  public static CIExecutionServiceModule getInstance() {
    if (instance == null) {
      instance = new CIExecutionServiceModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(CIPipelineExecutionService.class).to(CIPipelineExecutionServiceImpl.class);
    MapBinder<String, StepRegistrar> stepRegistrarMapBinder =
        MapBinder.newMapBinder(binder(), String.class, StepRegistrar.class);
    stepRegistrarMapBinder.addBinding(ExecutionRegistrar.class.getName()).to(ExecutionRegistrar.class);
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return ImmutableSet.of(RegistryModule.getInstance(), ExecutionPlanModule.getInstance(),
        OrchestrationModule.getInstance(OrchestrationModuleConfig.builder()
                                            .expressionEvaluatorProvider(new AmbianceExpressionEvaluatorProvider())
                                            .build()),
        CIBeansModule.getInstance());
  }
}