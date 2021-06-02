package io.harness.pms.listener.orchestrationevent;

import com.google.inject.Inject;
import io.dropwizard.lifecycle.Managed;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class OrchestrationEventEventConsumerService implements Managed {
  private ExecutorService orchestrationEventConsumerExecutorService;
  @Inject private OrchestrationEventRedisConsumer sdkOrchestrationEventRedisConsumer;

  @Override
  public void start() {
    orchestrationEventConsumerExecutorService = Executors.newFixedThreadPool(2);
    orchestrationEventConsumerExecutorService.execute(sdkOrchestrationEventRedisConsumer);
  }

  @Override
  public void stop() throws Exception {
    orchestrationEventConsumerExecutorService.shutdown();
    orchestrationEventConsumerExecutorService.awaitTermination(Duration.ofSeconds(10).getSeconds(), TimeUnit.SECONDS);
  }
}
