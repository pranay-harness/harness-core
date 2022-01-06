/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.batch.processing.tasklet;

import static io.harness.filesystem.FileIo.createDirectoryIfDoesNotExist;

import io.harness.avro.ClusterBillingData;
import io.harness.avro.Label;
import io.harness.batch.processing.billing.timeseries.data.InstanceBillingData;
import io.harness.batch.processing.billing.timeseries.service.impl.BillingDataServiceImpl;
import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.service.impl.GoogleCloudStorageServiceImpl;
import io.harness.batch.processing.tasklet.dto.HarnessTags;
import io.harness.batch.processing.tasklet.reader.BillingDataReader;
import io.harness.batch.processing.tasklet.support.HarnessEntitiesService;
import io.harness.batch.processing.tasklet.support.HarnessEntitiesService.HarnessEntities;
import io.harness.batch.processing.tasklet.support.HarnessTagService;
import io.harness.batch.processing.tasklet.support.K8SWorkloadService;
import io.harness.ccm.commons.beans.InstanceType;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.specific.SpecificDatumWriter;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Singleton
public class ClusterDataToBigQueryTasklet implements Tasklet {
  @Autowired private BatchMainConfig config;
  @Autowired private BillingDataServiceImpl billingDataService;
  @Autowired private GoogleCloudStorageServiceImpl googleCloudStorageService;
  @Autowired private K8SWorkloadService k8SWorkloadService;
  @Autowired private HarnessTagService harnessTagService;
  @Autowired private HarnessEntitiesService harnessEntitiesService;

  private static final String defaultParentWorkingDirectory = "./avro/";
  private static final String defaultBillingDataFileNameDaily = "billing_data_%s_%s_%s.avro";
  private static final String defaultBillingDataFileNameHourly = "billing_data_hourly_%s_%s_%s_%s.avro";
  private static final String gcsObjectNameFormat = "%s/%s";
  public static final long CACHE_SIZE = 10000;

  LoadingCache<HarnessEntitiesService.CacheKey, String> entityIdToNameCache =
      Caffeine.newBuilder()
          .maximumSize(CACHE_SIZE)
          .build(key -> harnessEntitiesService.fetchEntityName(key.getEntity(), key.getEntityId()));

  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
    JobParameters parameters = chunkContext.getStepContext().getStepExecution().getJobParameters();
    BatchJobType batchJobType =
        CCMJobConstants.getBatchJobTypeFromJobParams(parameters, CCMJobConstants.BATCH_JOB_TYPE);
    final CCMJobConstants jobConstants = new CCMJobConstants(chunkContext);
    int batchSize = config.getBatchQueryConfig().getQueryBatchSize();

