package io.harness.perpetualtask.datacollection;

import static io.harness.cvng.beans.DataCollectionExecutionStatus.FAILED;
import static io.harness.cvng.beans.DataCollectionExecutionStatus.SUCCESS;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.beans.DecryptableEntity;
import io.harness.cvng.beans.DataCollectionInfo;
import io.harness.cvng.beans.DataCollectionTaskDTO;
import io.harness.cvng.beans.DataCollectionTaskDTO.DataCollectionTaskResult;
import io.harness.cvng.beans.LogDataCollectionInfo;
import io.harness.cvng.perpetualtask.CVDataCollectionInfo;
import io.harness.datacollection.DataCollectionDSLService;
import io.harness.datacollection.entity.LogDataRecord;
import io.harness.datacollection.entity.RuntimeParameters;
import io.harness.datacollection.entity.TimeSeriesRecord;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.service.HostRecordDataStoreService;
import io.harness.delegate.service.LogRecordDataStoreService;
import io.harness.delegate.service.TimeSeriesDataStoreService;
import io.harness.grpc.utils.AnyUtils;
import io.harness.perpetualtask.PerpetualTaskExecutionParams;
import io.harness.perpetualtask.PerpetualTaskExecutor;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskResponse;
import io.harness.security.encryption.SecretDecryptionService;
import io.harness.serializer.KryoSerializer;
import io.harness.verificationclient.CVNextGenServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import software.wings.delegatetasks.DelegateLogService;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

@Slf4j
public class DataCollectionPerpetualTaskExecutor implements PerpetualTaskExecutor {
  @Inject private CVNextGenServiceClient cvNextGenServiceClient;
  @Inject private SecretDecryptionService secretDecryptionService;

  @Inject private DelegateLogService delegateLogService;
  @Inject private TimeSeriesDataStoreService timeSeriesDataStoreService;
  @Inject private LogRecordDataStoreService logRecordDataStoreService;
  @Inject private HostRecordDataStoreService hostRecordDataStoreService;
  @Inject private KryoSerializer kryoSerializer;

  @Inject private DataCollectionDSLService dataCollectionDSLService;
  @Inject @Named("verificationDataCollectorExecutor") protected ExecutorService dataCollectionService;

  @Override
  public PerpetualTaskResponse runOnce(
      PerpetualTaskId taskId, PerpetualTaskExecutionParams params, Instant heartbeatTime) {
    DataCollectionPerpetualTaskParams taskParams =
        AnyUtils.unpack(params.getCustomizedParams(), DataCollectionPerpetualTaskParams.class);
    logger.info("Executing for !! cvConfigId: {} verificationTaskId: {}", taskParams.getDataCollectionWorkerId());
    CVDataCollectionInfo cvDataCollectionInfo =
        (CVDataCollectionInfo) kryoSerializer.asObject(taskParams.getDataCollectionInfo().toByteArray());
    logger.info("DataCollectionInfo {} ", cvDataCollectionInfo);
    DataCollectionTaskDTO dataCollectionTask;
    secretDecryptionService.decrypt(cvDataCollectionInfo.getConnectorConfigDTO() instanceof DecryptableEntity
            ? (DecryptableEntity) cvDataCollectionInfo.getConnectorConfigDTO()
            : null,
        cvDataCollectionInfo.getEncryptedDataDetails());
    dataCollectionDSLService.registerDatacollectionExecutorService(dataCollectionService);
    try {
      // TODO: What happens if this task takes more time then the schedule?
      while (true) {
        dataCollectionTask = getNextDataCollectionTask(taskParams);
        if (dataCollectionTask == null) {
          logger.info("Nothing to process.");
          break;
        } else {
          logger.info("Next task to process: ", dataCollectionTask);
          run(taskParams, cvDataCollectionInfo.getConnectorConfigDTO(), dataCollectionTask);
        }
      }
    } catch (IOException e) {
      logger.error("Perpetual task failed with exception", e);
      throw new IllegalStateException(e);
    }

    return PerpetualTaskResponse.builder().responseCode(200).responseMessage("success").build();
  }

  private DataCollectionTaskDTO getNextDataCollectionTask(DataCollectionPerpetualTaskParams taskParams)
      throws IOException {
    return cvNextGenServiceClient
        .getNextDataCollectionTask(taskParams.getAccountId(), taskParams.getDataCollectionWorkerId())
        .execute()
        .body()
        .getResource();
  }

