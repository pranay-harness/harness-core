package io.harness.perpetualtask.datacollection;

import static io.harness.cvng.beans.ExecutionStatus.SUCCESS;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.cvng.beans.Connector;
import io.harness.cvng.beans.DataCollectionInfo;
import io.harness.cvng.beans.DataCollectionTaskDTO;
import io.harness.cvng.beans.DataCollectionTaskDTO.DataCollectionTaskResult;
import io.harness.cvng.perpetualtask.CVDataCollectionInfo;
import io.harness.datacollection.DataCollectionDSLService;
import io.harness.datacollection.entity.RuntimeParameters;
import io.harness.datacollection.entity.TimeSeriesRecord;
import io.harness.delegate.service.TimeSeriesDataStoreService;
import io.harness.grpc.utils.AnyUtils;
import io.harness.perpetualtask.PerpetualTaskExecutionParams;
import io.harness.perpetualtask.PerpetualTaskExecutor;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskResponse;
import io.harness.perpetualtask.PerpetualTaskState;
import io.harness.serializer.KryoUtils;
import io.harness.verificationclient.CVNextGenServiceClient;
import lombok.extern.slf4j.Slf4j;
import software.wings.annotation.EncryptableSetting;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.settings.SettingValue;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import javax.annotation.Nullable;

@Slf4j
public class DataCollectionPerpetualTaskExecutor implements PerpetualTaskExecutor {
  @Nullable @Inject private CVNextGenServiceClient cvNextGenServiceClient;
  @Inject private EncryptionService encryptionService;

  @Inject private DelegateLogService delegateLogService;
  @Inject private TimeSeriesDataStoreService timeSeriesDataStoreService;

  @Inject private DataCollectionDSLService dataCollectionDSLService;
  @Inject @Named("verificationDataCollectorExecutor") protected ExecutorService dataCollectionService;

  @Override
  public PerpetualTaskResponse runOnce(
      PerpetualTaskId taskId, PerpetualTaskExecutionParams params, Instant heartbeatTime) {
    if (cvNextGenServiceClient == null) {
      return PerpetualTaskResponse.builder()
          .perpetualTaskState(PerpetualTaskState.TASK_RUN_FAILED)
          .responseMessage("The CVNextGenServiceClient is not initialize")
          .build();
    }

    DataCollectionPerpetualTaskParams sampleParams =
        AnyUtils.unpack(params.getCustomizedParams(), DataCollectionPerpetualTaskParams.class);
    logger.info("Hello there !! {} ", sampleParams.getCvConfigId());
    CVDataCollectionInfo cvDataCollectionInfo =
        (CVDataCollectionInfo) KryoUtils.asObject(sampleParams.getDataCollectionInfo().toByteArray());
    logger.info("DataCollectionInfo {} ", cvDataCollectionInfo);
    try {
      DataCollectionTaskDTO dataCollectionTask =
          cvNextGenServiceClient.getNextDataCollectionTask(sampleParams.getAccountId(), sampleParams.getCvConfigId())
              .execute()
              .body()
              .getResource();
      logger.info("Next task to process: ", dataCollectionTask);
      SettingValue settingValue = cvDataCollectionInfo.getSettingValue();
      if (settingValue instanceof EncryptableSetting) {
        encryptionService.decrypt((EncryptableSetting) settingValue, cvDataCollectionInfo.getEncryptedDataDetails());
      }
      Connector connector = (Connector) settingValue;
      final String cvConfigId = dataCollectionTask.getCvConfigId();
      DataCollectionInfo dataCollectionInfo = dataCollectionTask.getDataCollectionInfo();
      dataCollectionDSLService.registerDatacollectionExecutorService(dataCollectionService);
      final RuntimeParameters runtimeParameters = RuntimeParameters.builder()
                                                      .baseUrl(connector.getBaseUrl())
                                                      .commonHeaders(connector.collectionHeaders())
                                                      .commonOptions(connector.collectionParams())
                                                      .otherEnvVariables(dataCollectionInfo.getDslEnvVariables())
                                                      .endTime(dataCollectionTask.getEndTime())
                                                      .startTime(dataCollectionTask.getStartTime())
                                                      .build();
      switch (dataCollectionInfo.getVerificationType()) {
        case TIME_SERIES:
          List<TimeSeriesRecord> timeSeriesRecords = (List<TimeSeriesRecord>) dataCollectionDSLService.execute(
              dataCollectionInfo.getDataCollectionDsl(), runtimeParameters,
              new ThirdPartyCallHandler(
                  connector.getAccountId(), dataCollectionTask.getCvConfigId(), delegateLogService));
          timeSeriesDataStoreService.saveTimeSeriesDataRecords(connector.getAccountId(), cvConfigId, timeSeriesRecords);
          break;
        case LOG:
          // TODO: implement log
          break;
        default:
          throw new IllegalArgumentException("Invalid type " + dataCollectionInfo.getVerificationType());
      }
      DataCollectionTaskResult result = DataCollectionTaskDTO.DataCollectionTaskResult.builder()
                                            .dataCollectionTaskId(dataCollectionTask.getUuid())
                                            .status(SUCCESS)
                                            .build();
      cvNextGenServiceClient.updateTaskStatus(sampleParams.getAccountId(), result).execute();
      logger.info("Updated task status to success.");
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    return PerpetualTaskResponse.builder()
        .responseCode(200)
        .perpetualTaskState(PerpetualTaskState.TASK_RUN_SUCCEEDED)
        .responseMessage(PerpetualTaskState.TASK_RUN_SUCCEEDED.name())
        .build();
  }

  @Override
  public boolean cleanup(PerpetualTaskId taskId, PerpetualTaskExecutionParams params) {
    return true;
  }
}
