/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.anomaly.entities;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public enum EntityType {
  CLUSTER,
  NAMESPACE,
  WORKLOAD,
  GCP_PRODUCT,
  GCP_SKU_ID,
  GCP_PROJECT,
  GCP_REGION,
  AWS_SERVICE,
  AWS_ACCOUNT,
  AWS_INSTANCE_TYPE,
  AWS_USAGE_TYPE;
}
