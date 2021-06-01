package io.harness.execution.consumers;

import io.harness.OrchestrationEventsFrameworkConstants;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import io.dropwizard.lifecycle.Managed;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SdkResponseEventRedisConsumerService implements Managed {
  private ExecutorService consumerExecutorService;
  @Inject private SdkResponseEventRedisConsumer redisConsumer;

  @Override
  public void start() throws Exception {
    consumerExecutorService = Executors.newFixedThreadPool(2,
        new ThreadFactoryBuilder()
            .setNameFormat(OrchestrationEventsFrameworkConstants.SDK_RESPONSE_EVENT_CONSUMER + "-%d")
            .build());
    consumerExecutorService.execute(redisConsumer);
  }

  @Override
  public void stop() throws Exception {
    consumerExecutorService.shutdown();
    consumerExecutorService.awaitTermination(Duration.ofSeconds(10).getSeconds(), TimeUnit.SECONDS);
  }
}
