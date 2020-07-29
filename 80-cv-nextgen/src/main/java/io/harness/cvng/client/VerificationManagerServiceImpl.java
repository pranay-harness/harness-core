package io.harness.cvng.client;

import com.google.inject.Inject;

import io.harness.cvng.core.entities.CVConfig.CVConfigKeys;
import io.harness.cvng.core.entities.DataCollectionTask.DataCollectionTaskKeys;

import java.util.HashMap;
import java.util.Map;

public class VerificationManagerServiceImpl implements VerificationManagerService {
  @Inject private VerificationManagerClient verificationManagerClient;
  @Inject private RequestExecutor requestExecutor;
  @Override
  public String createServiceGuardDataCollectionTask(
      String accountId, String cvConfigId, String connectorId, String dataCollectionWorkerId) {
    // Need to write this to handle retries, exception etc in a proper way.
    Map<String, String> params = new HashMap<>();
    params.put(DataCollectionTaskKeys.dataCollectionWorkerId, dataCollectionWorkerId);
    params.put(DataCollectionTaskKeys.cvConfigId, cvConfigId);
    params.put(CVConfigKeys.connectorId, connectorId);
    return requestExecutor.execute(verificationManagerClient.create(accountId, params)).getResource();
  }

  @Override
  public String createDeploymentVerificationDataCollectionTask(
      String accountId, String verificationTaskId, String connectorId, String dataCollectionWorkerId) {
    Map<String, String> params = new HashMap<>();
    params.put(DataCollectionTaskKeys.dataCollectionWorkerId, dataCollectionWorkerId);
    params.put(CVConfigKeys.connectorId, connectorId);
    params.put(DataCollectionTaskKeys.verificationTaskId, verificationTaskId);
    return requestExecutor.execute(verificationManagerClient.create(accountId, params)).getResource();
  }

  @Override
  public void deleteDataCollectionTask(String accountId, String taskId) {
    requestExecutor.execute(verificationManagerClient.deleteDataCollectionTask(accountId, taskId));
  }
}
