package io.harness.service;

import io.harness.beans.FeatureName;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.entities.infrastructureMapping.InfrastructureMapping;

import software.wings.service.impl.instance.Status;

public interface IInstanceSyncByPerpetualTaskhandler<T extends DelegateResponseData> {
  FeatureName getFeatureFlagToEnablePerpetualTaskForInstanceSync();

  IInstanceSyncPerpetualTaskCreator getInstanceSyncPerpetualTaskCreator();

  void processInstanceSyncResponseFromPerpetualTask(InfrastructureMapping infrastructureMapping, T response);

  Status getStatus(InfrastructureMapping infrastructureMapping, T response);
}
