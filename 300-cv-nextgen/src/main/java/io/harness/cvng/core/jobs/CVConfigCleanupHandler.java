/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.jobs;

import io.harness.cvng.core.entities.DeletedCVConfig;
import io.harness.cvng.core.services.api.DeletedCVConfigService;
import io.harness.mongo.iterator.MongoPersistenceIterator;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class CVConfigCleanupHandler implements MongoPersistenceIterator.Handler<DeletedCVConfig> {
  @Inject private DeletedCVConfigService deletedCVConfigService;

  @Override
  public void handle(DeletedCVConfig entity) {
    log.info("Triggering cleanup for CVConfig {}", entity.getCvConfig().getUuid());
    deletedCVConfigService.triggerCleanup(entity);
    log.info("Cleanup complete for CVConfig {}", entity.getCvConfig().getUuid());
  }
}
