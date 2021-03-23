package io.harness.accesscontrol.migrations.events;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import io.dropwizard.lifecycle.Managed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FeatureFlagEventListenerService implements Managed {
  private final FeatureFlagEventListener featureFlagEventListener;
  private final ExecutorService executorService;
  private Future<?> eventListenerFuture;

  @Inject
  public FeatureFlagEventListenerService(FeatureFlagEventListener featureFlagEventListener) {
    this.featureFlagEventListener = featureFlagEventListener;
    executorService = Executors.newSingleThreadExecutor(
        new ThreadFactoryBuilder().setNameFormat("feature-flag-event-listener-main-thread").build());
  }

  @Override
  public void start() throws Exception {
    eventListenerFuture = executorService.submit(featureFlagEventListener);
  }

  @Override
  public void stop() throws Exception {
    eventListenerFuture.cancel(true);
    executorService.shutdownNow();
  }
}
