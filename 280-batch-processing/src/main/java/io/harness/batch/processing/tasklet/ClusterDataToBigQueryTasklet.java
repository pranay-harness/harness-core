package io.harness.batch.processing.tasklet;

import static io.harness.filesystem.FileIo.createDirectoryIfDoesNotExist;

import io.harness.avro.ClusterBillingData;
import io.harness.avro.Label;
import io.harness.batch.processing.billing.timeseries.data.InstanceBillingData;
import io.harness.batch.processing.billing.timeseries.service.impl.BillingDataServiceImpl;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.service.impl.GoogleCloudStorageServiceImpl;
import io.harness.batch.processing.tasklet.dto.HarnessTags;
import io.harness.batch.processing.tasklet.reader.BillingDataReader;
import io.harness.batch.processing.tasklet.support.HarnessTagService;
import io.harness.batch.processing.tasklet.support.K8SWorkloadService;
import io.harness.ccm.commons.beans.InstanceType;

import com.google.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

  private static final String defaultParentWorkingDirectory = "./avro/";
  private static final String defaultBillingDataFileName = "billing_data_%s_%s_%s.avro";
  private static final String gcsObjectNameFormat = "%s/%s";

  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
    JobParameters parameters = chunkContext.getStepContext().getStepExecution().getJobParameters();
    Long startTime = CCMJobConstants.getFieldLongValueFromJobParams(parameters, CCMJobConstants.JOB_START_DATE);
    Long endTime = CCMJobConstants.getFieldLongValueFromJobParams(parameters, CCMJobConstants.JOB_END_DATE);
    int batchSize = config.getBatchQueryConfig().getQueryBatchSize();
    String accountId = parameters.getString(CCMJobConstants.ACCOUNT_ID);

    BillingDataReader billingDataReader = new BillingDataReader(
        billingDataService, accountId, Instant.ofEpochMilli(startTime), Instant.ofEpochMilli(endTime), batchSize, 0);

    ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.of("GMT"));
    String billingDataFileName =
        String.format(defaultBillingDataFileName, zdt.getYear(), zdt.getMonth(), zdt.getDayOfMonth());

    List<InstanceBillingData> instanceBillingDataList;
    boolean avroFileWithSchemaExists = false;
    do {
      instanceBillingDataList = billingDataReader.getNext();
      refreshLabelCache(accountId, instanceBillingDataList);
      List<ClusterBillingData> clusterBillingData = instanceBillingDataList.stream()
                                                        .map(this::convertInstanceBillingDataToAVROObjects)
                                                        .collect(Collectors.toList());
      writeDataToAvro(accountId, clusterBillingData, billingDataFileName, avroFileWithSchemaExists);
      avroFileWithSchemaExists = true;
    } while (instanceBillingDataList.size() == batchSize);

    final String gcsObjectName = String.format(gcsObjectNameFormat, accountId, billingDataFileName);
    googleCloudStorageService.uploadObject(gcsObjectName, defaultParentWorkingDirectory + gcsObjectName);

    // Delete file once upload is complete
    File workingDirectory = new File(defaultParentWorkingDirectory + accountId);
    File billingDataFile = new File(workingDirectory, billingDataFileName);
    Files.delete(billingDataFile.toPath());

    return null;
  }

  private void refreshLabelCache(String accountId, List<InstanceBillingData> instanceBillingDataList) {
    Map<String, Set<String>> clusterWorkload =
        instanceBillingDataList.stream()
            .filter(instanceBillingData -> instanceBillingData.getInstanceType().equals(InstanceType.K8S_POD.name()))
            .filter(instanceBillingData
                -> null
                    == k8SWorkloadService.getK8sWorkloadLabel(
                        accountId, instanceBillingData.getClusterId(), instanceBillingData.getWorkloadName()))
            .collect(Collectors.groupingBy(InstanceBillingData::getClusterId,
                Collectors.mapping(InstanceBillingData::getWorkloadName, Collectors.toSet())));
    clusterWorkload.forEach(
        (cluster, workloadNames) -> k8SWorkloadService.updateK8sWorkloadLabelCache(accountId, cluster, workloadNames));
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

    List<Label> labels = new ArrayList<>();
    if (instanceBillingData.getInstanceType().equals(InstanceType.K8S_POD.name())) {
      Map<String, String> k8sWorkloadLabel = k8SWorkloadService.getK8sWorkloadLabel(
          accountId, instanceBillingData.getClusterId(), instanceBillingData.getWorkloadName());

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
}
