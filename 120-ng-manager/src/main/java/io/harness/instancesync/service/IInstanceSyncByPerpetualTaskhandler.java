package io.harness.instancesync.service;

import io.harness.beans.FeatureName;
import io.harness.delegate.beans.DelegateResponseData;

import software.wings.beans.InfrastructureMapping;
import software.wings.service.impl.instance.Status;

public interface IInstanceSyncByPerpetualTaskhandler {
  FeatureName getFeatureFlagToEnablePerpetualTaskForInstanceSync();

  IInstanceSyncPerpetualTaskCreator getInstanceSyncPerpetualTaskCreator();

  void processInstanceSyncResponseFromPerpetualTask(
      InfrastructureMapping infrastructureMapping, DelegateResponseData response);

  Status getStatus(InfrastructureMapping infrastructureMapping, DelegateResponseData response);
}
