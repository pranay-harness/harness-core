package io.harness.cvng.client;

import com.google.inject.Inject;

public class VerificationManagerServiceImpl implements VerificationManagerService {
  @Inject private VerificationManagerClient verificationManagerClient;
  @Inject private RequestExecutor requestExecutor;
  @Override
  public String createDataCollectionTask(String accountId, String cvConfigId, String connectorId) {
    return requestExecutor
        .execute(verificationManagerClient.createDataCollectionTask(accountId, cvConfigId, connectorId))
        .getResource();
  }

  @Override
  public void deleteDataCollectionTask(String accountId, String taskId) {
    requestExecutor.execute(verificationManagerClient.deleteDataCollectionTask(accountId, taskId));
  }
}
