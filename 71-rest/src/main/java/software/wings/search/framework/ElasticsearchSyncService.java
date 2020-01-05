package software.wings.search.framework;

import com.google.inject.Inject;

import io.dropwizard.lifecycle.Managed;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.FeatureName;
import software.wings.service.intfc.FeatureFlagService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * The service which fires up the job responsible
 * to maintain elasticsearch db in sync with mongodb.
 *
 * @author utkarsh
 */

@Slf4j
public class ElasticsearchSyncService implements Managed {
  @Inject private ElasticsearchSyncJob elasticSearchSyncJob;
  @Inject private FeatureFlagService featureFlagService;
  private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
  private ScheduledFuture elasticsearchSyncJobFuture;

  @Override
  public void start() {
    if (featureFlagService.isGlobalEnabled(FeatureName.SEARCH)) {
      elasticsearchSyncJobFuture =
          scheduledExecutorService.scheduleWithFixedDelay(elasticSearchSyncJob, 0, 1, TimeUnit.SECONDS);
    }
  }

  @Override
  public void stop() {
    if (featureFlagService.isGlobalEnabled(FeatureName.SEARCH)) {
      elasticsearchSyncJobFuture.cancel(true);
      scheduledExecutorService.shutdownNow();
    }
  }
}
