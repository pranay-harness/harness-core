package io.harness.batch.processing.pricing.service.impl;

import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.pricing.data.VMInstanceBillingData;
import io.harness.batch.processing.pricing.gcp.bigquery.BigQueryHelperService;
import io.harness.batch.processing.writer.constants.InstanceMetaDataConstants;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class AwsCustomBillingServiceImplTest extends CategoryTest {
  @InjectMocks private AwsCustomBillingServiceImpl awsCustomBillingService;
  @Mock BigQueryHelperService bigQueryHelperService;

  private final String DATA_SET_ID = "dataSetId";
  private final String RESOURCE_ID = "resourceId1";
  private final double COMPUTE_COST = 10.5;
  private final double NETWORK_COST = 20.5;
  private final Instant NOW = Instant.now();
  private final Instant START_TIME = NOW.minus(1, ChronoUnit.HOURS);
  private final Instant END_TIME = NOW;

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testGetgetComputeVMPricingInfo() {
    Map<String, VMInstanceBillingData> vmInstanceBillingDataMap = new HashMap<>();
    VMInstanceBillingData vmInstanceBillingData = VMInstanceBillingData.builder()
                                                      .computeCost(COMPUTE_COST)
                                                      .networkCost(NETWORK_COST)
                                                      .resourceId(RESOURCE_ID)
                                                      .build();
    vmInstanceBillingDataMap.put(RESOURCE_ID, vmInstanceBillingData);
    List<String> resourceIds = Collections.singletonList("resourceId1");
    when(bigQueryHelperService.getAwsEC2BillingData(resourceIds, START_TIME, END_TIME, DATA_SET_ID))
        .thenReturn(vmInstanceBillingDataMap);
    awsCustomBillingService.updateAwsEC2BillingDataCache(resourceIds, START_TIME, END_TIME, DATA_SET_ID);
    Map<String, String> metaData = new HashMap<>();
    metaData.put(InstanceMetaDataConstants.CLOUD_PROVIDER_INSTANCE_ID, RESOURCE_ID);
    InstanceData instanceData = InstanceData.builder().metaData(metaData).build();
    VMInstanceBillingData computeVMPricingInfo =
        awsCustomBillingService.getComputeVMPricingInfo(instanceData, START_TIME, END_TIME);
    assertThat(computeVMPricingInfo).isEqualTo(vmInstanceBillingData);
  }
}
