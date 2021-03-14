package io.harness.ccm.billing.graphql;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

@TargetModule(Module._490_CE_COMMONS)
public enum CloudSortType {
  Time,
  gcpCost,
  gcpProjectId,
  gcpProduct,
  gcpSkuId,
  gcpSkuDescription,
  awsUnblendedCost,
  awsBlendedCost,
  awsService,
  awsLinkedAccount;
}
