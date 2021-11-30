package io.harness.cvng.core.jobs;

import io.harness.cvng.core.services.api.DataCollectionTaskService;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelIndicator;
import io.harness.mongo.iterator.MongoPersistenceIterator;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SLIDataCollectionTaskCreateNextTaskHandler
    implements MongoPersistenceIterator.Handler<ServiceLevelIndicator> {
  @Inject private DataCollectionTaskService dataCollectionTaskService;

  @Override
  public void handle(ServiceLevelIndicator entity) {
    dataCollectionTaskService.handleCreateNextSLITask(entity);
  }
}
