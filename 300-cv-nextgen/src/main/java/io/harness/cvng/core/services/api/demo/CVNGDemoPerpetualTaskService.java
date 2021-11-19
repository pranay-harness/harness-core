package io.harness.cvng.core.services.api.demo;

import io.harness.cvng.core.entities.demo.CVNGDemoPerpetualTask;

public interface CVNGDemoPerpetualTaskService {
  String createCVNGDemoPerpetualTask(String accountId, String dataCollectionWorkerId);

  void execute(CVNGDemoPerpetualTask cvngDemoPerpetualTask);

  void deletePerpetualTask(String accountId, String perpetualTaskId);
}
