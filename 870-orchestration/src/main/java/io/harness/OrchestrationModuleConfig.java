package io.harness;

import static io.harness.waiter.PmsNotifyEventListener.PMS_ORCHESTRATION;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.expressions.ExpressionEvaluatorProvider;
import io.harness.eventsframework.EventsFrameworkConfiguration;
import io.harness.redis.RedisConfig;

import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
public class OrchestrationModuleConfig {
  @NonNull String serviceName;
  @NonNull ExpressionEvaluatorProvider expressionEvaluatorProvider;
  @Default int corePoolSize = 1;
  @Default int maxPoolSize = 5;
  @Default long idleTimeInSecs = 10;
  @Default String publisherName = PMS_ORCHESTRATION;
  @Default
  EventsFrameworkConfiguration eventsFrameworkConfiguration =
      EventsFrameworkConfiguration.builder()
          .redisConfig(RedisConfig.builder().redisUrl("dummyRedisUrl").build())
          .build();
  boolean withPMS;
  boolean isPipelineService;
  boolean useFeatureFlagService;
  @Nullable io.harness.remote.client.ServiceHttpClientConfig accountServiceHttpClientConfig;
  @Nullable String accountServiceSecret;
  @Nullable String accountClientId;
  @Default
  OrchestrationRedisEventsConfig orchestrationRedisEventsConfig = OrchestrationRedisEventsConfig.builder().build();
}
