package io.harness.ccm.billing.graphql;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
@OwnedBy(CE)
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
  awsLinkedAccount,
  awsUsageType,
  awsInstanceType;
}
