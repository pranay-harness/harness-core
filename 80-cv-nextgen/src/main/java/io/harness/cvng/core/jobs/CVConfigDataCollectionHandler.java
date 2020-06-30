package io.harness.cvng.core.jobs;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.services.api.DataCollectionTaskService;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class CVConfigDataCollectionHandler implements MongoPersistenceIterator.Handler<CVConfig> {
  @Inject private DataCollectionTaskService dataCollectionTaskService;
  @Override
  public void handle(CVConfig entity) {
    logger.info("Enqueuing cvConfig {}", entity.getUuid());
    dataCollectionTaskService.enqueueFirstTask(entity);
    logger.info("Done enqueuing cvConfig {}", entity.getUuid());
  }
}
