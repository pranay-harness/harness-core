package io.harness.ccm.anomaly.entities;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

@TargetModule(Module._490_CE_COMMONS)
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
