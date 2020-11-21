package io.harness.batch.processing.billing.writer;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.batch.processing.billing.service.BillingCalculationService;
import io.harness.batch.processing.billing.service.BillingData;
import io.harness.batch.processing.billing.service.UtilizationData;
import io.harness.batch.processing.billing.timeseries.data.InstanceBillingData;
import io.harness.batch.processing.billing.timeseries.service.impl.BillingDataServiceImpl;
import io.harness.batch.processing.billing.timeseries.service.impl.UtilizationDataServiceImpl;
import io.harness.batch.processing.billing.writer.support.BillingDataGenerationValidator;
import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.dao.intfc.InstanceDataDao;
import io.harness.batch.processing.pricing.data.CloudProvider;
import io.harness.batch.processing.pricing.service.intfc.AwsCustomBillingService;
import io.harness.batch.processing.service.intfc.CustomBillingMetaDataService;
import io.harness.batch.processing.service.intfc.InstanceDataService;
import io.harness.batch.processing.writer.constants.InstanceMetaDataConstants;
import io.harness.ccm.commons.beans.HarnessServiceInfo;
import io.harness.ccm.commons.beans.InstanceType;
import io.harness.ccm.commons.beans.Resource;
import io.harness.ccm.commons.entities.InstanceData;
import io.harness.persistence.HPersistence;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Singleton
public class InstanceBillingDataTasklet implements Tasklet {
  @Autowired private BillingCalculationService billingCalculationService;
  @Autowired private BillingDataServiceImpl billingDataService;
  @Autowired private UtilizationDataServiceImpl utilizationDataService;
  @Autowired private BillingDataGenerationValidator billingDataGenerationValidator;
  @Autowired private InstanceDataService instanceDataService;
  @Autowired private AwsCustomBillingService awsCustomBillingService;
  @Autowired private CustomBillingMetaDataService customBillingMetaDataService;
  @Autowired private InstanceDataDao instanceDataDao;
  @Autowired private HPersistence persistence;
  @Autowired private BatchMainConfig config;

  private JobParameters parameters;

  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) {
    parameters = chunkContext.getStepContext().getStepExecution().getJobParameters();
    int batchSize = config.getBatchQueryConfig().getInstanceDataBatchSize();
    String accountId = parameters.getString(CCMJobConstants.ACCOUNT_ID);
    Instant startTime = getFieldValueFromJobParams(CCMJobConstants.JOB_START_DATE);
    Instant endTime = getFieldValueFromJobParams(CCMJobConstants.JOB_END_DATE);
    BatchJobType batchJobType =
        CCMJobConstants.getBatchJobTypeFromJobParams(parameters, CCMJobConstants.BATCH_JOB_TYPE);

    // Instant of 1-1-2018
    Instant seekingDate = Instant.ofEpochMilli(1514764800000l);
    List<InstanceData> instanceDataLists;

    do {
      instanceDataLists = instanceDataDao.getInstanceDataLists(accountId, batchSize, startTime, endTime, seekingDate);
      if (!instanceDataLists.isEmpty()) {
        Instant lastUsageStartTime = instanceDataLists.get(instanceDataLists.size() - 1).getUsageStartTime();
        seekingDate = instanceDataLists.get(instanceDataLists.size() - 1).getUsageStartTime();
        if (instanceDataLists.get(0).getUsageStartTime().equals(lastUsageStartTime)) {
          log.info("Incrementing Seeking Date by 1ms {} {} {} {}", instanceDataLists.size(), startTime, endTime,
              parameters.toString());
          seekingDate = seekingDate.plus(1, ChronoUnit.MILLIS);
        }
      }
      try {
        createBillingData(accountId, startTime, endTime, batchJobType, instanceDataLists);
      } catch (Exception ex) {
        log.error("Exception in billing step", ex);
        throw ex;
      }
    } while (instanceDataLists.size() == batchSize);
    return null;
  }

  void createBillingData(String accountId, Instant startTime, Instant endTime, BatchJobType batchJobType,
      List<InstanceData> instanceDataLists) {
    log.info("Instance data list {} {} {} {}", instanceDataLists.size(), startTime, endTime, parameters.toString());

    Map<String, List<InstanceData>> instanceDataGroupedCluster =
        instanceDataLists.stream().collect(Collectors.groupingBy(InstanceData::getClusterId));
    String awsDataSetId = customBillingMetaDataService.getAwsDataSetId(accountId);
    if (awsDataSetId != null) {
      Set<String> resourceIds = new HashSet<>();
      instanceDataLists.forEach(instanceData -> {
        String resourceId =
            getValueForKeyFromInstanceMetaData(InstanceMetaDataConstants.CLOUD_PROVIDER_INSTANCE_ID, instanceData);
        String cloudProvider =
            getValueForKeyFromInstanceMetaData(InstanceMetaDataConstants.CLOUD_PROVIDER, instanceData);
        if (null != resourceId && cloudProvider.equals(CloudProvider.AWS.name())) {
          resourceIds.add(resourceId);
        }
      });
      if (isNotEmpty(resourceIds)) {
        awsCustomBillingService.updateAwsEC2BillingDataCache(
            new ArrayList<>(resourceIds), startTime, endTime, awsDataSetId);
      }
    }

    List<InstanceBillingData> instanceBillingDataList = new ArrayList<>();
    instanceDataGroupedCluster.forEach((clusterRecordId, instanceDataList) -> {
      InstanceData firstInstanceData = instanceDataList.get(0);
      Map<String, UtilizationData> utilizationDataForInstances = utilizationDataService.getUtilizationDataForInstances(
          instanceDataList, startTime.toString(), endTime.toString(), firstInstanceData.getAccountId(),
          firstInstanceData.getSettingId(), firstInstanceData.getClusterId());

      instanceDataList.stream()
          .filter(instanceData -> instanceData.getInstanceType() != null)
          .filter(
              instanceData -> instanceData.getInstanceType() != InstanceType.K8S_PV) // currently not billing for K8S_PV
          .filter(instanceData
              -> billingDataGenerationValidator.shouldGenerateBillingData(
                  instanceData.getAccountId(), instanceData.getClusterId(), startTime))
          .forEach(instanceData -> {
            UtilizationData utilizationData = utilizationDataForInstances.get(instanceData.getInstanceId());
            BillingData billingData =
                billingCalculationService.getInstanceBillingAmount(instanceData, utilizationData, startTime, endTime);
            log.trace("Instance detail {} :: {} ", instanceData.getInstanceId(), billingData.getBillingAmountBreakup());
            HarnessServiceInfo harnessServiceInfo = getHarnessServiceInfo(instanceData);
            String settingId =
                (instanceData.getInstanceType() == InstanceType.EC2_INSTANCE) ? null : instanceData.getSettingId();
            String clusterId =
                (instanceData.getInstanceType() == InstanceType.EC2_INSTANCE) ? null : instanceData.getClusterId();
            String instanceName = (instanceData.getInstanceName() == null) ? instanceData.getInstanceId()
                                                                           : instanceData.getInstanceName();
            String region = getValueForKeyFromInstanceMetaData(InstanceMetaDataConstants.REGION, instanceData);
            if (null == region) {
              region = "on_prem";
            }

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
                    .region(region)
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
                    .networkCost(billingData.getNetworkCost())
                    .pricingSource(billingData.getPricingSource().name())
                    .build();
            instanceBillingDataList.add(instanceBillingData);
          });
    });
    billingDataService.create(instanceBillingDataList, batchJobType);
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
