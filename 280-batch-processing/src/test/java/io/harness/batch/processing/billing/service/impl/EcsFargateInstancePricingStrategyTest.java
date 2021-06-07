package io.harness.batch.processing.billing.service.impl;

import static io.harness.rule.OwnerRule.HITESH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.batch.processing.billing.service.PricingData;
import io.harness.batch.processing.pricing.data.EcsFargatePricingInfo;
import io.harness.batch.processing.pricing.service.intfc.AwsCustomBillingService;
import io.harness.batch.processing.pricing.service.intfc.VMPricingService;
import io.harness.batch.processing.service.intfc.CustomBillingMetaDataService;
import io.harness.category.element.UnitTests;
import io.harness.ccm.commons.beans.Resource;
import io.harness.ccm.commons.constants.CloudProvider;
import io.harness.ccm.commons.constants.InstanceMetaDataConstants;
import io.harness.ccm.commons.entities.batch.InstanceData;
import io.harness.rule.Owner;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EcsFargateInstancePricingStrategyTest extends CategoryTest {
  @InjectMocks private EcsFargateInstancePricingStrategy ecsFargateInstancePricingStrategy;
  @Mock VMPricingService vmPricingService;
  @Mock CustomBillingMetaDataService customBillingMetaDataService;
  @Mock AwsCustomBillingService awsCustomBillingService;

  private static final Instant NOW = Instant.now();
  private final String REGION = "us-east-1";
  private final String ACCOUNT_ID = "accountId";

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetPricePerHour() {
    Map<String, String> metaData = new HashMap<>();
    metaData.put(InstanceMetaDataConstants.CLOUD_PROVIDER, CloudProvider.AWS.name());
    metaData.put(InstanceMetaDataConstants.REGION, REGION);
    doReturn(EcsFargatePricingInfo.builder().region(REGION).cpuPrice(0.04656).memoryPrice(0.00511).build())
        .when(vmPricingService)
        .getFargatePricingInfo(REGION);
    doReturn(null).when(customBillingMetaDataService).getAwsDataSetId(ACCOUNT_ID);

    InstanceData instanceData = InstanceData.builder()
                                    .metaData(metaData)
                                    .accountId(ACCOUNT_ID)
                                    .totalResource(Resource.builder().cpuUnits(1.0 * 1024).memoryMb(8.0 * 1024).build())
                                    .build();

    PricingData pricingData = ecsFargateInstancePricingStrategy.getPricePerHour(
        instanceData, NOW.minus(24, ChronoUnit.HOURS), NOW, 24 * 3600D, 24 * 3600D);

    assertThat(pricingData.getPricePerHour()).isEqualTo(0.08743999999999999);
    assertThat(pricingData.getCpuUnit()).isEqualTo(1.0 * 1024);
    assertThat(pricingData.getMemoryMb()).isEqualTo(8.0 * 1024);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetPricePerHourFromPricingSource() {
    Map<String, String> metaData = new HashMap<>();
    metaData.put(InstanceMetaDataConstants.CLOUD_PROVIDER, CloudProvider.AWS.name());
    metaData.put(InstanceMetaDataConstants.REGION, REGION);
    doReturn(EcsFargatePricingInfo.builder().region(REGION).cpuPrice(0.04656).memoryPrice(0.00511).build())
        .when(vmPricingService)
        .getFargatePricingInfo(REGION);
    doReturn(null).when(customBillingMetaDataService).getAwsDataSetId(ACCOUNT_ID);

    InstanceData instanceData =
        InstanceData.builder()
            .metaData(metaData)
            .accountId(ACCOUNT_ID)
            .totalResource(Resource.builder().cpuUnits(1.0 * 1024).memoryMb(8.0 * 1024).build())
            .pricingResource(Resource.builder().cpuUnits(2.0 * 1024).memoryMb(9.0 * 1024).build())
            .build();

    PricingData pricingData = ecsFargateInstancePricingStrategy.getPricePerHour(
        instanceData, NOW.minus(24, ChronoUnit.HOURS), NOW, 24 * 3600D, 24 * 3600D);

    assertThat(pricingData.getPricePerHour()).isEqualTo(0.13911);
    assertThat(pricingData.getCpuUnit()).isEqualTo(1.0 * 1024);
    assertThat(pricingData.getMemoryMb()).isEqualTo(8.0 * 1024);
  }
}