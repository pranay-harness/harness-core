package io.harness;

import static io.harness.waiter.OrchestrationNotifyEventListener.ORCHESTRATION;

import io.harness.engine.expressions.ExpressionEvaluatorProvider;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class OrchestrationModuleConfig {
  @NonNull ExpressionEvaluatorProvider expressionEvaluatorProvider;
  @Builder.Default int corePoolSize = 1;
  @Builder.Default int maxPoolSize = 5;
  @Builder.Default long idleTimeInSecs = 10;
  @Builder.Default String publisherName = ORCHESTRATION;
}
