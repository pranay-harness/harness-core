package io.harness.cvng.core.services.impl;

import io.harness.cvng.client.VerificationManagerService;
import io.harness.cvng.core.entities.changeSource.KubernetesChangeSource;
import io.harness.cvng.core.services.api.monitoredService.ChangeSourceService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KubernetesChangeSourceUpdateHandler extends ChangeSourceUpdateHandler<KubernetesChangeSource> {
  @Inject private VerificationManagerService verificationManagerService;
  @Inject private ChangeSourceService changeSourceService;

  @Override
  public void handleCreate(KubernetesChangeSource changeSource) {
    if (changeSource.isConfiguredForDemo()) {
      log.info("Not creating perpetual task because change source is configured for demo");
    } else {
      log.info("Enqueuing change source {}", changeSource.getIdentifier());
      changeSourceService.enqueueDataCollectionTask(changeSource);
      log.info("Completed change source {}", changeSource.getIdentifier());
    }
  }

  @Override
  public void handleUpdate(KubernetesChangeSource existingChangeSource, KubernetesChangeSource newChangeSource) {}

  @Override
  public void handleDelete(KubernetesChangeSource changeSource) {
    if (changeSource.getDataCollectionTaskId() != null) {
      verificationManagerService.deletePerpetualTask(
          changeSource.getAccountId(), changeSource.getDataCollectionTaskId());
    }
  }
}
