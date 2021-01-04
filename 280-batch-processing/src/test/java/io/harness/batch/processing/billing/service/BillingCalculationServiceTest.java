package io.harness.batch.processing.billing.service;

import static io.harness.rule.OwnerRule.HITESH;
import static io.harness.rule.OwnerRule.ROHIT;
import static io.harness.rule.OwnerRule.SANDESH;
import static io.harness.rule.OwnerRule.SHUBHANSHU;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.batch.processing.billing.service.impl.ComputeInstancePricingStrategy;
import io.harness.batch.processing.billing.service.impl.EcsFargateInstancePricingStrategy;
import io.harness.batch.processing.ccm.ClusterType;
import io.harness.batch.processing.ccm.InstanceCategory;
import io.harness.batch.processing.ccm.PricingSource;
import io.harness.batch.processing.pricing.data.CloudProvider;
import io.harness.batch.processing.pricing.data.EcsFargatePricingInfo;
import io.harness.batch.processing.pricing.data.VMComputePricingInfo;
import io.harness.batch.processing.pricing.data.VMInstanceBillingData;
import io.harness.batch.processing.pricing.data.ZonePrice;
import io.harness.batch.processing.pricing.service.impl.VMPricingServiceImpl;
import io.harness.batch.processing.pricing.service.intfc.AwsCustomBillingService;
import io.harness.batch.processing.service.intfc.CustomBillingMetaDataService;
import io.harness.batch.processing.service.intfc.InstanceResourceService;
import io.harness.batch.processing.service.intfc.PricingProfileService;
import io.harness.batch.processing.writer.constants.InstanceMetaDataConstants;
import io.harness.category.element.UnitTests;
import io.harness.ccm.cluster.entities.PricingProfile;
import io.harness.ccm.commons.beans.InstanceType;
import io.harness.ccm.commons.beans.Resource;
import io.harness.ccm.commons.entities.InstanceData;
import io.harness.rule.Owner;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BillingCalculationServiceTest extends CategoryTest {
  @InjectMocks private BillingCalculationService billingCalculationService;
  @Mock private InstancePricingStrategyContext instancePricingStrategyRegistry;
  @Mock private VMPricingServiceImpl vmPricingService;
  @Mock private AwsCustomBillingService awsCustomBillingService;
  @Mock private InstanceResourceService instanceResourceService;
  @Mock private EcsFargateInstancePricingStrategy ecsFargateInstancePricingStrategy;
  @Mock private CustomBillingMetaDataService customBillingMetaDataService;
  @Mock private PricingProfileService pricingProfileService;

  private final Instant NOW = Instant.now().truncatedTo(ChronoUnit.DAYS);
  private final Instant INSTANCE_STOP_TIMESTAMP = NOW;
  private final Instant INSTANCE_START_TIMESTAMP = NOW.minus(1, ChronoUnit.DAYS);
  private final Long ONE_DAY_SECONDS = 86400l;
  private final Long HALF_DAY_SECONDS = 43200l;

  private final String ACCOUNT_ID = "accountId";
  private final String REGION = "us-east-1";
  private final String GCP_REGION = "us-central1";
  private final String GCP_ZONE_1 = "us-central1-a";
  private final String GCP_ZONE_2 = "us-central1-b";
  private final String DEFAULT_INSTANCE_FAMILY = "c4.8xlarge";
  private final String GCP_INSTANCE_FAMILY = "n1-standard-4";
  private final String GCP_CUSTOM_INSTANCE_FAMILY = "custom-8-20480";
  private final double DEFAULT_INSTANCE_CPU = 36;
  private final double DEFAULT_INSTANCE_MEMORY = 60;
  private final double DEFAULT_INSTANCE_PRICE = 1.60;
  private final double CPU_UTILIZATION = 0.5;
  private final double CPU_UTILIZATION_HIGH = 1.5;
  private final double MEMORY_UTILIZATION = 0.5;
  private final double MEMORY_UTILIZATION_HIGH = 1.5;

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetActiveInstanceTimeInInterval() {
    InstanceData instanceData = getInstanceWithTime(INSTANCE_START_TIMESTAMP, INSTANCE_STOP_TIMESTAMP);
    Long activeInstanceTimeInInterval = billingCalculationService.getActiveInstanceTimeInInterval(
        instanceData, INSTANCE_START_TIMESTAMP, INSTANCE_STOP_TIMESTAMP);
    assertThat(activeInstanceTimeInInterval).isEqualTo(ONE_DAY_SECONDS);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetActiveInstanceTimeInIntervalWhenStartedBefore() {
    InstanceData instanceData =
        getInstanceWithTime(INSTANCE_START_TIMESTAMP.minus(2, ChronoUnit.DAYS), INSTANCE_STOP_TIMESTAMP);
    Long activeInstanceTimeInInterval = billingCalculationService.getActiveInstanceTimeInInterval(
        instanceData, INSTANCE_START_TIMESTAMP, INSTANCE_STOP_TIMESTAMP);
    assertThat(activeInstanceTimeInInterval).isEqualTo(ONE_DAY_SECONDS);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetActiveInstanceTimeInIntervalWhenStartedAfter() {
    InstanceData instanceData =
        getInstanceWithTime(INSTANCE_START_TIMESTAMP.plus(12, ChronoUnit.HOURS), INSTANCE_STOP_TIMESTAMP);
    Long activeInstanceTimeInInterval = billingCalculationService.getActiveInstanceTimeInInterval(
        instanceData, INSTANCE_START_TIMESTAMP, INSTANCE_STOP_TIMESTAMP);
    assertThat(activeInstanceTimeInInterval).isEqualTo(HALF_DAY_SECONDS);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetActiveInstanceTimeInIntervalWhenStoppedBefore() {
    InstanceData instanceData =
        getInstanceWithTime(INSTANCE_START_TIMESTAMP, INSTANCE_STOP_TIMESTAMP.minus(12, ChronoUnit.HOURS));
    Long activeInstanceTimeInInterval = billingCalculationService.getActiveInstanceTimeInInterval(
        instanceData, INSTANCE_START_TIMESTAMP, INSTANCE_STOP_TIMESTAMP);
    assertThat(activeInstanceTimeInInterval).isEqualTo(HALF_DAY_SECONDS);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetInstanceActiveSeconds() {
    InstanceData instanceData = InstanceData.builder()
                                    .usageStartTime(INSTANCE_START_TIMESTAMP)
                                    .usageStopTime(INSTANCE_STOP_TIMESTAMP)
                                    .instanceType(InstanceType.ECS_TASK_FARGATE)
                                    .build();
    double instanceActiveSeconds = billingCalculationService.getInstanceActiveSeconds(
        instanceData, INSTANCE_START_TIMESTAMP, INSTANCE_STOP_TIMESTAMP);
    assertThat(instanceActiveSeconds).isEqualTo(ONE_DAY_SECONDS.doubleValue());
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetInstanceActiveSecondsWhenInstanceActiveTimeIsLessThanMinDuration() {
    InstanceData instanceData = InstanceData.builder()
                                    .usageStartTime(INSTANCE_START_TIMESTAMP)
                                    .usageStopTime(INSTANCE_START_TIMESTAMP.plus(1, ChronoUnit.MINUTES))
                                    .instanceType(InstanceType.EC2_INSTANCE)
                                    .build();
    double instanceActiveSeconds = billingCalculationService.getInstanceActiveSeconds(
        instanceData, INSTANCE_START_TIMESTAMP, INSTANCE_STOP_TIMESTAMP);
    assertThat(instanceActiveSeconds).isEqualTo(3600);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetInstanceActiveSecondsWhenInstanceIsRunning() {
    InstanceData instanceData = InstanceData.builder()
                                    .usageStartTime(INSTANCE_STOP_TIMESTAMP.minus(2, ChronoUnit.SECONDS))
                                    .instanceType(InstanceType.EC2_INSTANCE)
                                    .build();
    double instanceActiveSeconds = billingCalculationService.getInstanceActiveSeconds(
        instanceData, INSTANCE_START_TIMESTAMP, INSTANCE_STOP_TIMESTAMP);
    assertThat(instanceActiveSeconds).isEqualTo(2);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetInstanceActiveSecondsWhenInstanceActiveTimeIsLessThanMinChargeableTime() {
    InstanceData instanceData = InstanceData.builder()
                                    .usageStartTime(INSTANCE_START_TIMESTAMP.minus(4, ChronoUnit.SECONDS))
                                    .usageStopTime(INSTANCE_START_TIMESTAMP.plus(40, ChronoUnit.SECONDS))
                                    .instanceType(InstanceType.ECS_TASK_FARGATE)
                                    .build();
    double instanceActiveSeconds = billingCalculationService.getInstanceActiveSeconds(
        instanceData, INSTANCE_START_TIMESTAMP, INSTANCE_STOP_TIMESTAMP);
    assertThat(instanceActiveSeconds).isEqualTo(56);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetInstanceActiveSecondsWhenInstanceActiveTimeIsEqualToMinChargeableTime() {
    InstanceData instanceData = InstanceData.builder()
                                    .usageStartTime(INSTANCE_START_TIMESTAMP.minus(6, ChronoUnit.SECONDS))
                                    .usageStopTime(INSTANCE_START_TIMESTAMP.plus(54, ChronoUnit.SECONDS))
                                    .instanceType(InstanceType.ECS_TASK_FARGATE)
                                    .build();
    double instanceActiveSeconds = billingCalculationService.getInstanceActiveSeconds(
        instanceData, INSTANCE_START_TIMESTAMP, INSTANCE_STOP_TIMESTAMP);
    assertThat(instanceActiveSeconds).isEqualTo(54);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetInstanceActiveSecondsWhenInstanceActiveTimeIsGreaterThanMinChargeableTime() {
    InstanceData instanceData = InstanceData.builder()
                                    .usageStartTime(INSTANCE_START_TIMESTAMP.minus(6, ChronoUnit.SECONDS))
                                    .usageStopTime(INSTANCE_START_TIMESTAMP.plus(57, ChronoUnit.SECONDS))
                                    .instanceType(InstanceType.ECS_TASK_FARGATE)
                                    .build();
    double instanceActiveSeconds = billingCalculationService.getInstanceActiveSeconds(
        instanceData, INSTANCE_START_TIMESTAMP, INSTANCE_STOP_TIMESTAMP);
    assertThat(instanceActiveSeconds).isEqualTo(57);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetInstanceActiveSecondsWhenInstanceActiveStartTimeIsNotInRange() {
    InstanceData instanceData = InstanceData.builder()
                                    .usageStartTime(INSTANCE_STOP_TIMESTAMP.plus(6, ChronoUnit.HOURS))
                                    .instanceType(InstanceType.ECS_TASK_FARGATE)
                                    .build();
    double instanceActiveSeconds = billingCalculationService.getInstanceActiveSeconds(
        instanceData, INSTANCE_START_TIMESTAMP, INSTANCE_STOP_TIMESTAMP);
    assertThat(instanceActiveSeconds).isEqualTo(0);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetInstanceActiveSecondsWhenInstanceActiveEndTimeIsNotInRange() {
    InstanceData instanceData = InstanceData.builder()
                                    .usageStartTime(INSTANCE_START_TIMESTAMP.minus(6, ChronoUnit.HOURS))
                                    .usageStopTime(INSTANCE_START_TIMESTAMP.minus(2, ChronoUnit.HOURS))
                                    .instanceType(InstanceType.ECS_TASK_FARGATE)
                                    .build();
    double instanceActiveSeconds = billingCalculationService.getInstanceActiveSeconds(
        instanceData, INSTANCE_START_TIMESTAMP, INSTANCE_STOP_TIMESTAMP);
    assertThat(instanceActiveSeconds).isEqualTo(0);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetBillingAmountForResource() {
    Resource instanceResource = getInstanceResource(256, 512);
    Map<String, String> metaData = new HashMap<>();
    addParentResource(metaData, 1024, 1024);
    InstanceData instanceData = getInstance(instanceResource, instanceResource, metaData, INSTANCE_START_TIMESTAMP,
        INSTANCE_STOP_TIMESTAMP, InstanceType.ECS_TASK_EC2);
    BillingAmountBreakup billingAmountForResource = billingCalculationService.getBillingAmountForResource(
        instanceData, BigDecimal.valueOf(200), instanceResource.getCpuUnits(), instanceResource.getMemoryMb());
    assertThat(billingAmountForResource.getBillingAmount()).isEqualTo(new BigDecimal("75.000"));
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testGetIdleCostForResource() {
    Resource instanceResource = getInstanceResource(256, 512);
    Map<String, String> metaData = new HashMap<>();
    addParentResource(metaData, 1024, 1024);
    InstanceData instanceData = getInstance(instanceResource, instanceResource, metaData, INSTANCE_START_TIMESTAMP,
        INSTANCE_STOP_TIMESTAMP, InstanceType.ECS_TASK_EC2);
    BigDecimal billingAmount = BigDecimal.valueOf(200);
    BigDecimal cpuBillingAmount = BigDecimal.valueOf(100);
    BigDecimal memoryBillingAmount = BigDecimal.valueOf(100);
    BillingAmountBreakup billingAmountBreakup =
        new BillingAmountBreakup(billingAmount, cpuBillingAmount, memoryBillingAmount);
    UtilizationData utilizationData = getUtilization(CPU_UTILIZATION, MEMORY_UTILIZATION);
    IdleCostData idleCost =
        billingCalculationService.getIdleCostForResource(billingAmountBreakup, utilizationData, instanceData);
    assertThat(idleCost.getIdleCost()).isEqualTo(BigDecimal.valueOf(100.0));
    assertThat(idleCost.getMemoryIdleCost()).isEqualTo(BigDecimal.valueOf(50.0));
    assertThat(idleCost.getCpuIdleCost()).isEqualTo(BigDecimal.valueOf(50.0));
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetBillingAmount() {
    PricingData pricingData = new PricingData(0, 10, 256.0, 512.0, PricingSource.PUBLIC_API);
    Resource instanceResource = getInstanceResource(256, 512);
    Map<String, String> metaData = new HashMap<>();
    metaData.put(InstanceMetaDataConstants.CLUSTER_TYPE, ClusterType.ECS.name());
    addParentResource(metaData, 1024, 1024);
    metaData.put(InstanceMetaDataConstants.CLUSTER_TYPE, ClusterType.ECS.name());
    InstanceData instanceData = getInstance(instanceResource, instanceResource, metaData, INSTANCE_START_TIMESTAMP,
        INSTANCE_STOP_TIMESTAMP, InstanceType.ECS_TASK_EC2);
    UtilizationData utilizationData = getUtilization(CPU_UTILIZATION, MEMORY_UTILIZATION);
    BillingData billingAmount =
        billingCalculationService.getBillingAmount(instanceData, utilizationData, pricingData, ONE_DAY_SECONDS);
    assertThat(billingAmount.getBillingAmountBreakup().getBillingAmount()).isEqualTo(new BigDecimal("90.0000"));
    assertThat(billingAmount.getBillingAmountBreakup().getCpuBillingAmount()).isEqualTo(new BigDecimal("30.0000"));
    assertThat(billingAmount.getBillingAmountBreakup().getMemoryBillingAmount()).isEqualTo(new BigDecimal("60.000"));
    assertThat(billingAmount.getIdleCostData().getIdleCost()).isEqualTo(new BigDecimal("45.0"));
    assertThat(billingAmount.getIdleCostData().getCpuIdleCost()).isEqualTo(new BigDecimal("15.0"));
    assertThat(billingAmount.getIdleCostData().getMemoryIdleCost()).isEqualTo(new BigDecimal("30.0"));
    assertThat(billingAmount.getUsageDurationSeconds()).isEqualTo(ONE_DAY_SECONDS.doubleValue());
    assertThat(billingAmount.getCpuUnitSeconds()).isEqualTo(256.0 * ONE_DAY_SECONDS);
    assertThat(billingAmount.getMemoryMbSeconds()).isEqualTo(512.0 * ONE_DAY_SECONDS);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testGetBillingAmountWithZeroResourceInInstance() {
    PricingData pricingData = new PricingData(10, 10, 256.0, 512.0, PricingSource.PUBLIC_API);
    Resource instanceResource = getInstanceResource(0, 0);
    Map<String, String> metaData = new HashMap<>();
    addParentResource(metaData, 1024, 1024);
    metaData.put(InstanceMetaDataConstants.CLUSTER_TYPE, ClusterType.K8S.name());
    InstanceData instanceData = getInstance(instanceResource, instanceResource, metaData, INSTANCE_START_TIMESTAMP,
        INSTANCE_STOP_TIMESTAMP, InstanceType.K8S_POD);
    UtilizationData utilizationData = getUtilizationWithValues(CPU_UTILIZATION_HIGH, MEMORY_UTILIZATION_HIGH, 256, 512);
    BillingData billingAmount =
        billingCalculationService.getBillingAmount(instanceData, utilizationData, pricingData, ONE_DAY_SECONDS);
    assertThat(billingAmount.getBillingAmountBreakup().getBillingAmount()).isEqualTo(new BigDecimal("90.0000"));
    assertThat(billingAmount.getBillingAmountBreakup().getCpuBillingAmount()).isEqualTo(new BigDecimal("30.0000"));
    assertThat(billingAmount.getBillingAmountBreakup().getMemoryBillingAmount()).isEqualTo(new BigDecimal("60.000"));
    assertThat(billingAmount.getIdleCostData().getIdleCost().doubleValue()).isEqualTo(0.0);
    assertThat(billingAmount.getIdleCostData().getCpuIdleCost().doubleValue()).isEqualTo(0.0);
    assertThat(billingAmount.getIdleCostData().getMemoryIdleCost().doubleValue()).isEqualTo(0.0);
    assertThat(billingAmount.getUsageDurationSeconds()).isEqualTo(ONE_DAY_SECONDS.doubleValue());
    assertThat(billingAmount.getCpuUnitSeconds()).isEqualTo(256.0 * ONE_DAY_SECONDS);
    assertThat(billingAmount.getMemoryMbSeconds()).isEqualTo(512.0 * ONE_DAY_SECONDS);
    assertThat(billingAmount.getNetworkCost()).isEqualTo(0d);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetBillingAmountWhereResourceIsNotPresent() {
    PricingData pricingData = new PricingData(10, 10, 256.0, 512.0, PricingSource.CUR_REPORT);
    Map<String, String> metaData = new HashMap<>();
    InstanceData instanceData = getInstance(null, null, metaData, INSTANCE_START_TIMESTAMP,
        INSTANCE_STOP_TIMESTAMP.minus(12, ChronoUnit.HOURS), InstanceType.K8S_NODE);
    UtilizationData utilizationData = getUtilization(CPU_UTILIZATION, MEMORY_UTILIZATION);
    BillingData billingAmount =
        billingCalculationService.getBillingAmount(instanceData, utilizationData, pricingData, HALF_DAY_SECONDS);
    assertThat(billingAmount.getBillingAmountBreakup().getBillingAmount()).isEqualTo(new BigDecimal("120.0"));
    assertThat(billingAmount.getBillingAmountBreakup().getCpuBillingAmount()).isEqualTo(new BigDecimal("60.00"));
    assertThat(billingAmount.getBillingAmountBreakup().getMemoryBillingAmount()).isEqualTo(new BigDecimal("60.00"));
    assertThat(billingAmount.getIdleCostData().getIdleCost()).isEqualTo(new BigDecimal("60.0"));
    assertThat(billingAmount.getIdleCostData().getCpuIdleCost()).isEqualTo(new BigDecimal("30.0"));
    assertThat(billingAmount.getIdleCostData().getMemoryIdleCost()).isEqualTo(new BigDecimal("30.0"));
    assertThat(billingAmount.getUsageDurationSeconds()).isEqualTo(HALF_DAY_SECONDS.doubleValue());
    assertThat(billingAmount.getCpuUnitSeconds()).isEqualTo(256.0 * HALF_DAY_SECONDS);
    assertThat(billingAmount.getMemoryMbSeconds()).isEqualTo(512.0 * HALF_DAY_SECONDS);
    assertThat(billingAmount.getNetworkCost()).isEqualTo(10d);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetInstanceBillingAmountForCompute() throws IOException {
    when(vmPricingService.getComputeVMPricingInfo(DEFAULT_INSTANCE_FAMILY, REGION, CloudProvider.AWS))
        .thenReturn(createVMComputePricingInfo());
    when(instancePricingStrategyRegistry.getInstancePricingStrategy(InstanceType.ECS_TASK_EC2))
        .thenReturn(getComputeInstancePricingStrategy());
    Resource instanceResource = getInstanceResource(18432, 30720);
    Map<String, String> metaData = new HashMap<>();
    metaData.put(InstanceMetaDataConstants.CLOUD_PROVIDER, CloudProvider.AWS.name());
    metaData.put(InstanceMetaDataConstants.INSTANCE_FAMILY, DEFAULT_INSTANCE_FAMILY);
    metaData.put(InstanceMetaDataConstants.REGION, REGION);
    metaData.put(InstanceMetaDataConstants.INSTANCE_CATEGORY, InstanceCategory.ON_DEMAND.name());
    metaData.put(InstanceMetaDataConstants.CLUSTER_TYPE, ClusterType.ECS.name());
    addParentResource(metaData, DEFAULT_INSTANCE_CPU * 1024, DEFAULT_INSTANCE_MEMORY * 1024);
    InstanceData instanceData = getInstance(instanceResource, instanceResource, metaData, INSTANCE_START_TIMESTAMP,
        INSTANCE_STOP_TIMESTAMP.minus(12, ChronoUnit.HOURS), InstanceType.ECS_TASK_EC2);
    UtilizationData utilizationData = getUtilization(CPU_UTILIZATION, MEMORY_UTILIZATION);
    BillingData billingAmount = billingCalculationService.getInstanceBillingAmount(
        instanceData, utilizationData, INSTANCE_START_TIMESTAMP, INSTANCE_STOP_TIMESTAMP);
    assertThat(billingAmount.getBillingAmountBreakup().getBillingAmount()).isEqualTo(new BigDecimal("9.60"));
    assertThat(billingAmount.getIdleCostData().getIdleCost()).isEqualTo(new BigDecimal("4.8"));
    assertThat(billingAmount.getIdleCostData().getCpuIdleCost()).isEqualTo(new BigDecimal("2.4"));
    assertThat(billingAmount.getIdleCostData().getMemoryIdleCost()).isEqualTo(new BigDecimal("2.4"));
    assertThat(billingAmount.getUsageDurationSeconds()).isEqualTo(HALF_DAY_SECONDS.doubleValue());
    assertThat(billingAmount.getCpuUnitSeconds()).isEqualTo(18432 * HALF_DAY_SECONDS);
    assertThat(billingAmount.getMemoryMbSeconds()).isEqualTo(30720 * HALF_DAY_SECONDS);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetInstanceBillingAmountForComputeFromCurReport() throws IOException {
    when(instanceResourceService.getComputeVMResource(DEFAULT_INSTANCE_FAMILY, REGION, CloudProvider.AWS))
        .thenReturn(getResource());
    when(customBillingMetaDataService.getAwsDataSetId(ACCOUNT_ID)).thenReturn("AWSDataSet");
    when(instancePricingStrategyRegistry.getInstancePricingStrategy(InstanceType.K8S_NODE))
        .thenReturn(getComputeInstancePricingStrategy());
    Resource instanceResource = getInstanceResource(18432, 30720);
    Map<String, String> metaData = new HashMap<>();
    metaData.put(InstanceMetaDataConstants.CLOUD_PROVIDER, CloudProvider.AWS.name());
    metaData.put(InstanceMetaDataConstants.INSTANCE_FAMILY, DEFAULT_INSTANCE_FAMILY);
    metaData.put(InstanceMetaDataConstants.REGION, REGION);
    metaData.put(InstanceMetaDataConstants.INSTANCE_CATEGORY, InstanceCategory.ON_DEMAND.name());
    metaData.put(InstanceMetaDataConstants.CLUSTER_TYPE, ClusterType.ECS.name());
    addParentResource(metaData, DEFAULT_INSTANCE_CPU * 1024, DEFAULT_INSTANCE_MEMORY * 1024);
    InstanceData instanceData = getInstance(instanceResource, instanceResource, metaData, INSTANCE_START_TIMESTAMP,
        INSTANCE_STOP_TIMESTAMP.minus(12, ChronoUnit.HOURS), InstanceType.K8S_NODE);
    UtilizationData utilizationData = getUtilization(CPU_UTILIZATION, MEMORY_UTILIZATION);
    when(awsCustomBillingService.getComputeVMPricingInfo(
             instanceData, INSTANCE_START_TIMESTAMP, INSTANCE_STOP_TIMESTAMP))
        .thenReturn(
            VMInstanceBillingData.builder().resourceId("resourceId").networkCost(10.0).computeCost(40.0).build());
    BillingData billingAmount = billingCalculationService.getInstanceBillingAmount(
        instanceData, utilizationData, INSTANCE_START_TIMESTAMP, INSTANCE_STOP_TIMESTAMP);
    assertThat(billingAmount.getBillingAmountBreakup().getBillingAmount()).isEqualTo(new BigDecimal("40.0"));
    assertThat(billingAmount.getIdleCostData().getIdleCost()).isEqualTo(new BigDecimal("20.0"));
    assertThat(billingAmount.getIdleCostData().getCpuIdleCost()).isEqualTo(new BigDecimal("10.0"));
    assertThat(billingAmount.getIdleCostData().getMemoryIdleCost()).isEqualTo(new BigDecimal("10.0"));
    assertThat(billingAmount.getUsageDurationSeconds()).isEqualTo(HALF_DAY_SECONDS.doubleValue());
    assertThat(billingAmount.getCpuUnitSeconds()).isEqualTo(18432 * HALF_DAY_SECONDS);
    assertThat(billingAmount.getMemoryMbSeconds()).isEqualTo(30720 * HALF_DAY_SECONDS);
    assertThat(billingAmount.getNetworkCost()).isEqualTo(10.0);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetInstanceBillingAmountForSpotComputeInstance() throws IOException {
    when(vmPricingService.getComputeVMPricingInfo(GCP_INSTANCE_FAMILY, GCP_REGION, CloudProvider.GCP)).thenReturn(null);
    when(pricingProfileService.fetchPricingProfile(ACCOUNT_ID))
        .thenReturn(
            PricingProfile.builder().accountId(ACCOUNT_ID).vCpuPricePerHr(0.2).memoryGbPricePerHr(0.05).build());
    when(instancePricingStrategyRegistry.getInstancePricingStrategy(InstanceType.K8S_NODE))
        .thenReturn(getComputeInstancePricingStrategy());
    Resource totalResource = getInstanceResource(4096, 16384);
    Resource instanceResource = getInstanceResource(3988, 14360);
    Map<String, String> metaData = new HashMap<>();
    metaData.put(InstanceMetaDataConstants.CLOUD_PROVIDER, CloudProvider.GCP.name());
    metaData.put(InstanceMetaDataConstants.INSTANCE_FAMILY, GCP_INSTANCE_FAMILY);
    metaData.put(InstanceMetaDataConstants.REGION, GCP_REGION);
    metaData.put(InstanceMetaDataConstants.INSTANCE_CATEGORY, InstanceCategory.SPOT.name());
    metaData.put(InstanceMetaDataConstants.ZONE, GCP_ZONE_1);
    metaData.put(InstanceMetaDataConstants.CLUSTER_TYPE, ClusterType.K8S.name());
    InstanceData instanceData = getInstance(totalResource, instanceResource, metaData, INSTANCE_START_TIMESTAMP,
        INSTANCE_STOP_TIMESTAMP.minus(12, ChronoUnit.HOURS), InstanceType.K8S_NODE);
    UtilizationData utilizationData = getUtilization(CPU_UTILIZATION, MEMORY_UTILIZATION);
    BillingData billingAmount = billingCalculationService.getInstanceBillingAmount(
        instanceData, utilizationData, INSTANCE_START_TIMESTAMP, INSTANCE_STOP_TIMESTAMP);
    assertThat(billingAmount.getBillingAmountBreakup().getBillingAmount()).isEqualTo(new BigDecimal("19.2"));
    assertThat(billingAmount.getIdleCostData().getIdleCost()).isEqualTo(new BigDecimal("9.6"));
    assertThat(billingAmount.getIdleCostData().getCpuIdleCost()).isEqualTo(new BigDecimal("4.8"));
    assertThat(billingAmount.getIdleCostData().getMemoryIdleCost()).isEqualTo(new BigDecimal("4.8"));
    assertThat(billingAmount.getSystemCostData().getMemorySystemCost())
        .isEqualTo(BigDecimal.valueOf(1.1859374999999999));
    assertThat(billingAmount.getSystemCostData().getCpuSystemCost()).isEqualTo(BigDecimal.valueOf(0.253125));
    assertThat(billingAmount.getUsageDurationSeconds()).isEqualTo(HALF_DAY_SECONDS.doubleValue());
    assertThat(billingAmount.getCpuUnitSeconds()).isEqualTo(4096 * HALF_DAY_SECONDS);
    assertThat(billingAmount.getMemoryMbSeconds()).isEqualTo(16384 * HALF_DAY_SECONDS);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetInstanceBillingAmountForFargate() throws IOException {
    when(vmPricingService.getFargatePricingInfo(REGION)).thenReturn(createEcsFargatePricingInfo());
    when(instancePricingStrategyRegistry.getInstancePricingStrategy(InstanceType.ECS_TASK_FARGATE))
        .thenReturn(new EcsFargateInstancePricingStrategy(vmPricingService));
    Resource instanceResource = getInstanceResource(320, 2048);
    Map<String, String> metaData = new HashMap<>();
    metaData.put(InstanceMetaDataConstants.CLOUD_PROVIDER, CloudProvider.AWS.name());
    metaData.put(InstanceMetaDataConstants.REGION, REGION);
    metaData.put(InstanceMetaDataConstants.CLUSTER_TYPE, ClusterType.ECS.name());
    InstanceData instanceData = getInstance(instanceResource, instanceResource, metaData, INSTANCE_START_TIMESTAMP,
        INSTANCE_STOP_TIMESTAMP.minus(12, ChronoUnit.HOURS), InstanceType.ECS_TASK_FARGATE);
    UtilizationData utilizationData = getUtilization(CPU_UTILIZATION, MEMORY_UTILIZATION);
    BillingData billingAmount = billingCalculationService.getInstanceBillingAmount(
        instanceData, utilizationData, INSTANCE_START_TIMESTAMP, INSTANCE_STOP_TIMESTAMP);
    assertThat(billingAmount.getBillingAmountBreakup().getBillingAmount()).isEqualTo(new BigDecimal("19.5"));
    assertThat(billingAmount.getIdleCostData().getIdleCost()).isEqualTo(BigDecimal.ZERO);
    assertThat(billingAmount.getIdleCostData().getMemoryIdleCost()).isEqualTo(BigDecimal.ZERO);
    assertThat(billingAmount.getIdleCostData().getCpuIdleCost()).isEqualTo(BigDecimal.ZERO);
    assertThat(billingAmount.getUsageDurationSeconds()).isEqualTo(HALF_DAY_SECONDS.doubleValue());
    assertThat(billingAmount.getCpuUnitSeconds()).isEqualTo(320 * HALF_DAY_SECONDS);
    assertThat(billingAmount.getMemoryMbSeconds()).isEqualTo(2048 * HALF_DAY_SECONDS);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetInstanceBillingAmountGCPCustomInstance() throws IOException {
    when(instancePricingStrategyRegistry.getInstancePricingStrategy(InstanceType.K8S_POD))
        .thenReturn(getComputeInstancePricingStrategy());
    Resource instanceResource = getInstanceResource(4 * 1024, 5 * 1024);
    Map<String, String> metaData = new HashMap<>();
    metaData.put(InstanceMetaDataConstants.CLOUD_PROVIDER, CloudProvider.GCP.name());
    metaData.put(InstanceMetaDataConstants.INSTANCE_FAMILY, GCP_CUSTOM_INSTANCE_FAMILY);
    metaData.put(InstanceMetaDataConstants.REGION, REGION);
    metaData.put(InstanceMetaDataConstants.INSTANCE_CATEGORY, InstanceCategory.SPOT.name());
    metaData.put(InstanceMetaDataConstants.CLUSTER_TYPE, ClusterType.K8S.name());
    addParentResource(metaData, 8 * 1024, 20 * 1024);
    InstanceData instanceData = getInstance(instanceResource, instanceResource, metaData, INSTANCE_START_TIMESTAMP,
        INSTANCE_STOP_TIMESTAMP.minus(12, ChronoUnit.HOURS), InstanceType.K8S_POD);
    UtilizationData utilizationData = getUtilization(CPU_UTILIZATION, MEMORY_UTILIZATION);
    BillingData billingAmount = billingCalculationService.getInstanceBillingAmount(
        instanceData, utilizationData, INSTANCE_START_TIMESTAMP, INSTANCE_STOP_TIMESTAMP);
    assertThat(billingAmount.getBillingAmountBreakup().getBillingAmount())
        .isEqualTo(new BigDecimal("0.3358799999999999625"));
  }

  private ComputeInstancePricingStrategy getComputeInstancePricingStrategy() {
    return new ComputeInstancePricingStrategy(vmPricingService, awsCustomBillingService, instanceResourceService,
        ecsFargateInstancePricingStrategy, customBillingMetaDataService, pricingProfileService);
  }

  @Test
  @Owner(developers = SANDESH)
  @Category(UnitTests.class)
  public void testGetInstanceBillingAmountCustomInstance() throws IOException {
    when(instancePricingStrategyRegistry.getInstancePricingStrategy(InstanceType.K8S_POD))
        .thenReturn(getComputeInstancePricingStrategy());
    Resource instanceResource = getInstanceResource(4 * 1024, 5 * 1024);
    Map<String, String> metaData = new HashMap<>();
    metaData.put(InstanceMetaDataConstants.CLOUD_PROVIDER, CloudProvider.IBM.name());
    metaData.put(InstanceMetaDataConstants.INSTANCE_FAMILY, "b2c.4x16.encrypted");
    metaData.put(InstanceMetaDataConstants.REGION, "eu-de");
    metaData.put(InstanceMetaDataConstants.INSTANCE_CATEGORY, InstanceCategory.ON_DEMAND.name());
    metaData.put(InstanceMetaDataConstants.CLUSTER_TYPE, ClusterType.K8S.name());
    addParentResource(metaData, 8 * 1024, 20 * 1024);
    InstanceData instanceData = getInstance(instanceResource, instanceResource, metaData, INSTANCE_START_TIMESTAMP,
        INSTANCE_STOP_TIMESTAMP.minus(12, ChronoUnit.HOURS), InstanceType.K8S_POD);
    when(pricingProfileService.fetchPricingProfile(instanceData.getAccountId()))
        .thenReturn(PricingProfile.builder()
                        .accountId(instanceData.getAccountId())
                        .vCpuPricePerHr(0.016)
                        .memoryGbPricePerHr(0.008)
                        .build());
    UtilizationData utilizationData = getUtilization(1, 1);
    BillingData billingAmount = billingCalculationService.getInstanceBillingAmount(
        instanceData, utilizationData, INSTANCE_START_TIMESTAMP, INSTANCE_STOP_TIMESTAMP);
    assertThat(billingAmount.getBillingAmountBreakup().getBillingAmount())
        .isEqualTo(new BigDecimal("1.2960000000000001500"));
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetInstanceBillingAmountIBMInstance() throws IOException {
    when(instancePricingStrategyRegistry.getInstancePricingStrategy(InstanceType.K8S_POD))
        .thenReturn(getComputeInstancePricingStrategy());
    Resource instanceResource = getInstanceResource(4 * 1024, 5 * 1024);
    Map<String, String> metaData = new HashMap<>();
    metaData.put(InstanceMetaDataConstants.CLOUD_PROVIDER, CloudProvider.IBM.name());
    metaData.put(InstanceMetaDataConstants.INSTANCE_FAMILY, "b2c.4x16.encrypted");
    metaData.put(InstanceMetaDataConstants.REGION, "eu-de");
    metaData.put(InstanceMetaDataConstants.INSTANCE_CATEGORY, InstanceCategory.ON_DEMAND.name());
    metaData.put(InstanceMetaDataConstants.CLUSTER_TYPE, ClusterType.K8S.name());
    addParentResource(metaData, 8 * 1024, 20 * 1024);
    InstanceData instanceData = getInstance(instanceResource, instanceResource, metaData, INSTANCE_START_TIMESTAMP,
        INSTANCE_STOP_TIMESTAMP.minus(12, ChronoUnit.HOURS), InstanceType.K8S_POD);
    UtilizationData utilizationData = getUtilization(1, 1);
    when(pricingProfileService.fetchPricingProfile(instanceData.getAccountId()))
        .thenReturn(PricingProfile.builder()
                        .accountId(instanceData.getAccountId())
                        .vCpuPricePerHr(0.016)
                        .memoryGbPricePerHr(0.008)
                        .build());
    BillingData billingAmount = billingCalculationService.getInstanceBillingAmount(
        instanceData, utilizationData, INSTANCE_START_TIMESTAMP, INSTANCE_STOP_TIMESTAMP);
    assertThat(billingAmount.getBillingAmountBreakup().getBillingAmount())
        .isEqualTo(new BigDecimal("1.2960000000000001500"));
  }

  private InstanceData getInstanceWithTime(Instant startInstant, Instant endInstant) {
    return InstanceData.builder().usageStartTime(startInstant).usageStopTime(endInstant).build();
  }

  private InstanceData getInstance(Resource totalResource, Resource instanceResource, Map<String, String> metaData,
      Instant startInstant, Instant endInstant, InstanceType instanceType) {
    return InstanceData.builder()
        .accountId(ACCOUNT_ID)
        .totalResource(totalResource)
        .allocatableResource(instanceResource)
        .metaData(metaData)
        .usageStartTime(startInstant)
        .usageStopTime(endInstant)
        .instanceType(instanceType)
        .build();
  }

  private UtilizationData getUtilization(double cpuUtilization, double memoryUtilization) {
    return UtilizationData.builder()
        .maxCpuUtilization(cpuUtilization)
        .maxMemoryUtilization(memoryUtilization)
        .avgCpuUtilization(cpuUtilization)
        .avgMemoryUtilization(memoryUtilization)
        .build();
  }

  private UtilizationData getUtilizationWithValues(
      double cpuUtilization, double memoryUtilization, double cpuUtilizationValue, double memoryUtilizationValue) {
    return UtilizationData.builder()
        .maxCpuUtilization(cpuUtilization)
        .maxMemoryUtilization(memoryUtilization)
        .avgCpuUtilization(cpuUtilization)
        .avgMemoryUtilization(memoryUtilization)
        .maxCpuUtilizationValue(cpuUtilizationValue)
        .maxMemoryUtilizationValue(memoryUtilizationValue)
        .avgCpuUtilizationValue(cpuUtilizationValue)
        .avgMemoryUtilizationValue(memoryUtilizationValue)
        .build();
  }

  private Resource getResource() {
    return Resource.builder().cpuUnits(DEFAULT_INSTANCE_CPU).memoryMb(DEFAULT_INSTANCE_MEMORY).build();
  }

  private VMComputePricingInfo createVMComputePricingInfo() {
    return VMComputePricingInfo.builder()
        .cpusPerVm(DEFAULT_INSTANCE_CPU)
        .memPerVm(DEFAULT_INSTANCE_MEMORY)
        .onDemandPrice(DEFAULT_INSTANCE_PRICE)
        .type(DEFAULT_INSTANCE_FAMILY)
        .spotPrice(createZonePrice())
        .build();
  }

  private List<ZonePrice> createZonePrice() {
    List<ZonePrice> zonePrices = new ArrayList<>();
    zonePrices.add(new ZonePrice(GCP_ZONE_1, 0.4));
    zonePrices.add(new ZonePrice(GCP_ZONE_2, 0.5));
    return zonePrices;
  }

  private EcsFargatePricingInfo createEcsFargatePricingInfo() {
    return EcsFargatePricingInfo.builder().cpuPrice(2).memoryPrice(0.5).build();
  }

  private Resource getInstanceResource(double cpu, double memory) {
    return Resource.builder().cpuUnits(cpu).memoryMb(memory).build();
  }

  private void addParentResource(Map<String, String> metaData, double parentCpu, double parentMemory) {
    metaData.put(InstanceMetaDataConstants.PARENT_RESOURCE_CPU, String.valueOf(parentCpu));
    metaData.put(InstanceMetaDataConstants.PARENT_RESOURCE_MEMORY, String.valueOf(parentMemory));
  }
}
