package io.harness.pms.listener.interrupts;

import static io.harness.pms.listener.PmsUtilityConsumerConstants.PT_INTERRUPT_CONSUMER;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import io.dropwizard.lifecycle.Managed;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class InterruptRedisConsumerService implements Managed {
  private ExecutorService interruptConsumerExecutorService;
  @Inject private InterruptEventRedisConsumer interruptEventRedisConsumer;

  @Override
  public void start() throws Exception {
    interruptConsumerExecutorService =
        Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat(PT_INTERRUPT_CONSUMER).build());
    interruptConsumerExecutorService.execute(interruptEventRedisConsumer);
  }

  @Override
  public void stop() throws Exception {
    interruptConsumerExecutorService.shutdown();
    interruptConsumerExecutorService.awaitTermination(Duration.ofSeconds(10).getSeconds(), TimeUnit.SECONDS);
  }
}
