package io.harness.batch.processing.billing.service;

import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.batch.processing.billing.service.impl.ComputeInstancePricingStrategy;
import io.harness.batch.processing.billing.service.impl.EcsFargateInstancePricingStrategy;
import io.harness.batch.processing.billing.service.intfc.InstancePricingStrategy;
import io.harness.batch.processing.ccm.InstanceType;
import io.harness.batch.processing.pricing.service.impl.VMPricingServiceImpl;
import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class InstancePricingStrategyContextTest extends CategoryTest {
  @Mock private VMPricingServiceImpl vmPricingService;

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetInstancePricingStrategy() {
    InstancePricingStrategyContext instancePricingStrategyContext = new InstancePricingStrategyContext(
        new ComputeInstancePricingStrategy(vmPricingService), new EcsFargateInstancePricingStrategy(vmPricingService));
    InstancePricingStrategy computeInstancePricingStrategy =
        instancePricingStrategyContext.getInstancePricingStrategy(InstanceType.EC2_INSTANCE);
    assertThat(computeInstancePricingStrategy.getClass()).isEqualTo(ComputeInstancePricingStrategy.class);
    InstancePricingStrategy ecsFargateInstancePricingStrategy =
        instancePricingStrategyContext.getInstancePricingStrategy(InstanceType.ECS_TASK_FARGATE);
    assertThat(ecsFargateInstancePricingStrategy.getClass()).isEqualTo(EcsFargateInstancePricingStrategy.class);
  }
}
