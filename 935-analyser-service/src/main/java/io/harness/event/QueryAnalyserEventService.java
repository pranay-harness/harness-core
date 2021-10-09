package io.harness.event;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import io.dropwizard.lifecycle.Managed;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@OwnedBy(HarnessTeam.PIPELINE)
public class QueryAnalyserEventService implements Managed {
  private ExecutorService queryAnalysisMessageConsumerExecutorService;
  @Inject private QueryAnalysisMessageConsumer queryAnalysisMessageConsumer;

  @Override
  public void start() {
    queryAnalysisMessageConsumerExecutorService = Executors.newFixedThreadPool(2);
    queryAnalysisMessageConsumerExecutorService.execute(queryAnalysisMessageConsumer);
  }

  @Override
  public void stop() throws Exception {
    queryAnalysisMessageConsumerExecutorService.shutdown();
    queryAnalysisMessageConsumerExecutorService.awaitTermination(Duration.ofSeconds(10).getSeconds(), TimeUnit.SECONDS);
  }
}
