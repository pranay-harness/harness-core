package io.harness.pms.listener.node.start;

import static io.harness.pms.listener.PmsUtilityConsumerConstants.PT_NODE_START_CONSUMER;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import io.dropwizard.lifecycle.Managed;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NodeStartRedisConsumerService implements Managed {
  private ExecutorService executorService;
  @Inject private NodeStartEventRedisConsumer nodeStartEventRedisConsumer;

  @Override
  public void start() throws Exception {
    executorService =
        Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat(PT_NODE_START_CONSUMER).build());
    executorService.execute(nodeStartEventRedisConsumer);
  }

  @Override
  public void stop() throws Exception {
    executorService.shutdown();
    executorService.awaitTermination(Duration.ofSeconds(10).getSeconds(), TimeUnit.SECONDS);
  }
}
