package io.harness;

import com.google.common.collect.ImmutableSet;

import io.harness.engine.expressions.AmbianceExpressionEvaluatorProvider;
import io.harness.executionplan.ExecutionPlanModule;
import io.harness.govern.DependencyModule;
import io.harness.impl.CIPipelineExecutionService;
import io.harness.impl.CIPipelineExecutionServiceImpl;
import io.harness.registries.RegistryModule;

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