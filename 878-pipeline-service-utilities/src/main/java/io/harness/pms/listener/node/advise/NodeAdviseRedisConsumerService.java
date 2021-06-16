package io.harness.pms.listener.node.advise;

import static io.harness.pms.listener.PmsUtilityConsumerConstants.PT_NODE_ADVISE_CONSUMER;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import io.dropwizard.lifecycle.Managed;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@OwnedBy(HarnessTeam.PIPELINE)
public class NodeAdviseRedisConsumerService implements Managed {
  private ExecutorService executorService;
  @Inject private NodeAdviseEventRedisConsumer nodeAdviseEventRedisConsumer;

  @Override
  public void start() throws Exception {
    executorService =
        Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat(PT_NODE_ADVISE_CONSUMER).build());
    executorService.execute(nodeAdviseEventRedisConsumer);
  }

  @Override
  public void stop() throws Exception {
    executorService.shutdown();
    executorService.awaitTermination(Duration.ofSeconds(10).getSeconds(), TimeUnit.SECONDS);
  }
}
