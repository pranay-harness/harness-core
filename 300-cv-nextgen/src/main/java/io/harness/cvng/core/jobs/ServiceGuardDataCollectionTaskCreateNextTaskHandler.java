package io.harness.cvng.core.jobs;

import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.DataCollectionTask;
import io.harness.cvng.core.services.api.DataCollectionTaskManagementService;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;

@Singleton
public class ServiceGuardDataCollectionTaskCreateNextTaskHandler
    implements DataCollectionTaskCreateNextTaskHandler<CVConfig> {
  @Inject
  private Map<DataCollectionTask.Type, DataCollectionTaskManagementService>
      dataCollectionTaskManagementServiceMapBinder;

  @Override
  public void handle(CVConfig entity) {
    Preconditions.checkArgument(dataCollectionTaskManagementServiceMapBinder.containsKey(DataCollectionTask.Type.SLI));
    dataCollectionTaskManagementServiceMapBinder.get(DataCollectionTask.Type.SLI).handleCreateNextTask(entity);
  }
}
