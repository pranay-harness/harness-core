/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.entities;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.DX)
public enum InstanceType {
  PHYSICAL_HOST_INSTANCE,
  EC2_CLOUD_INSTANCE,
  GCP_CLOUD_INSTANCE,
  ECS_CONTAINER_INSTANCE,
  K8S_INSTANCE,
  PCF_INSTANCE,
  AZURE_VMSS_INSTANCE,
  AZURE_WEB_APP_INSTANCE,
  KUBERNETES_CONTAINER_INSTANCE
}
