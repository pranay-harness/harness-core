package io.harness.pms.sdk.core.execution.events.orchestration;

import io.harness.pms.utils.PmsManagedService;

import com.google.inject.Inject;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SdkOrchestrationEventEventRedisConsumerService extends PmsManagedService {
  private ExecutorService orchestrationEventConsumerExecutorService;
  @Inject private SdkOrchestrationEventRedisConsumer sdkOrchestrationEventRedisConsumer;

  @Override
  protected void startUp() {
    orchestrationEventConsumerExecutorService = Executors.newFixedThreadPool(2);
    orchestrationEventConsumerExecutorService.execute(sdkOrchestrationEventRedisConsumer);
  }

  @Override
  protected void shutDown() throws Exception {
    orchestrationEventConsumerExecutorService.shutdown();
    orchestrationEventConsumerExecutorService.awaitTermination(Duration.ofSeconds(10).getSeconds(), TimeUnit.SECONDS);
  }
}
