package io.harness.batch.processing.ccm;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum InstanceType {
  ECS_TASK_FARGATE(PricingGroup.ECS_FARGATE, 60, CostAttribution.COMPLETE),
  ECS_TASK_EC2(PricingGroup.COMPUTE, 1, CostAttribution.PARTIAL),
  ECS_CONTAINER_INSTANCE(PricingGroup.COMPUTE, 3600, CostAttribution.COMPLETE),
  EC2_INSTANCE(PricingGroup.COMPUTE, 3600, CostAttribution.COMPLETE),
  K8S_POD(PricingGroup.COMPUTE, 1, CostAttribution.PARTIAL),
  K8S_NODE(PricingGroup.COMPUTE, 3600, CostAttribution.COMPLETE),
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
