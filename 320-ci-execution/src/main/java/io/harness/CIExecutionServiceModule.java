package io.harness;

import io.harness.ci.config.CIExecutionServiceConfig;
import io.harness.core.ci.services.CIBuildService;
import io.harness.core.ci.services.CIBuildServiceImpl;
import io.harness.engine.expressions.AmbianceExpressionEvaluatorProvider;
import io.harness.executionplan.ExecutionPlanModule;
import io.harness.impl.CIPipelineExecutionService;
import io.harness.impl.CIPipelineExecutionServiceImpl;
import io.harness.waiter.OrchestrationNotifyEventListener;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class CIExecutionServiceModule extends AbstractModule {
  private CIExecutionServiceConfig ciExecutionServiceConfig;

  @Inject
  public CIExecutionServiceModule(CIExecutionServiceConfig ciExecutionServiceConfig) {
    this.ciExecutionServiceConfig = ciExecutionServiceConfig;
  }

  @Override
  protected void configure() {
    install(CIBeansModule.getInstance());
    install(OrchestrationStepsModule.getInstance());
    install(OrchestrationVisualizationModule.getInstance());
    install(ExecutionPlanModule.getInstance());

    install(NGPipelineCommonsModule.getInstance());
    bind(CIBuildService.class).to(CIBuildServiceImpl.class);
    this.bind(CIExecutionServiceConfig.class).toInstance(this.ciExecutionServiceConfig);
    bind(CIPipelineExecutionService.class).to(CIPipelineExecutionServiceImpl.class);
  }

  @Provides
  @Singleton
  public OrchestrationModuleConfig orchestrationModuleConfig() {
    return OrchestrationModuleConfig.builder()
        .serviceName("CI")
        .expressionEvaluatorProvider(new AmbianceExpressionEvaluatorProvider())
        .publisherName(OrchestrationNotifyEventListener.ORCHESTRATION)
        .maxPoolSize(10)
        .build();
  }
}
