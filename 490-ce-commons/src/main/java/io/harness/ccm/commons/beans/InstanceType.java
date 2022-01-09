/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.commons.beans;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum InstanceType {
  ECS_TASK_FARGATE(PricingGroup.ECS_FARGATE, 60, CostAttribution.COMPLETE),
  ECS_TASK_EC2(PricingGroup.COMPUTE, 1, CostAttribution.PARTIAL),
  ECS_CONTAINER_INSTANCE(PricingGroup.COMPUTE, 3600, CostAttribution.COMPLETE),
  EC2_INSTANCE(PricingGroup.COMPUTE, 3600, CostAttribution.COMPLETE),
  K8S_POD(PricingGroup.COMPUTE, 1, CostAttribution.PARTIAL),
  K8S_POD_FARGATE(PricingGroup.COMPUTE, 60, CostAttribution.COMPLETE),
  K8S_NODE(PricingGroup.COMPUTE, 3600, CostAttribution.COMPLETE),
  K8S_PV(PricingGroup.STORAGE, 1, CostAttribution.COMPLETE),
  K8S_PVC(PricingGroup.STORAGE, 1, CostAttribution.PARTIAL),
  CLUSTER_UNALLOCATED(PricingGroup.COMPUTE, 3600, CostAttribution.COMPLETE);

  private final PricingGroup pricingGroup;
  private final double minChargeableSeconds;
  private final CostAttribution costAttribution;

  public PricingGroup getPricingGroup() {
    return pricingGroup;
  }
  public double getMinChargeableSeconds() {
    return minChargeableSeconds;
  }
  public CostAttribution getCostAttribution() {
    return costAttribution;
  }
}
