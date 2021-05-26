package io.harness;

import static io.harness.waiter.PmsNotifyEventListener.PMS_ORCHESTRATION;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.expressions.ExpressionEvaluatorProvider;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
public class OrchestrationModuleConfig {
  @NonNull String serviceName;
  @NonNull ExpressionEvaluatorProvider expressionEvaluatorProvider;
  @Builder.Default int corePoolSize = 1;
  @Builder.Default int maxPoolSize = 5;
  @Builder.Default long idleTimeInSecs = 10;
  @Builder.Default String publisherName = PMS_ORCHESTRATION;
  boolean withPMS;
  boolean isPipelineService;
}
