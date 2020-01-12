package software.wings.search.framework;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;

import io.dropwizard.lifecycle.Managed;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.FeatureName;
import software.wings.service.intfc.FeatureFlagService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
  private final ExecutorService executorService =
      Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("search-main-thread").build());
  private Future elasticsearchSyncJobFuture;

  @Override
  public void start() {
    if (featureFlagService.isGlobalEnabled(FeatureName.SEARCH)) {
      elasticsearchSyncJobFuture = executorService.submit(elasticSearchSyncJob);
    }
  }

  @Override
  public void stop() {
    if (featureFlagService.isGlobalEnabled(FeatureName.SEARCH)) {
      elasticsearchSyncJobFuture.cancel(true);
      executorService.shutdownNow();
    }
  }
}