  private void run(DataCollectionPerpetualTaskParams taskParams, ConnectorConfigDTO connectorConfigDTO,
      DataCollectionTaskDTO dataCollectionTask) throws IOException {
    try {
      final String cvConfigId = dataCollectionTask.getCvConfigId();
      DataCollectionInfo dataCollectionInfo = dataCollectionTask.getDataCollectionInfo();
      final RuntimeParameters runtimeParameters =
          RuntimeParameters.builder()
              .baseUrl(dataCollectionTask.getDataCollectionInfo().getBaseUrl(connectorConfigDTO))
              .commonHeaders(dataCollectionTask.getDataCollectionInfo().collectionHeaders(connectorConfigDTO))
              .commonOptions(dataCollectionTask.getDataCollectionInfo().collectionParams(connectorConfigDTO))
              .otherEnvVariables(dataCollectionInfo.getDslEnvVariables())
              .endTime(dataCollectionTask.getEndTime())
              .startTime(dataCollectionTask.getStartTime())
              .build();
      switch (dataCollectionInfo.getVerificationType()) {
        case TIME_SERIES:
          List<TimeSeriesRecord> timeSeriesRecords = (List<TimeSeriesRecord>) dataCollectionDSLService.execute(
              dataCollectionInfo.getDataCollectionDsl(), runtimeParameters,
              new ThirdPartyCallHandler(
                  dataCollectionTask.getAccountId(), dataCollectionTask.getVerificationTaskId(), delegateLogService));
          timeSeriesDataStoreService.saveTimeSeriesDataRecords(dataCollectionTask.getAccountId(), cvConfigId,
              dataCollectionTask.getVerificationTaskId(), timeSeriesRecords);

          break;
        case LOG:
          List<LogDataRecord> logDataRecords = (List<LogDataRecord>) dataCollectionDSLService.execute(
              dataCollectionInfo.getDataCollectionDsl(), runtimeParameters,
              new ThirdPartyCallHandler(
                  dataCollectionTask.getAccountId(), dataCollectionTask.getVerificationTaskId(), delegateLogService));
          // TODO: merge these 2 IDs into a single concepts and use it everywhere in the data collection using models.
          logRecordDataStoreService.save(dataCollectionTask.getAccountId(), dataCollectionTask.getCvConfigId(),
              dataCollectionTask.getVerificationTaskId(), logDataRecords);
          if (dataCollectionInfo.isCollectHostData()) {
            LogDataCollectionInfo logDataCollectionInfo = (LogDataCollectionInfo) dataCollectionInfo;
            Set<String> hosts = new HashSet<>((Collection<String>) dataCollectionDSLService.execute(
                logDataCollectionInfo.getHostCollectionDSL(), runtimeParameters,
                new ThirdPartyCallHandler(dataCollectionTask.getAccountId(), dataCollectionTask.getVerificationTaskId(),
                    delegateLogService)));
            hostRecordDataStoreService.save(dataCollectionTask.getAccountId(),
                dataCollectionTask.getVerificationTaskId(), dataCollectionTask.getStartTime(),
                dataCollectionTask.getEndTime(), hosts);
          }
          break;
        default:
          throw new IllegalArgumentException("Invalid type " + dataCollectionInfo.getVerificationType());
      }
      DataCollectionTaskResult result =
          DataCollectionTaskResult.builder().dataCollectionTaskId(dataCollectionTask.getUuid()).status(SUCCESS).build();
      cvNextGenServiceClient.updateTaskStatus(taskParams.getAccountId(), result).execute();
      logger.info("Updated task status to success.");

    } catch (Exception e) {
      logger.error("Perpetual task failed with exception", e);
      DataCollectionTaskResult result = DataCollectionTaskResult.builder()
                                            .dataCollectionTaskId(dataCollectionTask.getUuid())
                                            .status(FAILED)
                                            .exception(ExceptionUtils.getMessage(e))
                                            .stacktrace(ExceptionUtils.getStackTrace(e))
                                            .build();
      cvNextGenServiceClient.updateTaskStatus(taskParams.getAccountId(), result).execute();
    }
  }

  @Override
  public boolean cleanup(PerpetualTaskId taskId, PerpetualTaskExecutionParams params) {
    return true;
  }
}
