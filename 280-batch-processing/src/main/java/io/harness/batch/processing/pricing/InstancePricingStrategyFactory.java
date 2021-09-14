/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.pricing;

import io.harness.ccm.commons.beans.InstanceType;
import io.harness.ccm.commons.beans.PricingGroup;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InstancePricingStrategyFactory {
  Map<PricingGroup, InstancePricingStrategy> pricingStrategyContext = new HashMap<>();

  @Autowired
  public InstancePricingStrategyFactory(InstancePricingStrategy computeInstancePricingStrategy,
      InstancePricingStrategy ecsFargateInstancePricingStrategy, InstancePricingStrategy storagePricingStrategy) {
    pricingStrategyContext.put(PricingGroup.COMPUTE, computeInstancePricingStrategy);
    pricingStrategyContext.put(PricingGroup.ECS_FARGATE, ecsFargateInstancePricingStrategy);
    pricingStrategyContext.put(PricingGroup.STORAGE, storagePricingStrategy);
  }

  public InstancePricingStrategy getInstancePricingStrategy(InstanceType instanceType) {
    return pricingStrategyContext.get(instanceType.getPricingGroup());
  }
}