    BillingDataReader billingDataReader = new BillingDataReader(billingDataService, jobConstants.getAccountId(),
        Instant.ofEpochMilli(jobConstants.getJobStartTime()), Instant.ofEpochMilli(jobConstants.getJobEndTime()),
        batchSize, 0, batchJobType);

    ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(jobConstants.getJobStartTime()), ZoneId.of("GMT"));
    String billingDataFileName = "";
    if (batchJobType == BatchJobType.CLUSTER_DATA_TO_BIG_QUERY) {
      billingDataFileName =
          String.format(defaultBillingDataFileNameDaily, zdt.getYear(), zdt.getMonth(), zdt.getDayOfMonth());
    } else if (batchJobType == BatchJobType.CLUSTER_DATA_HOURLY_TO_BIG_QUERY) {
      billingDataFileName = String.format(
          defaultBillingDataFileNameHourly, zdt.getYear(), zdt.getMonth(), zdt.getDayOfMonth(), zdt.getHour());
    }

    List<InstanceBillingData> instanceBillingDataList;
    boolean avroFileWithSchemaExists = false;
    do {
      instanceBillingDataList = billingDataReader.getNext();
      refreshLabelCache(jobConstants.getAccountId(), instanceBillingDataList);
      List<ClusterBillingData> clusterBillingData = instanceBillingDataList.stream()
                                                        .map(this::convertInstanceBillingDataToAVROObjects)
                                                        .collect(Collectors.toList());
      writeDataToAvro(jobConstants.getAccountId(), clusterBillingData, billingDataFileName, avroFileWithSchemaExists);
      avroFileWithSchemaExists = true;
    } while (instanceBillingDataList.size() == batchSize);

    final String gcsObjectName = String.format(gcsObjectNameFormat, jobConstants.getAccountId(), billingDataFileName);
    googleCloudStorageService.uploadObject(gcsObjectName, defaultParentWorkingDirectory + gcsObjectName);

    // Delete file once upload is complete
    File workingDirectory = new File(defaultParentWorkingDirectory + jobConstants.getAccountId());
    File billingDataFile = new File(workingDirectory, billingDataFileName);
    Files.delete(billingDataFile.toPath());

    return null;
  }

  @VisibleForTesting
  public void refreshLabelCache(String accountId, @NotNull List<InstanceBillingData> instanceBillingDataList) {
    final List<InstanceBillingData> dataNotPresentInLabelsCache =
        instanceBillingDataList.stream()
            .filter(instanceBillingData
                -> ImmutableSet.of(InstanceType.K8S_POD.name(), InstanceType.K8S_POD_FARGATE.name())
                       .contains(instanceBillingData.getInstanceType()))
            .filter(instanceBillingData
                -> null
                    == k8SWorkloadService.getK8sWorkloadLabel(accountId, instanceBillingData.getClusterId(),
                        instanceBillingData.getNamespace(), instanceBillingData.getWorkloadName()))
            .collect(Collectors.toList());

    final Map<K8SWorkloadService.CacheKey, HashSet<String>> clusterNamespaceWorkload = new HashMap<>();

    for (InstanceBillingData instanceBillingData : dataNotPresentInLabelsCache) {
      K8SWorkloadService.CacheKey key = new K8SWorkloadService.CacheKey(
          accountId, instanceBillingData.getClusterId(), instanceBillingData.getNamespace(), null);

      clusterNamespaceWorkload.computeIfAbsent(key, k -> new HashSet<>()).add(instanceBillingData.getWorkloadName());
    }

    clusterNamespaceWorkload.forEach(
        (key, workloadNames) -> k8SWorkloadService.updateK8sWorkloadLabelCache(key, workloadNames));
  }

  private void writeDataToAvro(String accountId, List<ClusterBillingData> instanceBillingDataAvro,
      String billingDataFileName, boolean avroFileWithSchemaExists) throws IOException {
    String directoryPath = defaultParentWorkingDirectory + accountId;
    createDirectoryIfDoesNotExist(directoryPath);
    File workingDirectory = new File(directoryPath);
    File billingDataFile = new File(workingDirectory, billingDataFileName);
    DataFileWriter<ClusterBillingData> dataFileWriter = getInstanceBillingDataDataFileWriter();
    if (avroFileWithSchemaExists) {
      dataFileWriter.appendTo(billingDataFile);
    } else {
      dataFileWriter.create(ClusterBillingData.getClassSchema(), billingDataFile);
    }
    for (ClusterBillingData row : instanceBillingDataAvro) {
      dataFileWriter.append(row);
    }
    dataFileWriter.close();
  }

  private ClusterBillingData convertInstanceBillingDataToAVROObjects(InstanceBillingData instanceBillingData) {
    String accountId = instanceBillingData.getAccountId();
    ClusterBillingData clusterBillingData = new ClusterBillingData();
    clusterBillingData.setAppid(instanceBillingData.getAppId());
    clusterBillingData.setEnvid(instanceBillingData.getEnvId());
    clusterBillingData.setRegion(instanceBillingData.getRegion());
    clusterBillingData.setServiceid(instanceBillingData.getServiceId());
    clusterBillingData.setCloudservicename(instanceBillingData.getCloudServiceName());
    clusterBillingData.setAccountid(accountId);
    clusterBillingData.setInstanceid(instanceBillingData.getInstanceId());
    clusterBillingData.setInstancename(instanceBillingData.getInstanceName());
    clusterBillingData.setClusterid(instanceBillingData.getClusterId());
    clusterBillingData.setSettingid(instanceBillingData.getSettingId());
    clusterBillingData.setLaunchtype(instanceBillingData.getLaunchType());
    clusterBillingData.setTaskid(instanceBillingData.getTaskId());
    clusterBillingData.setNamespace(instanceBillingData.getNamespace());
    clusterBillingData.setClustername(instanceBillingData.getClusterName());
    clusterBillingData.setClustertype(instanceBillingData.getClusterType());
    clusterBillingData.setInstancetype(instanceBillingData.getInstanceType());
    clusterBillingData.setWorkloadname(instanceBillingData.getWorkloadName());
    clusterBillingData.setWorkloadtype(instanceBillingData.getWorkloadType());
    clusterBillingData.setBillingaccountid(instanceBillingData.getBillingAccountId());
    clusterBillingData.setParentinstanceid(instanceBillingData.getParentInstanceId());
    clusterBillingData.setCloudproviderid(instanceBillingData.getCloudProviderId());
    clusterBillingData.setCloudprovider(instanceBillingData.getCloudProvider());
    clusterBillingData.setPricingsource(instanceBillingData.getPricingSource());

    clusterBillingData.setBillingamount(instanceBillingData.getBillingAmount().doubleValue());
    clusterBillingData.setCpubillingamount(instanceBillingData.getCpuBillingAmount().doubleValue());
    clusterBillingData.setMemorybillingamount(instanceBillingData.getMemoryBillingAmount().doubleValue());
    clusterBillingData.setIdlecost(instanceBillingData.getIdleCost().doubleValue());
    clusterBillingData.setCpuidlecost(instanceBillingData.getCpuIdleCost().doubleValue());
    clusterBillingData.setMemoryidlecost(instanceBillingData.getMemoryIdleCost().doubleValue());
    clusterBillingData.setSystemcost(instanceBillingData.getSystemCost().doubleValue());
    clusterBillingData.setCpusystemcost(instanceBillingData.getCpuSystemCost().doubleValue());
    clusterBillingData.setMemorysystemcost(instanceBillingData.getMemorySystemCost().doubleValue());
    clusterBillingData.setActualidlecost(instanceBillingData.getActualIdleCost().doubleValue());
    clusterBillingData.setCpuactualidlecost(instanceBillingData.getCpuActualIdleCost().doubleValue());
    clusterBillingData.setMemoryactualidlecost(instanceBillingData.getMemoryActualIdleCost().doubleValue());
    clusterBillingData.setNetworkcost(instanceBillingData.getNetworkCost());
    clusterBillingData.setUnallocatedcost(instanceBillingData.getUnallocatedCost().doubleValue());
    clusterBillingData.setCpuunallocatedcost(instanceBillingData.getCpuUnallocatedCost().doubleValue());
    clusterBillingData.setMemoryunallocatedcost(instanceBillingData.getMemoryUnallocatedCost().doubleValue());

    clusterBillingData.setMaxcpuutilization(instanceBillingData.getMaxCpuUtilization());
    clusterBillingData.setMaxmemoryutilization(instanceBillingData.getMaxMemoryUtilization());
    clusterBillingData.setAvgcpuutilization(instanceBillingData.getAvgCpuUtilization());
    clusterBillingData.setAvgmemoryutilization(instanceBillingData.getAvgMemoryUtilization());
    clusterBillingData.setMaxcpuutilizationvalue(instanceBillingData.getMaxCpuUtilizationValue());
    clusterBillingData.setMaxmemoryutilizationvalue(instanceBillingData.getMaxMemoryUtilizationValue());
    clusterBillingData.setAvgcpuutilizationvalue(instanceBillingData.getAvgCpuUtilizationValue());
    clusterBillingData.setAvgmemoryutilizationvalue(instanceBillingData.getAvgMemoryUtilizationValue());
    clusterBillingData.setCpurequest(instanceBillingData.getCpuRequest());
    clusterBillingData.setCpulimit(instanceBillingData.getCpuLimit());
    clusterBillingData.setMemoryrequest(instanceBillingData.getMemoryRequest());
    clusterBillingData.setMemorylimit(instanceBillingData.getMemoryLimit());
    clusterBillingData.setCpuunitseconds(instanceBillingData.getCpuUnitSeconds());
    clusterBillingData.setMemorymbseconds(instanceBillingData.getMemoryMbSeconds());
    clusterBillingData.setUsagedurationseconds(instanceBillingData.getUsageDurationSeconds());
    clusterBillingData.setEndtime(instanceBillingData.getEndTimestamp());
    clusterBillingData.setStarttime(instanceBillingData.getStartTimestamp());
    clusterBillingData.setStoragecost(getDoubleValueFromBigDecimal(instanceBillingData.getStorageBillingAmount()));
    clusterBillingData.setStorageactualidlecost(
        getDoubleValueFromBigDecimal(instanceBillingData.getStorageActualIdleCost()));
    clusterBillingData.setStorageunallocatedcost(
        getDoubleValueFromBigDecimal(instanceBillingData.getStorageUnallocatedCost()));
    clusterBillingData.setStorageutilizationvalue(instanceBillingData.getStorageUtilizationValue());
    clusterBillingData.setStoragerequest(instanceBillingData.getStorageRequest());
    clusterBillingData.setMaxstorageutilizationvalue(instanceBillingData.getMaxStorageUtilizationValue());
    clusterBillingData.setMaxstoragerequest(instanceBillingData.getMaxStorageRequest());
    clusterBillingData.setOrgIdentifier(instanceBillingData.getOrgIdentifier());
    clusterBillingData.setProjectIdentifier(instanceBillingData.getProjectIdentifier());

    if (instanceBillingData.getAppId() != null) {
      clusterBillingData.setAppname(entityIdToNameCache.get(
          new HarnessEntitiesService.CacheKey(instanceBillingData.getAppId(), HarnessEntities.APP)));
    } else {
      clusterBillingData.setAppname(null);
    }

    if (instanceBillingData.getEnvId() != null) {
      clusterBillingData.setEnvname(entityIdToNameCache.get(
          new HarnessEntitiesService.CacheKey(instanceBillingData.getEnvId(), HarnessEntities.ENV)));
    } else {
      clusterBillingData.setEnvname(null);
    }

    if (instanceBillingData.getServiceId() != null) {
      clusterBillingData.setServicename(entityIdToNameCache.get(
          new HarnessEntitiesService.CacheKey(instanceBillingData.getServiceId(), HarnessEntities.SERVICE)));
    } else {
      clusterBillingData.setServicename(null);
    }

    List<Label> labels = new ArrayList<>();
    if (ImmutableSet.of(InstanceType.K8S_POD.name(), InstanceType.K8S_POD_FARGATE.name())
            .contains(instanceBillingData.getInstanceType())) {
      Map<String, String> k8sWorkloadLabel =
          k8SWorkloadService.getK8sWorkloadLabel(accountId, instanceBillingData.getClusterId(),
              instanceBillingData.getNamespace(), instanceBillingData.getWorkloadName());

      if (null != k8sWorkloadLabel) {
        k8sWorkloadLabel.forEach((key, value) -> {
          Label workloadLabel = new Label();
          workloadLabel.setKey(key);
          workloadLabel.setValue(value);
          labels.add(workloadLabel);
        });
      }
    }

    if (null != instanceBillingData.getAppId()) {
      List<HarnessTags> harnessTags = harnessTagService.getHarnessTags(accountId, instanceBillingData.getAppId());
      harnessTags.addAll(harnessTagService.getHarnessTags(accountId, instanceBillingData.getServiceId()));
      harnessTags.addAll(harnessTagService.getHarnessTags(accountId, instanceBillingData.getEnvId()));
      harnessTags.forEach(harnessTag -> {
        Label harnessLabel = new Label();
        harnessLabel.setKey(harnessTag.getKey());
        harnessLabel.setValue(harnessTag.getValue());
        labels.add(harnessLabel);
      });
    }

    clusterBillingData.setLabels(Arrays.asList(labels.toArray()));
    return clusterBillingData;
  }

  @NotNull
  private static DataFileWriter<ClusterBillingData> getInstanceBillingDataDataFileWriter() {
    DatumWriter<ClusterBillingData> userDatumWriter = new SpecificDatumWriter<>(ClusterBillingData.class);
    return new DataFileWriter<>(userDatumWriter);
  }

  private static double getDoubleValueFromBigDecimal(BigDecimal value) {
    if (value != null) {
      return value.doubleValue();
    }
    return 0D;
  }
}
