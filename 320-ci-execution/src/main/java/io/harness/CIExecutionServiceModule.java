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
import java.util.Optional;

public class CIExecutionServiceModule extends AbstractModule {
  private CIExecutionServiceConfig ciExecutionServiceConfig;
  private final Boolean withPMS;

  @Inject
  public CIExecutionServiceModule(CIExecutionServiceConfig ciExecutionServiceConfig, Boolean withPMS) {
    this.ciExecutionServiceConfig = ciExecutionServiceConfig;
    this.withPMS = withPMS;
  }

  @Override
  protected void configure() {
    install(CIBeansModule.getInstance());
    install(OrchestrationStepsModule.getInstance());
    install(OrchestrationVisualizationModule.getInstance());
    install(ExecutionPlanModule.getInstance());

    install(
        NGPipelineCommonsModule.getInstance(OrchestrationModuleConfig.builder()
                                                .serviceName("CI")
                                                .withPMS(Optional.ofNullable(withPMS).orElse(false))
                                                .expressionEvaluatorProvider(new AmbianceExpressionEvaluatorProvider())
                                                .publisherName(OrchestrationNotifyEventListener.ORCHESTRATION)
                                                .maxPoolSize(10)
                                                .build()));
    bind(CIBuildService.class).to(CIBuildServiceImpl.class);
    this.bind(CIExecutionServiceConfig.class).toInstance(this.ciExecutionServiceConfig);
    bind(CIPipelineExecutionService.class).to(CIPipelineExecutionServiceImpl.class);
  }
}
