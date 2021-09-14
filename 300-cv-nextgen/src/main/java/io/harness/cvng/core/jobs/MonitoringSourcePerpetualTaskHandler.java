/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.jobs;

import io.harness.cvng.core.entities.MonitoringSourcePerpetualTask;
import io.harness.cvng.core.services.api.MonitoringSourcePerpetualTaskService;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class MonitoringSourcePerpetualTaskHandler implements Handler<MonitoringSourcePerpetualTask> {
  @Inject private MonitoringSourcePerpetualTaskService monitoringSourcePerpetualTaskService;
  @Override
  public void handle(MonitoringSourcePerpetualTask entity) {
    log.info("Enqueuing monitoring source {}", entity.getUuid());
    monitoringSourcePerpetualTaskService.createPerpetualTask(entity);
    log.info("Done enqueuing monitoring source {}", entity.getUuid());
  }
}
