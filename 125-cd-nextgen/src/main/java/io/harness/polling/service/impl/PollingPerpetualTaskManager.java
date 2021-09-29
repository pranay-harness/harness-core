package io.harness.polling.service.impl;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.polling.bean.PollingDocument;
import io.harness.polling.service.intfc.PollingPerpetualTaskService;
import io.harness.polling.service.intfc.PollingServiceObserver;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.CDC)
@Singleton
public class PollingPerpetualTaskManager implements PollingServiceObserver {
  private PollingPerpetualTaskService pollingPerpetualTaskService;

  @Inject
  public PollingPerpetualTaskManager(PollingPerpetualTaskService pollingPerpetualTaskService) {
    this.pollingPerpetualTaskService = pollingPerpetualTaskService;
  }

  @Override
  public void onSaved(PollingDocument pollingDocument) {
    pollingPerpetualTaskService.createPerpetualTask(pollingDocument);
  }

  @Override
  public void onUpdated(PollingDocument pollingDocument) {
    pollingPerpetualTaskService.resetPerpetualTask(pollingDocument);
  }

  @Override
  public void onDeleted(PollingDocument pollingDocument) {
    pollingPerpetualTaskService.deletePerpetualTask(
        pollingDocument.getPerpetualTaskId(), pollingDocument.getAccountId());
  }
}
