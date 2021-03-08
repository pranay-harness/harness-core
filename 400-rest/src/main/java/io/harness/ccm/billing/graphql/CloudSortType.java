package io.harness.ccm.billing.graphql;

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
