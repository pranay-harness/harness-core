package io.harness.batch.processing.billing.service;

import static io.harness.rule.OwnerRule.HITESH;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.batch.processing.billing.service.impl.ComputeInstancePricingStrategy;
import io.harness.batch.processing.billing.service.impl.EcsFargateInstancePricingStrategy;
import io.harness.batch.processing.billing.service.impl.StoragePricingStrategy;
import io.harness.batch.processing.pricing.service.impl.VMPricingServiceImpl;
import io.harness.batch.processing.pricing.service.intfc.AwsCustomBillingService;
import io.harness.batch.processing.pricing.service.intfc.AzureCustomBillingService;
import io.harness.batch.processing.service.intfc.CustomBillingMetaDataService;
import io.harness.batch.processing.service.intfc.InstanceResourceService;
import io.harness.batch.processing.service.intfc.PricingProfileService;
import io.harness.category.element.UnitTests;
import io.harness.ccm.commons.beans.InstanceType;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@OwnedBy(HarnessTeam.CE)
@RunWith(MockitoJUnitRunner.class)
public class InstancePricingStrategyContextTest extends CategoryTest {
  @Mock private VMPricingServiceImpl vmPricingService;
  @Mock private AwsCustomBillingService awsCustomBillingService;
  @Mock private AzureCustomBillingService azureCustomBillingService;
  @Mock private InstanceResourceService instanceResourceService;
  @Mock private EcsFargateInstancePricingStrategy ecsFargateInstancePricingStrategy;
  @Mock private CustomBillingMetaDataService customBillingMetaDataService;
  @Mock private PricingProfileService pricingProfileService;

  private static InstancePricingStrategyContext instancePricingStrategyContext;

  @Before
  public void before() {
    instancePricingStrategyContext = new InstancePricingStrategyContext(
        new ComputeInstancePricingStrategy(vmPricingService, awsCustomBillingService, azureCustomBillingService,
            instanceResourceService, ecsFargateInstancePricingStrategy, customBillingMetaDataService,
            pricingProfileService),
        new EcsFargateInstancePricingStrategy(vmPricingService, customBillingMetaDataService, awsCustomBillingService),
        new StoragePricingStrategy());
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetInstancePricingStrategy() {
    assertThat(instancePricingStrategyContext.getInstancePricingStrategy(InstanceType.EC2_INSTANCE))
        .isInstanceOf(ComputeInstancePricingStrategy.class);

    assertThat(instancePricingStrategyContext.getInstancePricingStrategy(InstanceType.ECS_TASK_FARGATE))
        .isInstanceOf(EcsFargateInstancePricingStrategy.class);

    assertThat(instancePricingStrategyContext.getInstancePricingStrategy(InstanceType.K8S_PV))
        .isInstanceOf(StoragePricingStrategy.class);
  }
}
