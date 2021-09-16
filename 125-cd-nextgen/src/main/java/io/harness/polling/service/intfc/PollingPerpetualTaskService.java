package io.harness.polling.service.intfc;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.polling.bean.PollingDocument;

@OwnedBy(HarnessTeam.CDC)
public interface PollingPerpetualTaskService {
  void createPerpetualTask(PollingDocument pollingDocument);
  void resetPerpetualTask(PollingDocument pollingDocument);
  void deletePerpetualTask(String perpetualTaskId, String accountId);
}
