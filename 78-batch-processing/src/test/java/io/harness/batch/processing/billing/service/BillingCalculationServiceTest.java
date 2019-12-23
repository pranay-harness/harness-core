package io.harness.batch.processing.billing.service;

import static io.harness.rule.OwnerRule.HITESH;
import static io.harness.rule.OwnerRule.SHUBHANSHU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.batch.processing.billing.service.impl.ComputeInstancePricingStrategy;
import io.harness.batch.processing.billing.service.impl.EcsFargateInstancePricingStrategy;
import io.harness.batch.processing.ccm.InstanceType;
import io.harness.batch.processing.ccm.Resource;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.pricing.data.CloudProvider;
import io.harness.batch.processing.pricing.data.EcsFargatePricingInfo;
import io.harness.batch.processing.pricing.data.VMComputePricingInfo;
import io.harness.batch.processing.pricing.service.impl.VMPricingServiceImpl;
import io.harness.batch.processing.pricing.service.intfc.AwsCustomPricingService;
import io.harness.batch.processing.writer.constants.InstanceMetaDataConstants;
import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class BillingCalculationServiceTest extends CategoryTest {
  @InjectMocks private BillingCalculationService billingCalculationService;
  @Mock private InstancePricingStrategyContext instancePricingStrategyRegistry;
  @Mock private VMPricingServiceImpl vmPricingService;
  @Mock private AwsCustomPricingService awsCustomPricingService;

  private final Instant NOW = Instant.now().truncatedTo(ChronoUnit.DAYS);
  private final Instant INSTANCE_STOP_TIMESTAMP = NOW;
  private final Instant INSTANCE_START_TIMESTAMP = NOW.minus(1, ChronoUnit.DAYS);
  private final Long ONE_DAY_SECONDS = 86400l;
  private final Long HALF_DAY_SECONDS = 43200l;

  private final String REGION = "us-east-1";
  private final String DEFAULT_INSTANCE_FAMILY = "c4.8xlarge";
  private final double DEFAULT_INSTANCE_CPU = 36;
  private final double DEFAULT_INSTANCE_MEMORY = 60;
  private final double DEFAULT_INSTANCE_PRICE = 1.60;
  private final double CPU_UTILIZATION = 0.5;
  private final double MEMORY_UTILIZATION = 0.5;

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
    InstanceData instanceData = getInstance(
        instanceResource, metaData, INSTANCE_START_TIMESTAMP, INSTANCE_STOP_TIMESTAMP, InstanceType.ECS_TASK_EC2);
    BigDecimal billingAmountForResource =
        billingCalculationService.getBillingAmountForResource(instanceData, new BigDecimal(200));
    assertThat(billingAmountForResource).isEqualTo(new BigDecimal("75.000"));
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testGetIdleCostForResource() {
    Resource instanceResource = getInstanceResource(256, 512);
    Map<String, String> metaData = new HashMap<>();
    addParentResource(metaData, 1024, 1024);
    InstanceData instanceData = getInstance(
        instanceResource, metaData, INSTANCE_START_TIMESTAMP, INSTANCE_STOP_TIMESTAMP, InstanceType.ECS_TASK_EC2);
    BigDecimal billingAmount = new BigDecimal(200);
    UtilizationData utilizationData = getUtilization(CPU_UTILIZATION, MEMORY_UTILIZATION);
    IdleCostData idleCost =
        billingCalculationService.getIdleCostForResource(billingAmount, utilizationData, instanceData);
    assertThat(idleCost.getIdleCost()).isEqualTo(new BigDecimal("100.0"));
    assertThat(idleCost.getMemoryIdleCost()).isEqualTo(new BigDecimal("50.0"));
    assertThat(idleCost.getCpuIdleCost()).isEqualTo(new BigDecimal("50.0"));
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetBillingAmount() {
    PricingData pricingData = new PricingData(10, 256.0, 512.0);
    Resource instanceResource = getInstanceResource(256, 512);
    Map<String, String> metaData = new HashMap<>();
    addParentResource(metaData, 1024, 1024);
    InstanceData instanceData = getInstance(
        instanceResource, metaData, INSTANCE_START_TIMESTAMP, INSTANCE_STOP_TIMESTAMP, InstanceType.ECS_TASK_EC2);
    UtilizationData utilizationData = getUtilization(CPU_UTILIZATION, MEMORY_UTILIZATION);
    BillingData billingAmount =
        billingCalculationService.getBillingAmount(instanceData, utilizationData, pricingData, ONE_DAY_SECONDS);
    assertThat(billingAmount.getBillingAmount()).isEqualTo(new BigDecimal("90.0000"));
    assertThat(billingAmount.getIdleCostData().getIdleCost()).isEqualTo(new BigDecimal("45.0"));
    assertThat(billingAmount.getIdleCostData().getCpuIdleCost()).isEqualTo(new BigDecimal("22.5"));
    assertThat(billingAmount.getIdleCostData().getMemoryIdleCost()).isEqualTo(new BigDecimal("22.5"));
    assertThat(billingAmount.getUsageDurationSeconds()).isEqualTo(ONE_DAY_SECONDS.doubleValue());
    assertThat(billingAmount.getCpuUnitSeconds()).isEqualTo(256.0 * ONE_DAY_SECONDS);
    assertThat(billingAmount.getMemoryMbSeconds()).isEqualTo(512.0 * ONE_DAY_SECONDS);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetBillingAmountWhereResourceIsNotPresent() {
    PricingData pricingData = new PricingData(10, 256.0, 512.0);
    Map<String, String> metaData = new HashMap<>();
    InstanceData instanceData = getInstance(null, metaData, INSTANCE_START_TIMESTAMP,
        INSTANCE_STOP_TIMESTAMP.minus(12, ChronoUnit.HOURS), InstanceType.EC2_INSTANCE);
    UtilizationData utilizationData = getUtilization(CPU_UTILIZATION, MEMORY_UTILIZATION);
    BillingData billingAmount =
        billingCalculationService.getBillingAmount(instanceData, utilizationData, pricingData, HALF_DAY_SECONDS);
    assertThat(billingAmount.getBillingAmount()).isEqualTo(new BigDecimal("120.0"));
    assertThat(billingAmount.getIdleCostData().getIdleCost()).isEqualTo(new BigDecimal("60.0"));
    assertThat(billingAmount.getIdleCostData().getCpuIdleCost()).isEqualTo(new BigDecimal("30.0"));
    assertThat(billingAmount.getIdleCostData().getMemoryIdleCost()).isEqualTo(new BigDecimal("30.0"));
    assertThat(billingAmount.getUsageDurationSeconds()).isEqualTo(HALF_DAY_SECONDS.doubleValue());
    assertThat(billingAmount.getCpuUnitSeconds()).isEqualTo(256.0 * HALF_DAY_SECONDS);
    assertThat(billingAmount.getMemoryMbSeconds()).isEqualTo(512.0 * HALF_DAY_SECONDS);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetInstanceBillingAmountForCompute() throws IOException {
    when(vmPricingService.getComputeVMPricingInfo(DEFAULT_INSTANCE_FAMILY, REGION, CloudProvider.AWS))
        .thenReturn(createVMComputePricingInfo());
    when(instancePricingStrategyRegistry.getInstancePricingStrategy(InstanceType.ECS_TASK_EC2))
        .thenReturn(new ComputeInstancePricingStrategy(vmPricingService, awsCustomPricingService));
    Resource instanceResource = getInstanceResource(18432, 30720);
    Map<String, String> metaData = new HashMap<>();
    metaData.put(InstanceMetaDataConstants.CLOUD_PROVIDER, CloudProvider.AWS.name());
    metaData.put(InstanceMetaDataConstants.INSTANCE_FAMILY, DEFAULT_INSTANCE_FAMILY);
    metaData.put(InstanceMetaDataConstants.REGION, REGION);
    addParentResource(metaData, DEFAULT_INSTANCE_CPU * 1024, DEFAULT_INSTANCE_MEMORY * 1024);
    InstanceData instanceData = getInstance(instanceResource, metaData, INSTANCE_START_TIMESTAMP,
        INSTANCE_STOP_TIMESTAMP.minus(12, ChronoUnit.HOURS), InstanceType.ECS_TASK_EC2);
    UtilizationData utilizationData = getUtilization(CPU_UTILIZATION, MEMORY_UTILIZATION);
    BillingData billingAmount = billingCalculationService.getInstanceBillingAmount(
        instanceData, utilizationData, INSTANCE_START_TIMESTAMP, INSTANCE_STOP_TIMESTAMP);
    assertThat(billingAmount.getBillingAmount()).isEqualTo(new BigDecimal("9.60"));
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
  public void testGetInstanceBillingAmountForFargate() throws IOException {
    when(vmPricingService.getFargatePricingInfo(REGION)).thenReturn(createEcsFargatePricingInfo());
    when(instancePricingStrategyRegistry.getInstancePricingStrategy(InstanceType.ECS_TASK_FARGATE))
        .thenReturn(new EcsFargateInstancePricingStrategy(vmPricingService, awsCustomPricingService));
    Resource instanceResource = getInstanceResource(320, 2048);
    Map<String, String> metaData = new HashMap<>();
    metaData.put(InstanceMetaDataConstants.CLOUD_PROVIDER, CloudProvider.AWS.name());
    metaData.put(InstanceMetaDataConstants.REGION, REGION);
    InstanceData instanceData = getInstance(instanceResource, metaData, INSTANCE_START_TIMESTAMP,
        INSTANCE_STOP_TIMESTAMP.minus(12, ChronoUnit.HOURS), InstanceType.ECS_TASK_FARGATE);
    UtilizationData utilizationData = getUtilization(CPU_UTILIZATION, MEMORY_UTILIZATION);
    BillingData billingAmount = billingCalculationService.getInstanceBillingAmount(
        instanceData, utilizationData, INSTANCE_START_TIMESTAMP, INSTANCE_STOP_TIMESTAMP);
    assertThat(billingAmount.getBillingAmount()).isEqualTo(new BigDecimal("19.5"));
    assertThat(billingAmount.getIdleCostData().getIdleCost()).isEqualTo(BigDecimal.ZERO);
    assertThat(billingAmount.getIdleCostData().getMemoryIdleCost()).isEqualTo(BigDecimal.ZERO);
    assertThat(billingAmount.getIdleCostData().getCpuIdleCost()).isEqualTo(BigDecimal.ZERO);
    assertThat(billingAmount.getUsageDurationSeconds()).isEqualTo(HALF_DAY_SECONDS.doubleValue());
    assertThat(billingAmount.getCpuUnitSeconds()).isEqualTo(320 * HALF_DAY_SECONDS);
    assertThat(billingAmount.getMemoryMbSeconds()).isEqualTo(2048 * HALF_DAY_SECONDS);
  }

  private InstanceData getInstanceWithTime(Instant startInstant, Instant endInstant) {
    return InstanceData.builder().usageStartTime(startInstant).usageStopTime(endInstant).build();
  }

  private InstanceData getInstance(Resource instanceResource, Map<String, String> metaData, Instant startInstant,
      Instant endInstant, InstanceType instanceType) {
    return InstanceData.builder()
        .totalResource(instanceResource)
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
        .avgMemoryUtilization(cpuUtilization)
        .avgMemoryUtilization(memoryUtilization)
        .build();
  }

  private VMComputePricingInfo createVMComputePricingInfo() {
    return VMComputePricingInfo.builder()
        .cpusPerVm(DEFAULT_INSTANCE_CPU)
        .memPerVm(DEFAULT_INSTANCE_MEMORY)
        .onDemandPrice(DEFAULT_INSTANCE_PRICE)
        .type(DEFAULT_INSTANCE_FAMILY)
        .build();
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
