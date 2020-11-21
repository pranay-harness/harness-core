package io.harness.cvng.core.jobs;

import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.services.api.DataCollectionTaskService;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class CVConfigDataCollectionHandler implements Handler<CVConfig> {
  @Inject private DataCollectionTaskService dataCollectionTaskService;
  @Override
  public void handle(CVConfig entity) {
    log.info("Enqueuing cvConfig {}", entity.getUuid());
    dataCollectionTaskService.enqueueFirstTask(entity);
    log.info("Done enqueuing cvConfig {}", entity.getUuid());
  }
}
