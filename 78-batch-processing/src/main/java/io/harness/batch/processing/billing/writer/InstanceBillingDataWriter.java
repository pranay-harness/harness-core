package io.harness.batch.processing.billing.writer;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;

import io.harness.batch.processing.billing.service.BillingCalculationService;
import io.harness.batch.processing.billing.service.BillingData;
import io.harness.batch.processing.billing.service.UtilizationData;
import io.harness.batch.processing.billing.timeseries.data.InstanceBillingData;
import io.harness.batch.processing.billing.timeseries.service.impl.BillingDataServiceImpl;
import io.harness.batch.processing.billing.timeseries.service.impl.UtilizationDataServiceImpl;
import io.harness.batch.processing.billing.writer.support.BillingDataGenerationValidator;
import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.ccm.InstanceType;
import io.harness.batch.processing.ccm.Resource;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.service.intfc.InstanceDataService;
import io.harness.batch.processing.writer.constants.InstanceMetaDataConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import software.wings.beans.instance.HarnessServiceInfo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class InstanceBillingDataWriter implements ItemWriter<InstanceData> {
  @Autowired private BillingCalculationService billingCalculationService;
  @Autowired private BillingDataServiceImpl billingDataService;
  @Autowired private UtilizationDataServiceImpl utilizationDataService;
  @Autowired private BillingDataGenerationValidator billingDataGenerationValidator;
  @Autowired private InstanceDataService instanceDataService;

  private JobParameters parameters;

  @BeforeStep
  public void beforeStep(final StepExecution stepExecution) {
    parameters = stepExecution.getJobExecution().getJobParameters();
  }

  @Override
  public void write(List<? extends InstanceData> instanceDataLists) throws Exception {
    Instant startTime = getFieldValueFromJobParams(CCMJobConstants.JOB_START_DATE);
    Instant endTime = getFieldValueFromJobParams(CCMJobConstants.JOB_END_DATE);
    BatchJobType batchJobType =
        CCMJobConstants.getBatchJobTypeFromJobParams(parameters, CCMJobConstants.BATCH_JOB_TYPE);
    logger.info("Instance data list {} {} {} {}", instanceDataLists.size(), startTime, endTime, parameters.toString());

    Map<String, ? extends List<? extends InstanceData>> instanceDataGroupedCluster =
        instanceDataLists.stream().collect(Collectors.groupingBy(InstanceData::getClusterId));

    instanceDataGroupedCluster.forEach((clusterRecordId, instanceDataList) -> {
      InstanceData firstInstanceData = instanceDataList.get(0);
      Map<String, UtilizationData> utilizationDataForInstances = utilizationDataService.getUtilizationDataForInstances(
          instanceDataList, startTime.toString(), endTime.toString(), firstInstanceData.getAccountId(),
          firstInstanceData.getSettingId(), firstInstanceData.getClusterId());

      instanceDataList.stream()
          .filter(instanceData
              -> billingDataGenerationValidator.shouldGenerateBillingData(
                  instanceData.getAccountId(), instanceData.getClusterId(), startTime))
          .forEach(instanceData -> {
            UtilizationData utilizationData = utilizationDataForInstances.get(instanceData.getInstanceId());
            BillingData billingData =
                billingCalculationService.getInstanceBillingAmount(instanceData, utilizationData, startTime, endTime);
            logger.trace(
                "Instance detail {} :: {} ", instanceData.getInstanceId(), billingData.getBillingAmountBreakup());
            HarnessServiceInfo harnessServiceInfo = getHarnessServiceInfo(instanceData);
            String settingId =
                (instanceData.getInstanceType() == InstanceType.EC2_INSTANCE) ? null : instanceData.getSettingId();
            String clusterId =
                (instanceData.getInstanceType() == InstanceType.EC2_INSTANCE) ? null : instanceData.getClusterId();
            String instanceName = (instanceData.getInstanceName() == null) ? instanceData.getInstanceId()
                                                                           : instanceData.getInstanceName();

            Resource totalResource = instanceData.getTotalResource();
            Resource limitResource = Resource.builder().cpuUnits(0.0).memoryMb(0.0).build();
            if (null != instanceData.getLimitResource()) {
              limitResource = instanceData.getLimitResource();
            }

            InstanceBillingData instanceBillingData =
                InstanceBillingData.builder()
                    .accountId(instanceData.getAccountId())
                    .settingId(settingId)
                    .clusterId(clusterId)
                    .instanceType(instanceData.getInstanceType().toString())
                    .billingAccountId("BILLING_ACCOUNT_ID")
                    .startTimestamp(startTime.toEpochMilli())
                    .endTimestamp(endTime.toEpochMilli())
                    .billingAmount(billingData.getBillingAmountBreakup().getBillingAmount())
                    .cpuBillingAmount(billingData.getBillingAmountBreakup().getCpuBillingAmount())
                    .memoryBillingAmount(billingData.getBillingAmountBreakup().getMemoryBillingAmount())
                    .systemCost(billingData.getSystemCostData().getSystemCost())
                    .cpuSystemCost(billingData.getSystemCostData().getCpuSystemCost())
                    .memorySystemCost(billingData.getSystemCostData().getMemorySystemCost())
                    .idleCost(billingData.getIdleCostData().getIdleCost())
                    .cpuIdleCost(billingData.getIdleCostData().getCpuIdleCost())
                    .memoryIdleCost(billingData.getIdleCostData().getMemoryIdleCost())
                    .usageDurationSeconds(billingData.getUsageDurationSeconds())
                    .instanceId(instanceData.getInstanceId())
                    .instanceName(instanceName)
                    .clusterName(instanceData.getClusterName())
                    .appId(harnessServiceInfo.getAppId())
                    .serviceId(harnessServiceInfo.getServiceId())
                    .cloudProviderId(harnessServiceInfo.getCloudProviderId())
                    .envId(harnessServiceInfo.getEnvId())
                    .cpuUnitSeconds(billingData.getCpuUnitSeconds())
                    .memoryMbSeconds(billingData.getMemoryMbSeconds())
                    .parentInstanceId(getParentInstanceId(instanceData))
                    .launchType(getValueForKeyFromInstanceMetaData(InstanceMetaDataConstants.LAUNCH_TYPE, instanceData))
                    .taskId(getValueForKeyFromInstanceMetaData(InstanceMetaDataConstants.TASK_ID, instanceData))
                    .namespace(getValueForKeyFromInstanceMetaData(InstanceMetaDataConstants.NAMESPACE, instanceData))
                    .region(getValueForKeyFromInstanceMetaData(InstanceMetaDataConstants.REGION, instanceData))
                    .clusterType(
                        getValueForKeyFromInstanceMetaData(InstanceMetaDataConstants.CLUSTER_TYPE, instanceData))
                    .cloudProvider(
                        getValueForKeyFromInstanceMetaData(InstanceMetaDataConstants.CLOUD_PROVIDER, instanceData))
                    .workloadName(
                        getValueForKeyFromInstanceMetaData(InstanceMetaDataConstants.WORKLOAD_NAME, instanceData))
                    .workloadType(
                        getValueForKeyFromInstanceMetaData(InstanceMetaDataConstants.WORKLOAD_TYPE, instanceData))
                    .cloudServiceName(getCloudServiceName(instanceData))
                    .maxCpuUtilization(utilizationData.getMaxCpuUtilization())
                    .maxMemoryUtilization(utilizationData.getMaxMemoryUtilization())
                    .avgCpuUtilization(utilizationData.getAvgCpuUtilization())
                    .avgMemoryUtilization(utilizationData.getAvgMemoryUtilization())
                    .cpuRequest(totalResource.getCpuUnits())
                    .memoryRequest(totalResource.getMemoryMb())
                    .cpuLimit(limitResource.getCpuUnits())
                    .memoryLimit(limitResource.getMemoryMb())
                    .maxCpuUtilizationValue(utilizationData.getMaxCpuUtilizationValue())
                    .maxMemoryUtilizationValue(utilizationData.getMaxMemoryUtilizationValue())
                    .avgCpuUtilizationValue(utilizationData.getAvgCpuUtilizationValue())
                    .avgMemoryUtilizationValue(utilizationData.getAvgMemoryUtilizationValue())
                    // Actual idle cost and unallocated cost for node/container will get updated by actualIdleCost job
                    .actualIdleCost(billingData.getIdleCostData().getIdleCost())
                    .cpuActualIdleCost(billingData.getIdleCostData().getCpuIdleCost())
                    .memoryActualIdleCost(billingData.getIdleCostData().getMemoryIdleCost())
                    .unallocatedCost(BigDecimal.ZERO)
                    .cpuUnallocatedCost(BigDecimal.ZERO)
                    .memoryUnallocatedCost(BigDecimal.ZERO)
                    .build();
            billingDataService.create(instanceBillingData, batchJobType);
          });
    });
  }

  String getParentInstanceId(InstanceData instanceData) {
    String actualParentResourceId =
        getValueForKeyFromInstanceMetaData(InstanceMetaDataConstants.ACTUAL_PARENT_RESOURCE_ID, instanceData);
    if (null == actualParentResourceId && InstanceType.K8S_POD == instanceData.getInstanceType()) {
      String parentResourceId =
          getValueForKeyFromInstanceMetaData(InstanceMetaDataConstants.PARENT_RESOURCE_ID, instanceData);
      if (null != parentResourceId) {
        InstanceData parentInstanceData = instanceDataService.fetchInstanceDataWithName(
            instanceData.getAccountId(), instanceData.getClusterId(), parentResourceId, Instant.now().toEpochMilli());
        if (null != parentInstanceData) {
          return parentInstanceData.getInstanceId();
        } else {
          return parentResourceId;
        }
      }
    }
    return actualParentResourceId;
  }

  String getCloudServiceName(InstanceData instanceData) {
    String cloudServiceName =
        getValueForKeyFromInstanceMetaData(InstanceMetaDataConstants.ECS_SERVICE_NAME, instanceData);
    InstanceType instanceType = instanceData.getInstanceType();
    if (null == cloudServiceName
        && ImmutableSet.of(InstanceType.ECS_TASK_FARGATE, InstanceType.ECS_TASK_EC2).contains(instanceType)) {
      cloudServiceName = "none";
    }
    return cloudServiceName;
  }

  HarnessServiceInfo getHarnessServiceInfo(InstanceData instanceData) {
    if (null != instanceData.getHarnessServiceInfo()) {
      return instanceData.getHarnessServiceInfo();
    }
    return new HarnessServiceInfo(null, null, null, null, null, null);
  }

  String getValueForKeyFromInstanceMetaData(String metaDataKey, InstanceData instanceData) {
    if (null != instanceData.getMetaData() && instanceData.getMetaData().containsKey(metaDataKey)) {
      return instanceData.getMetaData().get(metaDataKey);
    }
    return null;
  }

  private Instant getFieldValueFromJobParams(String fieldName) {
    return Instant.ofEpochMilli(Long.parseLong(parameters.getString(fieldName)));
  }
}
