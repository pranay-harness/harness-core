package io.harness.cvng.core.services.impl;

import io.harness.cvng.client.VerificationManagerService;
import io.harness.cvng.core.entities.changeSource.KubernetesChangeSource;

import com.google.inject.Inject;

public class KubernetesChangeSourceUpdateHandler extends ChangeSourceUpdateHandler<KubernetesChangeSource> {
  @Inject private VerificationManagerService verificationManagerService;

  @Override
  public void handleCreate(KubernetesChangeSource changeSource) {}

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
