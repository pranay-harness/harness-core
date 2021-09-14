/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.service.instancesyncperpetualtask;

import static io.harness.perpetualtask.PerpetualTaskType.K8S_INSTANCE_SYNC;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.UnexpectedException;
import io.harness.service.instancesyncperpetualtask.instancesyncperpetualtaskhandler.InstanceSyncPerpetualTaskHandler;
import io.harness.service.instancesyncperpetualtask.instancesyncperpetualtaskhandler.k8s.K8SInstanceSyncPerpetualTaskHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;

@Singleton
@OwnedBy(HarnessTeam.DX)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public final class InstanceSyncPerpetualTaskServiceRegister {
  private final K8SInstanceSyncPerpetualTaskHandler k8sInstanceSyncPerpetualService;

  public InstanceSyncPerpetualTaskHandler getInstanceSyncPerpetualService(String perpetualTaskType) {
    switch (perpetualTaskType) {
      case K8S_INSTANCE_SYNC:
        return k8sInstanceSyncPerpetualService;
      default:
        throw new UnexpectedException(
            "No instance sync service registered for perpetual task type: " + perpetualTaskType);
    }
  }
}
