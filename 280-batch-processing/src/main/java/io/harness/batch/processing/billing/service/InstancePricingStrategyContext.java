package io.harness.batch.processing.billing.service;

import io.harness.batch.processing.billing.service.intfc.InstancePricingStrategy;
import io.harness.ccm.commons.beans.InstanceType;
import io.harness.ccm.commons.beans.PricingGroup;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InstancePricingStrategyContext {
  Map<PricingGroup, InstancePricingStrategy> pricingStrategyContext = new HashMap<>();

  @Autowired
  public InstancePricingStrategyContext(InstancePricingStrategy computeInstancePricingStrategy,
      InstancePricingStrategy ecsFargateInstancePricingStrategy) {
    pricingStrategyContext.put(PricingGroup.COMPUTE, computeInstancePricingStrategy);
    pricingStrategyContext.put(PricingGroup.ECS_FARGATE, ecsFargateInstancePricingStrategy);
  }

  InstancePricingStrategy getInstancePricingStrategy(InstanceType instanceType) {
    return pricingStrategyContext.get(instanceType.getPricingGroup());
  }
}
