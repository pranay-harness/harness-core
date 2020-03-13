package io.harness.batch.processing.billing.service;

import io.harness.batch.processing.billing.service.intfc.InstancePricingStrategy;
import io.harness.batch.processing.ccm.ClusterType;
import io.harness.batch.processing.ccm.CostAttribution;
import io.harness.batch.processing.ccm.InstanceType;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.writer.constants.InstanceMetaDataConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Service
@Slf4j
public class BillingCalculationService {
  private final InstancePricingStrategyContext instancePricingStrategyContext;

  @Autowired
  public BillingCalculationService(InstancePricingStrategyContext instancePricingStrategyContext) {
    this.instancePricingStrategyContext = instancePricingStrategyContext;
  }

  public BillingData getInstanceBillingAmount(
      InstanceData instanceData, UtilizationData utilizationData, Instant startTime, Instant endTime) {
    double instanceActiveSeconds = getInstanceActiveSeconds(instanceData, startTime, endTime);
    InstancePricingStrategy instancePricingStrategy =
        instancePricingStrategyContext.getInstancePricingStrategy(instanceData.getInstanceType());

    PricingData pricingData = instancePricingStrategy.getPricePerHour(instanceData, startTime);
    return getBillingAmount(instanceData, utilizationData, pricingData, instanceActiveSeconds);
  }

  BillingData getBillingAmount(InstanceData instanceData, UtilizationData utilizationData, PricingData pricingData,
      double instanceActiveSeconds) {
    double pricePerHour = pricingData.getPricePerHour();
    BigDecimal billingAmount = BigDecimal.valueOf((pricePerHour * instanceActiveSeconds) / 3600);
    Double cpuUnit;
    Double memoryMb;
    if (null != instanceData.getTotalResource()) {
      cpuUnit = instanceData.getTotalResource().getCpuUnits();
      memoryMb = instanceData.getTotalResource().getMemoryMb();
      if (instanceData.getMetaData().get(InstanceMetaDataConstants.CLUSTER_TYPE).equals(ClusterType.K8S.name())) {
        if (utilizationData.getAvgCpuUtilizationValue() > cpuUnit) {
          cpuUnit = utilizationData.getAvgCpuUtilizationValue();
        }
        if (utilizationData.getAvgMemoryUtilizationValue() > memoryMb) {
          memoryMb = utilizationData.getAvgMemoryUtilizationValue();
        }
      }
    } else {
      cpuUnit = pricingData.getCpuUnit();
      memoryMb = pricingData.getMemoryMb();
    }
    logger.debug("Billing amount {} {} {}", billingAmount, pricePerHour, instanceActiveSeconds);
    BillingAmountBreakup billingAmountForResource =
        getBillingAmountForResource(instanceData, billingAmount, cpuUnit, memoryMb);
    return new BillingData(billingAmountForResource,
        getIdleCostForResource(billingAmountForResource, utilizationData, instanceData),
        getSystemCostForResource(billingAmountForResource, instanceData), instanceActiveSeconds,
        cpuUnit * instanceActiveSeconds, memoryMb * instanceActiveSeconds);
  }

  BillingAmountBreakup getBillingAmountForResource(
      InstanceData instanceData, BigDecimal billingAmount, double instanceCpu, double instanceMemory) {
    if (instanceData.getInstanceType().getCostAttribution() == CostAttribution.PARTIAL) {
      Map<String, String> instanceMetaData = instanceData.getMetaData();
      Double parentInstanceCpu = Double.valueOf(instanceMetaData.get(InstanceMetaDataConstants.PARENT_RESOURCE_CPU));
      Double parentInstanceMemory =
          Double.valueOf(instanceMetaData.get(InstanceMetaDataConstants.PARENT_RESOURCE_MEMORY));

      BigDecimal instanceUsage =
          BigDecimal.valueOf(((instanceCpu / parentInstanceCpu) + (instanceMemory / parentInstanceMemory)) * 0.5);
      return BillingAmountBreakup.builder()
          .billingAmount(instanceUsage.multiply(billingAmount))
          .cpuBillingAmount(billingAmount.multiply(BigDecimal.valueOf((instanceCpu / parentInstanceCpu) * 0.5)))
          .memoryBillingAmount(
              billingAmount.multiply(BigDecimal.valueOf((instanceMemory / parentInstanceMemory) * 0.5)))
          .build();
    }
    return BillingAmountBreakup.builder()
        .billingAmount(billingAmount)
        .cpuBillingAmount(billingAmount.multiply(BigDecimal.valueOf(0.5)))
        .memoryBillingAmount(billingAmount.multiply(BigDecimal.valueOf(0.5)))
        .build();
  }

