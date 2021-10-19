package io.harness;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.plugin.compatible.PluginCompatibleStep;
import io.harness.beans.steps.stepinfo.PluginStepInfo;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.beans.steps.stepinfo.RunTestsStepInfo;
import io.harness.ci.config.CIExecutionServiceConfig;
import io.harness.ci.integrationstage.InitializeStepInfoBuilder;
import io.harness.ci.integrationstage.K8InitializeStepInfoBuilder;
import io.harness.ci.serializer.PluginCompatibleStepSerializer;
import io.harness.ci.serializer.PluginStepProtobufSerializer;
import io.harness.ci.serializer.ProtobufStepSerializer;
import io.harness.ci.serializer.RunStepProtobufSerializer;
import io.harness.ci.serializer.RunTestsStepProtobufSerializer;
import io.harness.engine.expressions.AmbianceExpressionEvaluatorProvider;
import io.harness.pms.listener.NgOrchestrationNotifyEventListener;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.TypeLiteral;
import java.util.Optional;

@OwnedBy(HarnessTeam.CI)
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
    install(OrchestrationStepsModule.getInstance(null));
    install(OrchestrationModule.getInstance(OrchestrationModuleConfig.builder()
                                                .serviceName("CI")
                                                .withPMS(Optional.ofNullable(withPMS).orElse(false))
                                                .expressionEvaluatorProvider(new AmbianceExpressionEvaluatorProvider())
                                                .publisherName(NgOrchestrationNotifyEventListener.NG_ORCHESTRATION)
                                                .build()));
    install(NGPipelineCommonsModule.getInstance());
    this.bind(CIExecutionServiceConfig.class).toInstance(this.ciExecutionServiceConfig);
    bind(InitializeStepInfoBuilder.class).to(K8InitializeStepInfoBuilder.class);
    bind(new TypeLiteral<ProtobufStepSerializer<RunStepInfo>>() {}).toInstance(new RunStepProtobufSerializer());
    bind(new TypeLiteral<ProtobufStepSerializer<PluginStepInfo>>() {}).toInstance(new PluginStepProtobufSerializer());
    bind(new TypeLiteral<ProtobufStepSerializer<RunTestsStepInfo>>() {
    }).toInstance(new RunTestsStepProtobufSerializer());
    bind(new TypeLiteral<ProtobufStepSerializer<PluginCompatibleStep>>() {
    }).toInstance(new PluginCompatibleStepSerializer());
  }
}
