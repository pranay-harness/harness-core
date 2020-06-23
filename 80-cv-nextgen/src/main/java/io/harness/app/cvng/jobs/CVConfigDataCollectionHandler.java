package io.harness.app.cvng.jobs;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.app.cvng.api.DataCollectionTaskService;
import io.harness.cvng.core.services.entities.CVConfig;
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
