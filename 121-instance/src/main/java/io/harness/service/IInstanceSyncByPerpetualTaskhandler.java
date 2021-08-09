package io.harness.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.entities.InfrastructureMapping;

import software.wings.service.impl.instance.Status;

@OwnedBy(HarnessTeam.DX)
public interface IInstanceSyncByPerpetualTaskhandler<T extends DelegateResponseData> {
  IInstanceSyncPerpetualTaskCreator getInstanceSyncPerpetualTaskCreator();

  void processInstanceSyncResponseFromPerpetualTask(InfrastructureMapping infrastructureMapping, T response);

  Status getStatus(InfrastructureMapping infrastructureMapping, T response);
}
