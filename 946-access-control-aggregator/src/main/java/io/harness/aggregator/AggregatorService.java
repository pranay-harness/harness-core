package io.harness.aggregator;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.dropwizard.lifecycle.Managed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Singleton
@OwnedBy(HarnessTeam.PL)
public class AggregatorService implements Managed {
  private final AggregatorJob aggregatorJob;
  private final ExecutorService executorService;

  @Inject
  public AggregatorService(AggregatorJob aggregatorJob) {
    this.aggregatorJob = aggregatorJob;
    this.executorService =
        Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("aggregator-job").build());
  }

  @Override
  public void start() {
    executorService.execute(aggregatorJob);
  }

  @Override
  public void stop() {
    executorService.shutdown();
    try {
      executorService.awaitTermination(1, TimeUnit.MINUTES);
    } catch (InterruptedException interruptedException) {
      Thread.currentThread().interrupt();
    }
  }
}
