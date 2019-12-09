package io.harness.batch.processing.billing.service;

import io.harness.batch.processing.billing.service.intfc.InstancePricingStrategy;
import io.harness.batch.processing.ccm.CostAttribution;
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
    PricingData pricingData = instancePricingStrategy.getPricePerHour(instanceData);
    return getBillingAmount(instanceData, utilizationData, pricingData, instanceActiveSeconds);
  }

  BillingData getBillingAmount(InstanceData instanceData, UtilizationData utilizationData, PricingData pricingData,
      double instanceActiveSeconds) {
    double pricePerHour = pricingData.getPricePerHour();
    BigDecimal billingAmount = BigDecimal.valueOf((pricePerHour * instanceActiveSeconds) / 3600);
    double cpuUnit = 0;
    double memoryMb = 0;
    if (null != instanceData.getTotalResource()) {
      cpuUnit = instanceData.getTotalResource().getCpuUnits();
      memoryMb = instanceData.getTotalResource().getMemoryMb();
    } else {
      cpuUnit = pricingData.getCpuUnit();
      memoryMb = pricingData.getMemoryMb();
    }
    logger.info("Billing amount {} {} {}", billingAmount, pricePerHour, instanceActiveSeconds);
    BigDecimal billingAmountForResource = getBillingAmountForResource(instanceData, billingAmount);
    return new BillingData(billingAmountForResource,
        getIdleCostForResource(billingAmountForResource, utilizationData, instanceData), instanceActiveSeconds,
        cpuUnit * instanceActiveSeconds, memoryMb * instanceActiveSeconds);
  }

  BigDecimal getBillingAmountForResource(InstanceData instanceData, BigDecimal billingAmount) {
    if (instanceData.getInstanceType().getCostAttribution() == CostAttribution.PARTIAL) {
      Double instanceCpu = instanceData.getTotalResource().getCpuUnits();
      Double instanceMemory = instanceData.getTotalResource().getMemoryMb();
      Map<String, String> instanceMetaData = instanceData.getMetaData();
      Double parentInstanceCpu = Double.valueOf(instanceMetaData.get(InstanceMetaDataConstants.PARENT_RESOURCE_CPU));
      Double parentInstanceMemory =
          Double.valueOf(instanceMetaData.get(InstanceMetaDataConstants.PARENT_RESOURCE_MEMORY));

      BigDecimal instanceUsage =
          BigDecimal.valueOf(((instanceCpu / parentInstanceCpu) + (instanceMemory / parentInstanceMemory)) * 0.5);
      return instanceUsage.multiply(billingAmount);
    }
    return billingAmount;
  }

  IdleCostData getIdleCostForResource(
      BigDecimal billingDataForResource, UtilizationData utilizationData, InstanceData instanceData) {
    if (instanceData.getInstanceType().toString().equals("ECS_TASK_FARGATE") || utilizationData == null) {
      return new IdleCostData(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }
    Double billingAmount = billingDataForResource.doubleValue();
    BigDecimal cpuIdleCost = BigDecimal.valueOf(billingAmount * ((1 - utilizationData.getCpuUtilization()) / 2));
    BigDecimal memoryIdleCost = BigDecimal.valueOf(billingAmount * ((1 - utilizationData.getMemoryUtilization()) / 2));
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
