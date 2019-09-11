package software.wings.search.framework;

import com.google.inject.Inject;

import io.dropwizard.lifecycle.Managed;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.FeatureName;
import software.wings.service.intfc.FeatureFlagService;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

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
  @Inject private ExecutorService executorService;
  private CompletableFuture<Void> elasticsearchSyncJobFuture;

  public void start() {
    if (featureFlagService.isGlobalEnabled(FeatureName.SEARCH)) {
      elasticsearchSyncJobFuture =
          CompletableFuture.runAsync(elasticSearchSyncJob, executorService).thenRun(() -> { this.start(); });
    }
  }

  public void stop() {
    if (featureFlagService.isGlobalEnabled(FeatureName.SEARCH)) {
      elasticsearchSyncJobFuture.cancel(true);
    }
  }
}
