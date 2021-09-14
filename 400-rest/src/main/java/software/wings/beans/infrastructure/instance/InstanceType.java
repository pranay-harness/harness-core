/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.infrastructure.instance;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

/**
 * @author rktummala on 09/07/17
 */
@OwnedBy(CDP)
@TargetModule(HarnessModule._957_CG_BEANS)
public enum InstanceType {
  PHYSICAL_HOST_INSTANCE,
  EC2_CLOUD_INSTANCE,
  GCP_CLOUD_INSTANCE,
  ECS_CONTAINER_INSTANCE,
  KUBERNETES_CONTAINER_INSTANCE,
  PCF_INSTANCE,
  AZURE_VMSS_INSTANCE,
  AZURE_WEB_APP_INSTANCE
}
