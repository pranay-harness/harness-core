package io.harness.pms.sdk.core.interrupt;

import static io.harness.pms.events.PmsEventFrameworkConstants.INTERRUPT_CONSUMER;

import io.harness.pms.utils.PmsManagedService;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class InterruptRedisConsumerService extends PmsManagedService {
  private ExecutorService interruptConsumerExecutorService;
  @Inject private InterruptEventRedisConsumer redisConsumer;

  @Override
  protected void startUp() throws Exception {
    interruptConsumerExecutorService =
        Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat(INTERRUPT_CONSUMER).build());
    interruptConsumerExecutorService.execute(redisConsumer);
  }

  @Override
  protected void shutDown() throws Exception {
    interruptConsumerExecutorService.shutdown();
    interruptConsumerExecutorService.awaitTermination(Duration.ofSeconds(30).getSeconds(), TimeUnit.SECONDS);
  }
}
