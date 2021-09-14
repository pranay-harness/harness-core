/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.cluster.entities;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(CE)
@TargetModule(HarnessModule._490_CE_COMMONS)
public class ClusterType {
  public static final String DIRECT_KUBERNETES = "DIRECT_KUBERNETES";
  public static final String AWS_ECS = "AWS_ECS";
  public static final String GCP_KUBERNETES = "GCP_KUBERNETES";
  public static final String AZURE_KUBERNETES = "AZURE_KUBERNETES";
}
