/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.activity.jobs;

import io.harness.cvng.activity.entities.KubernetesActivitySource;
import io.harness.cvng.activity.source.services.api.KubernetesActivitySourceService;
import io.harness.mongo.iterator.MongoPersistenceIterator;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class K8ActivityCollectionHandler implements MongoPersistenceIterator.Handler<KubernetesActivitySource> {
  @Inject private KubernetesActivitySourceService activityService;
  @Override
  public void handle(KubernetesActivitySource activitySource) {
    log.info("Enqueuing activitySource {}", activitySource.getUuid());
    activityService.enqueueDataCollectionTask(activitySource);
    log.info("Done enqueuing activitySource {}", activitySource.getUuid());
  }
}
