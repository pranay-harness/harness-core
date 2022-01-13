/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.search.framework;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

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
  private final ExecutorService executorService =
      Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("search-main-thread").build());
  private Future elasticsearchSyncJobFuture;

  @Override
  public void start() {
    elasticsearchSyncJobFuture = executorService.submit(elasticSearchSyncJob);
  }

  @Override
  public void stop() {
    elasticsearchSyncJobFuture.cancel(true);
    executorService.shutdownNow();
  }
}
