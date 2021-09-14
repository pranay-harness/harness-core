/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.search.framework;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.ff.FeatureFlagService;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import io.dropwizard.lifecycle.Managed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;

/**
 * The service which fires up the job responsible
 * to maintain elasticsearch db in sync with mongodb.
 *
 * @author utkarsh
 */

@OwnedBy(PL)
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
