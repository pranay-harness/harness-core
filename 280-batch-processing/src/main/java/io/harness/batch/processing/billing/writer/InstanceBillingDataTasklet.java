package io.harness.batch.processing.billing.writer;

import static io.harness.batch.processing.tasklet.util.InstanceMetaDataUtils.getValueForKeyFromInstanceMetaData;
import static io.harness.ccm.commons.beans.InstanceType.K8S_POD;
import static io.harness.ccm.commons.beans.InstanceType.K8S_POD_FARGATE;
import static io.harness.ccm.commons.beans.InstanceType.K8S_PV;
import static io.harness.data.structure.HasPredicate.hasSome;

import static com.google.common.base.MoreObjects.firstNonNull;

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
import io.harness.batch.processing.tasklet.util.InstanceMetaDataUtils;
import io.harness.batch.processing.writer.constants.InstanceMetaDataConstants;
import io.harness.batch.processing.writer.constants.K8sCCMConstants;
import io.harness.ccm.commons.beans.HarnessServiceInfo;
import io.harness.ccm.commons.beans.InstanceType;
import io.harness.ccm.commons.beans.Resource;
import io.harness.ccm.commons.entities.InstanceData;
import io.harness.persistence.HPersistence;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.mutable.MutableInt;
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
  private static final String CLAIM_REF_SEPARATOR = "/";
  private int batchSize;
  private BatchJobType batchJobType;

  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) {
    parameters = chunkContext.getStepContext().getStepExecution().getJobParameters();
    batchSize = config.getBatchQueryConfig().getInstanceDataBatchSize();
    String accountId = parameters.getString(CCMJobConstants.ACCOUNT_ID);
    Instant startTime = getFieldValueFromJobParams(CCMJobConstants.JOB_START_DATE);
    Instant endTime = getFieldValueFromJobParams(CCMJobConstants.JOB_END_DATE);
    batchJobType = CCMJobConstants.getBatchJobTypeFromJobParams(parameters, CCMJobConstants.BATCH_JOB_TYPE);
    // Instant of 1-1-2018
    Instant seekingDate = Instant.ofEpochMilli(1514764800000l);
    // bill PV first
    List<InstanceBillingData> pvInstanceBillingDataList = billPVInstances(accountId, startTime, endTime);
    Map<String, InstanceBillingData> claimRefToPVInstanceBillingData =
        pvInstanceBillingDataList.stream().collect(Collectors.toMap(e
            -> e.getNamespace() + CLAIM_REF_SEPARATOR + e.getWorkloadName(),
            e -> e, (e1, e2) -> e1.getStartTimestamp() > e2.getStartTimestamp() ? e1 : e2));

    Map<String, MutableInt> pvcClaimCount = getPvcClaimCount(accountId, startTime, endTime);
    List<InstanceData> instanceDataLists;
    do {
      instanceDataLists =
          instanceDataDao.getInstanceDataListsOtherThanPV(accountId, batchSize, startTime, endTime, seekingDate);
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
        createBillingData(accountId, startTime, endTime, batchJobType, instanceDataLists,
            claimRefToPVInstanceBillingData, pvcClaimCount);
      } catch (Exception ex) {
        log.error("Exception in billing step", ex);
        throw ex;
      }
    } while (instanceDataLists.size() == batchSize);
    return null;
  }

  private Map<String, MutableInt> getPvcClaimCount(String accountId, Instant startTime, Instant endTime) {
    Instant seekingDate = Instant.ofEpochMilli(1514764800000l);
    List<InstanceData> instanceDataLists;
    Map<String, MutableInt> result = new HashMap<>();
    do {
      // TODO change here
      instanceDataLists =
          instanceDataDao.getInstanceDataListsOfType(accountId, batchSize, startTime, endTime, seekingDate, K8S_POD);
      if (!instanceDataLists.isEmpty()) {
        Instant lastUsageStartTime = instanceDataLists.get(instanceDataLists.size() - 1).getUsageStartTime();
        seekingDate = instanceDataLists.get(instanceDataLists.size() - 1).getUsageStartTime();
        if (instanceDataLists.get(0).getUsageStartTime().equals(lastUsageStartTime)) {
          log.info("Incrementing Seeking Date by 1ms {} {} {} {}", instanceDataLists.size(), startTime, endTime,
              parameters.toString());
          seekingDate = seekingDate.plus(1, ChronoUnit.MILLIS);
        }
      }
      for (InstanceData instanceData : instanceDataLists) {
        List<String> pvcClaimNames = firstNonNull(instanceData.getPvcClaimNames(), Collections.emptyList());
        String namespace = getValueForKeyFromInstanceMetaData(InstanceMetaDataConstants.NAMESPACE, instanceData);
        for (String claimName : pvcClaimNames) {
          String claimRef = namespace + CLAIM_REF_SEPARATOR + claimName;
          result.computeIfAbsent(claimRef, k -> new MutableInt(0));
          result.get(claimRef).increment();
        }
      }
    } while (instanceDataLists.size() == batchSize);

    return result;
  }

  private List<InstanceBillingData> billPVInstances(String accountId, Instant startTime, Instant endTime) {
    List<InstanceBillingData> instanceBillingDataList = new ArrayList<>();
    Instant seekingDate = Instant.ofEpochMilli(1514764800000l);
    List<InstanceData> instanceDataLists;
    do {
      instanceDataLists =
          instanceDataDao.getInstanceDataListsOfType(accountId, batchSize, startTime, endTime, seekingDate, K8S_PV);
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
        instanceBillingDataList.addAll(createBillingData(
            accountId, startTime, endTime, batchJobType, instanceDataLists, ImmutableMap.of(), ImmutableMap.of()));
      } catch (Exception ex) {
        log.error("Exception in billing step", ex);
        throw ex;
      }
    } while (instanceDataLists.size() == batchSize);
    return instanceBillingDataList;
  }

  List<InstanceBillingData> createBillingData(String accountId, Instant startTime, Instant endTime,
      BatchJobType batchJobType, List<InstanceData> instanceDataLists,
      Map<String, InstanceBillingData> claimRefToPVInstanceBillingData, Map<String, MutableInt> pvcClaimCount) {
    log.info("Instance data list new {} {} {} {}", instanceDataLists.size(), startTime, endTime, parameters.toString());

    Map<String, List<InstanceData>> instanceDataGroupedCluster =
        instanceDataLists.stream()
            .filter(this::validInstanceForBilling)
            .collect(Collectors.groupingBy(InstanceData::getClusterId));
    String awsDataSetId = customBillingMetaDataService.getAwsDataSetId(accountId);
    log.info("AWS data set {}", awsDataSetId);
    if (awsDataSetId != null) {
      Set<String> resourceIds = new HashSet<>();
      Set<String> eksFargateResourceIds = new HashSet<>();
      instanceDataLists.forEach(instanceData -> {
        String resourceId =
            getValueForKeyFromInstanceMetaData(InstanceMetaDataConstants.CLOUD_PROVIDER_INSTANCE_ID, instanceData);
        String cloudProvider =
            getValueForKeyFromInstanceMetaData(InstanceMetaDataConstants.CLOUD_PROVIDER, instanceData);
        if (null != resourceId && cloudProvider.equals(CloudProvider.AWS.name())) {
          resourceIds.add(resourceId);
        }

        String computeType = InstanceMetaDataUtils.getValueForKeyFromInstanceMetaData(
            InstanceMetaDataConstants.COMPUTE_TYPE, instanceData.getMetaData());
        if (instanceData.getInstanceType() == K8S_POD_FARGATE
            || (instanceData.getInstanceType() == K8S_POD
                && K8sCCMConstants.AWS_FARGATE_COMPUTE_TYPE.equals(computeType))) {
          // instanceId is resourceId
          eksFargateResourceIds.add(instanceData.getInstanceId());
        }
      });
      if (hasSome(resourceIds)) {
        awsCustomBillingService.updateAwsEC2BillingDataCache(
            new ArrayList<>(resourceIds), startTime, endTime, awsDataSetId);
      }

      if (hasSome(eksFargateResourceIds)) {
        log.info("Updating EKS Fargate Cache for Resource Id's List of Size: {}", eksFargateResourceIds.size());
        awsCustomBillingService.updateEksFargateDataCache(
            new ArrayList<>(eksFargateResourceIds), startTime, endTime, awsDataSetId);
      }
    }

    List<InstanceBillingData> instanceBillingDataList = new ArrayList<>();
    instanceDataGroupedCluster.forEach((clusterRecordId, instanceDataList) -> {
      InstanceData firstInstanceData = instanceDataList.get(0);
      Map<String, UtilizationData> utilizationDataForInstances = utilizationDataService.getUtilizationDataForInstances(
          instanceDataList, startTime.toString(), endTime.toString(), firstInstanceData.getAccountId(),
          firstInstanceData.getSettingId(), firstInstanceData.getClusterId());

      for (InstanceData instanceData : instanceDataList) {
        if (instanceData.getInstanceType() != null
            && billingDataGenerationValidator.shouldGenerateBillingData(
                instanceData.getAccountId(), instanceData.getClusterId(), startTime)) {
          InstanceBillingData instanceBillingData = getInstanceBillingData(instanceData, utilizationDataForInstances,
              startTime, endTime, claimRefToPVInstanceBillingData, pvcClaimCount);
          instanceBillingDataList.add(instanceBillingData);
        }
      }
    });

    billingDataService.create(instanceBillingDataList, batchJobType);
    return instanceBillingDataList;
  }

  private boolean validInstanceForBilling(InstanceData instanceData) {
    String computeType = InstanceMetaDataUtils.getValueForKeyFromInstanceMetaData(
        InstanceMetaDataConstants.COMPUTE_TYPE, instanceData.getMetaData());
    boolean validInstance = true;
    if ((instanceData.getInstanceType() == null)
        || (instanceData.getInstanceType() == InstanceType.K8S_NODE
            && K8sCCMConstants.AWS_FARGATE_COMPUTE_TYPE.equals(computeType))) {
      validInstance = false;
    }
    return validInstance;
  }

  private InstanceBillingData getInstanceBillingData(final InstanceData instanceData,
      Map<String, UtilizationData> utilizationDataForInstances, Instant startTime, Instant endTime,
      Map<String, InstanceBillingData> claimRefToPVInstanceBillingData, Map<String, MutableInt> pvcClaimCount) {
    InstanceType instanceType = instanceData.getInstanceType();
    String computeType = InstanceMetaDataUtils.getValueForKeyFromInstanceMetaData(
        InstanceMetaDataConstants.COMPUTE_TYPE, instanceData.getMetaData());
    if (instanceData.getInstanceType() == K8S_POD && K8sCCMConstants.AWS_FARGATE_COMPUTE_TYPE.equals(computeType)) {
      instanceType = InstanceType.K8S_POD_FARGATE;
      instanceData.setInstanceType(instanceType);
    }
    UtilizationData utilizationData = utilizationDataForInstances.get(instanceData.getInstanceId());
    BillingData billingData =
        billingCalculationService.getInstanceBillingAmount(instanceData, utilizationData, startTime, endTime);

    log.trace("Instance detail {} :: {} ", instanceData.getInstanceId(), billingData.getBillingAmountBreakup());

    HarnessServiceInfo harnessServiceInfo = getHarnessServiceInfo(instanceData);
    String settingId = instanceData.getSettingId();
    String clusterId = instanceData.getClusterId();
    if (instanceType == InstanceType.EC2_INSTANCE) {
      settingId = null;
      clusterId = null;
    }
    String instanceName =
        (instanceData.getInstanceName() == null) ? instanceData.getInstanceId() : instanceData.getInstanceName();
    String region = getValueForKeyFromInstanceMetaData(InstanceMetaDataConstants.REGION, instanceData);
    String namespace = getValueForKeyFromInstanceMetaData(InstanceMetaDataConstants.NAMESPACE, instanceData);
    String workloadType = getValueForKeyFromInstanceMetaData(InstanceMetaDataConstants.WORKLOAD_TYPE, instanceData);
    String workloadName = getValueForKeyFromInstanceMetaData(InstanceMetaDataConstants.WORKLOAD_NAME, instanceData);

    Resource totalResource = instanceData.getTotalResource();
    Resource limitResource = null == instanceData.getLimitResource()
        ? Resource.builder().cpuUnits(0.0).memoryMb(0.0).build()
        : instanceData.getLimitResource();

    BigDecimal billingAmount = billingData.getBillingAmountBreakup().getBillingAmount();
    BigDecimal actualIdleCost = billingData.getIdleCostData().getIdleCost();
    BigDecimal unallocatedCost = BigDecimal.ZERO;

    BigDecimal storageBillingAmount = billingData.getBillingAmountBreakup().getStorageBillingAmount();
    BigDecimal storageActualIdleCost = billingData.getIdleCostData().getStorageIdleCost();
    BigDecimal storageUnallocatedCost = getStorageUnallocatedCost(billingData, utilizationData, instanceData);

    double storageRequest = utilizationData.getAvgStorageRequestValue();
    double storageUtilization = utilizationData.getAvgStorageUsageValue();
    double storageMBSeconds = billingData.getStorageMbSeconds();

    if (K8S_PV == instanceType) {
      totalResource = Resource.builder().cpuUnits(0D).memoryMb(0D).build();
      workloadName = getValueForKeyFromInstanceMetaData(InstanceMetaDataConstants.CLAIM_NAME, instanceData);
      namespace = getValueForKeyFromInstanceMetaData(InstanceMetaDataConstants.CLAIM_NAMESPACE, instanceData);
      unallocatedCost = unallocatedCost.add(storageUnallocatedCost);
    } else if (K8S_POD == instanceType) {
      List<String> pvcClaimNames = firstNonNull(instanceData.getPvcClaimNames(), ImmutableList.of());
      for (String claimName : pvcClaimNames) {
        String claimRef = namespace + CLAIM_REF_SEPARATOR + claimName;
        InstanceBillingData storageInstanceBillingData = claimRefToPVInstanceBillingData.get(claimRef);

        if (storageInstanceBillingData != null) {
          Integer claimCount = pvcClaimCount.getOrDefault(claimRef, new MutableInt(1)).getValue();
          // only costs gets divided by claimCount to handle double spending, the requests will remain the same.
          storageBillingAmount = storageBillingAmount.add(storageInstanceBillingData.getStorageBillingAmount().divide(
              BigDecimal.valueOf(claimCount), MathContext.DECIMAL128));
          storageActualIdleCost =
              storageActualIdleCost.add(storageInstanceBillingData.getStorageActualIdleCost().divide(
                  BigDecimal.valueOf(claimCount), MathContext.DECIMAL128));
          storageUnallocatedCost =
              storageUnallocatedCost.add(storageInstanceBillingData.getStorageUnallocatedCost().divide(
                  BigDecimal.valueOf(claimCount), MathContext.DECIMAL128));

          storageRequest += storageInstanceBillingData.getStorageRequest();
          storageUtilization += storageInstanceBillingData.getStorageUtilizationValue();
          storageMBSeconds += storageInstanceBillingData.getStorageMbSeconds();
        }
      }
      billingAmount = billingAmount.add(storageBillingAmount);
      actualIdleCost = actualIdleCost.add(storageActualIdleCost);
      unallocatedCost = unallocatedCost.add(storageUnallocatedCost);
    }

    return InstanceBillingData.builder()
        .accountId(instanceData.getAccountId())
        .settingId(settingId)
        .clusterId(clusterId)
        .instanceType(instanceType.toString())
        .billingAccountId("BILLING_ACCOUNT_ID")
        .startTimestamp(startTime.toEpochMilli())
        .endTimestamp(endTime.toEpochMilli())
        .billingAmount(billingAmount)
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
        .namespace(namespace)
        .region(firstNonNull(region, "on_prem"))
        .clusterType(getValueForKeyFromInstanceMetaData(InstanceMetaDataConstants.CLUSTER_TYPE, instanceData))
        .cloudProvider(getValueForKeyFromInstanceMetaData(InstanceMetaDataConstants.CLOUD_PROVIDER, instanceData))
        .workloadName(workloadName)
        .workloadType(workloadType)
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
        .actualIdleCost(actualIdleCost)
        .cpuActualIdleCost(billingData.getIdleCostData().getCpuIdleCost())
        .memoryActualIdleCost(billingData.getIdleCostData().getMemoryIdleCost())
        .unallocatedCost(unallocatedCost)
        .cpuUnallocatedCost(BigDecimal.ZERO)
        .memoryUnallocatedCost(BigDecimal.ZERO)
        .networkCost(billingData.getNetworkCost())
        .pricingSource(billingData.getPricingSource().name())
        .storageBillingAmount(storageBillingAmount)
        .storageActualIdleCost(storageActualIdleCost)
        .storageUnallocatedCost(storageUnallocatedCost)
        .storageUtilizationValue(storageUtilization)
        .storageRequest(storageRequest)
        .storageMbSeconds(storageMBSeconds)
        .build();
  }

  private BigDecimal getStorageUnallocatedCost(
      BillingData billingData, UtilizationData utilizationData, InstanceData instanceData) {
    if (K8S_PV == instanceData.getInstanceType()) {
      BigDecimal storageUnallocatedFraction = BigDecimal.ZERO;
      if (instanceData.getStorageResource() != null && instanceData.getStorageResource().getCapacity() > 0) {
        BigDecimal capacityFromInstanceData = BigDecimal.valueOf(instanceData.getStorageResource().getCapacity());
        storageUnallocatedFraction =
            capacityFromInstanceData.subtract(BigDecimal.valueOf(utilizationData.getAvgStorageRequestValue()))
                .divide(capacityFromInstanceData, MathContext.DECIMAL128);
      }
      if (storageUnallocatedFraction.compareTo(BigDecimal.ZERO) < 0) {
        log.warn("-ve storageUnallocatedCost, Request:{}/Capacity:{} {}", utilizationData.getAvgStorageRequestValue(),
            utilizationData.getAvgStorageCapacityValue(), instanceData.toString());
        return BigDecimal.ZERO;
      }
      return billingData.getBillingAmountBreakup().getStorageBillingAmount().multiply(storageUnallocatedFraction);
    }
    return BigDecimal.ZERO;
  }

  String getParentInstanceId(InstanceData instanceData) {
    String actualParentResourceId =
        getValueForKeyFromInstanceMetaData(InstanceMetaDataConstants.ACTUAL_PARENT_RESOURCE_ID, instanceData);
    if (null == actualParentResourceId
        && ImmutableSet.of(InstanceType.K8S_POD, InstanceType.K8S_POD_FARGATE)
               .contains(instanceData.getInstanceType())) {
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

  private Instant getFieldValueFromJobParams(String fieldName) {
    return Instant.ofEpochMilli(Long.parseLong(parameters.getString(fieldName)));
  }
}