  SystemCostData getSystemCostForResource(BillingAmountBreakup billingDataForResource, InstanceData instanceData) {
    BigDecimal cpuSystemCost = BigDecimal.ZERO;
    BigDecimal memorySystemCost = BigDecimal.ZERO;
    BigDecimal systemCost = BigDecimal.ZERO;
    if (instanceData.getAllocatableResource() != null && instanceData.getTotalResource() != null) {
      BigDecimal cpuBillingAmount = billingDataForResource.getCpuBillingAmount();
      BigDecimal memoryBillingAmount = billingDataForResource.getMemoryBillingAmount();
      if (instanceData.getTotalResource().getCpuUnits() > 0) {
        cpuSystemCost = BigDecimal.valueOf(cpuBillingAmount.doubleValue()
            * (1
                  - (instanceData.getAllocatableResource().getCpuUnits()
                        / instanceData.getTotalResource().getCpuUnits())));
      }
      if (instanceData.getTotalResource().getMemoryMb() > 0) {
        memorySystemCost = BigDecimal.valueOf(memoryBillingAmount.doubleValue()
            * (1
                  - (instanceData.getAllocatableResource().getMemoryMb()
                        / instanceData.getTotalResource().getMemoryMb())));
      }
      systemCost = cpuSystemCost.add(memorySystemCost);
    }
    return new SystemCostData(systemCost, cpuSystemCost, memorySystemCost);
  }

  IdleCostData getIdleCostForResource(
      BillingAmountBreakup billingDataForResource, UtilizationData utilizationData, InstanceData instanceData) {
    if (instanceData.getInstanceType() == InstanceType.ECS_TASK_FARGATE || utilizationData == null) {
      return new IdleCostData(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }
    BigDecimal cpuIdleCost = BigDecimal.ZERO;
    BigDecimal memoryIdleCost = BigDecimal.ZERO;
    if (utilizationData.getAvgCpuUtilization() < 1) {
      cpuIdleCost = BigDecimal.valueOf(
          billingDataForResource.getCpuBillingAmount().doubleValue() * (1 - utilizationData.getAvgCpuUtilization()));
    }
    if (utilizationData.getAvgMemoryUtilization() < 1) {
      memoryIdleCost = BigDecimal.valueOf(billingDataForResource.getMemoryBillingAmount().doubleValue()
          * (1 - utilizationData.getAvgMemoryUtilization()));
    }
    BigDecimal idleCost = BigDecimal.valueOf(cpuIdleCost.doubleValue() + memoryIdleCost.doubleValue());
    return new IdleCostData(idleCost, cpuIdleCost, memoryIdleCost);
  }

  double getInstanceActiveSeconds(InstanceData instanceData, Instant startTime, Instant endTime) {
    if ((null != instanceData.getUsageStartTime() && instanceData.getUsageStartTime().isAfter(endTime))
        || (null != instanceData.getUsageStopTime() && instanceData.getUsageStopTime().isBefore(startTime))) {
      return 0;
    }
    Long instanceActiveSeconds = getActiveInstanceTimeInInterval(instanceData, startTime, endTime);
    double minChargeableSeconds = instanceData.getInstanceType().getMinChargeableSeconds();
    if (null == instanceData.getUsageStopTime() || instanceActiveSeconds > minChargeableSeconds) {
      return instanceActiveSeconds;
    } else {
      double totalInstanceActiveSeconds = getTotalActiveInstanceTimeInterval(instanceData);
      if (totalInstanceActiveSeconds > minChargeableSeconds) {
        return instanceActiveSeconds;
      } else {
        return minChargeableSeconds - totalInstanceActiveSeconds + instanceActiveSeconds;
      }
    }
  }

  private Long getTotalActiveInstanceTimeInterval(InstanceData instanceData) {
    return instanceData.getUsageStopTime().getEpochSecond() - instanceData.getUsageStartTime().getEpochSecond();
  }

  Long getActiveInstanceTimeInInterval(InstanceData instanceData, Instant startTime, Instant endTime) {
    Instant activeStartTime = startTime;
    if (instanceData.getUsageStartTime().isAfter(startTime)) {
      activeStartTime = instanceData.getUsageStartTime();
    }

    Instant activeEndTime = endTime;
    if (null != instanceData.getUsageStopTime() && instanceData.getUsageStopTime().isBefore(endTime)) {
      activeEndTime = instanceData.getUsageStopTime();
    }
    return activeEndTime.getEpochSecond() - activeStartTime.getEpochSecond();
  }
}
