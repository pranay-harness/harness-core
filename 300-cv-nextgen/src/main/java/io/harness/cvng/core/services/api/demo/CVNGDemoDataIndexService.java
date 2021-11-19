package io.harness.cvng.core.services.api.demo;

public interface CVNGDemoDataIndexService {
  int readIndexForDemoData(String accountId, String dataCollectionWorkerId, String verificationTaskId);
  void saveIndexForDemoData(String accountId, String dataCollectionWorkerId, String verificationTaskId, int index);
}
