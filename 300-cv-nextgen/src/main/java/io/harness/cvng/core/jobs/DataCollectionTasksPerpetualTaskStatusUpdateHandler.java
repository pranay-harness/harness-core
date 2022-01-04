package io.harness.cvng.core.jobs;

import io.harness.cvng.core.entities.DataCollectionTask;
import io.harness.cvng.core.services.api.DataCollectionTaskService;
import io.harness.mongo.iterator.MongoPersistenceIterator;

import com.google.inject.Inject;

public class DataCollectionTasksPerpetualTaskStatusUpdateHandler
    implements MongoPersistenceIterator.Handler<DataCollectionTask> {
  @Inject DataCollectionTaskService dataCollectionTaskService;
  @Override
  public void handle(DataCollectionTask entity) {
    dataCollectionTaskService.updatePerpetualTaskStatus(entity);
  }
}
