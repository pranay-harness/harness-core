package io.harness.cvng.core.jobs;

import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.services.api.DataCollectionTaskService;
import io.harness.mongo.iterator.MongoPersistenceIterator;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DataCollectionTaskCreateNextTaskHandler implements MongoPersistenceIterator.Handler<CVConfig> {
  @Inject private DataCollectionTaskService dataCollectionTaskService;

  @Override
  public void handle(CVConfig entity) {
    dataCollectionTaskService.handleCreateNextTask(entity);
  }
}
