/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.models.dashboard;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.environment.beans.EnvironmentType;

import java.util.Map;

@OwnedBy(HarnessTeam.DX)
public class InstanceCountDetailsByService extends InstanceCountDetailsByEnvTypeBase {
  private String serviceId;

  public InstanceCountDetailsByService(Map<EnvironmentType, Integer> envTypeVsInstanceCountMap, String serviceId) {
    super(envTypeVsInstanceCountMap);
    this.serviceId = serviceId;
  }
}
