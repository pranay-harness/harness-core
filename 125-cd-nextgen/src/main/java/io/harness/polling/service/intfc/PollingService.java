package io.harness.polling.service.intfc;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.polling.bean.PolledResponse;
import io.harness.polling.bean.PollingDocument;
import io.harness.polling.contracts.PollingItem;

import javax.validation.Valid;

@OwnedBy(HarnessTeam.CDC)
public interface PollingService {
  String save(@Valid PollingDocument pollingDocument);

  PollingDocument get(String accountId, String pollingDocId);

  String update(@Valid PollingDocument pollingDocument);

  void delete(PollingDocument pollingDocument);

  boolean attachPerpetualTask(String accountId, String pollDocId, String perpetualTaskId);

  void updateFailedAttempts(String accountId, String pollingDocId, int failedAttempts);

  void updatePolledResponse(String accountId, String pollingDocId, PolledResponse polledResponse);

  String subscribe(PollingItem pollingItem);

  boolean unsubscribe(PollingItem pollingItem);
}
