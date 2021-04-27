package io.harness.instancesync.service;

import io.harness.beans.FeatureName;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.instancesync.entity.infrastructureMapping.InfrastructureMapping;

import software.wings.service.impl.instance.Status;

public interface IInstanceSyncByPerpetualTaskhandler {
  FeatureName getFeatureFlagToEnablePerpetualTaskForInstanceSync();

  IInstanceSyncPerpetualTaskCreator getInstanceSyncPerpetualTaskCreator();

  void processInstanceSyncResponseFromPerpetualTask(
      InfrastructureMapping infrastructureMapping, DelegateResponseData response);

  Status getStatus(InfrastructureMapping infrastructureMapping, DelegateResponseData response);
}
