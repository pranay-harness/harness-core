package io.harness.polling.service.intfc;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.polling.bean.PollingDocument;

@OwnedBy(HarnessTeam.CDC)
public interface PollingServiceObserver {
  void onSaved(PollingDocument pollingDocument);
  void onUpdated(PollingDocument pollingDocument);
  void onDeleted(PollingDocument pollingDocument);
}
