package io.harness.batch.processing.tasklet;

import static io.harness.filesystem.FileIo.createDirectoryIfDoesNotExist;

import com.google.inject.Singleton;

import io.harness.avro.ClusterBillingData;
import io.harness.avro.Label;
import io.harness.batch.processing.billing.timeseries.data.InstanceBillingData;
import io.harness.batch.processing.billing.timeseries.service.impl.BillingDataServiceImpl;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.service.impl.GoogleCloudStorageServiceImpl;
import io.harness.batch.processing.tasklet.reader.BillingDataReader;
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class ClusterDataToBigQueryTasklet implements Tasklet {
  @Autowired private BatchMainConfig config;
  @Autowired private BillingDataServiceImpl billingDataService;
  @Autowired private GoogleCloudStorageServiceImpl googleCloudStorageService;

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
      List<ClusterBillingData> clusterBillingData = instanceBillingDataList.stream()
                                                        .map(this ::convertInstanceBillingDataToAVROObjects)
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
    ClusterBillingData clusterBillingData = new ClusterBillingData();
    clusterBillingData.setAppid(instanceBillingData.getAppId());
    clusterBillingData.setEnvid(instanceBillingData.getEnvId());
    clusterBillingData.setRegion(instanceBillingData.getRegion());
    clusterBillingData.setServiceid(instanceBillingData.getServiceId());
    clusterBillingData.setCloudservicename(instanceBillingData.getCloudServiceName());
    clusterBillingData.setAccountid(instanceBillingData.getAccountId());
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

    Label appLabel = new Label();
    appLabel.setKey("appId");
    appLabel.setValue(instanceBillingData.getAppId());

    Label envLabel = new Label();
    envLabel.setKey("envId");
    envLabel.setValue(instanceBillingData.getEnvId());

    Label serviceLabel = new Label();
    serviceLabel.setKey("serviceId");
    serviceLabel.setValue(instanceBillingData.getServiceId());

    clusterBillingData.setLabels(Arrays.asList(appLabel, envLabel, serviceLabel));

    return clusterBillingData;
  }

  @NotNull
  private static DataFileWriter<ClusterBillingData> getInstanceBillingDataDataFileWriter() {
    DatumWriter<ClusterBillingData> userDatumWriter = new SpecificDatumWriter<>(ClusterBillingData.class);
    return new DataFileWriter<>(userDatumWriter);
  }
}
