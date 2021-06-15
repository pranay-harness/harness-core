package io.harness.pms.listener.facilitators;

import static io.harness.pms.listener.PmsUtilityConsumerConstants.PT_FACILITATOR_CONSUMER;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import io.dropwizard.lifecycle.Managed;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FacilitatorRedisConsumerService implements Managed {
  private ExecutorService executorService;
  @Inject private FacilitatorEventRedisConsumer facilitatorEventRedisConsumer;

  @Override
  public void start() throws Exception {
    executorService =
        Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat(PT_FACILITATOR_CONSUMER).build());
    executorService.execute(facilitatorEventRedisConsumer);
  }

  @Override
  public void stop() throws Exception {
    executorService.shutdown();
    executorService.awaitTermination(Duration.ofSeconds(10).getSeconds(), TimeUnit.SECONDS);
  }
}
