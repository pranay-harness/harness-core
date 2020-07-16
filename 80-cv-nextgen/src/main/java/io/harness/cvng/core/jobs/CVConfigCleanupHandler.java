package io.harness.cvng.core.jobs;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cvng.core.entities.DeletedCVConfig;
import io.harness.cvng.core.services.api.DeletedCVConfigService;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class CVConfigCleanupHandler implements MongoPersistenceIterator.Handler<DeletedCVConfig> {
  @Inject private DeletedCVConfigService deletedCVConfigService;

  @Override
  public void handle(DeletedCVConfig entity) {
    logger.info("Triggering cleanup for CVConfig {}", entity.getCvConfig().getUuid());
    deletedCVConfigService.triggerCleanup(entity);
    logger.info("Cleanup complete for CVConfig {}", entity.getCvConfig().getUuid());
  }
}
