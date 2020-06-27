package io.harness.perpetualtask.datacollection;

import static io.harness.cvng.core.services.entities.DataCollectionTask.ExecutionStatus.SUCCESS;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.cvng.beans.Connector;
import io.harness.cvng.core.services.entities.CVConfig;
import io.harness.cvng.core.services.entities.DataCollectionTask.DataCollectionTaskResult;
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
import java.time.temporal.ChronoUnit;
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
      io.harness.cvng.core.services.entities.DataCollectionTask dataCollectionTask =
          cvNextGenServiceClient.getNextDataCollectionTask(sampleParams.getAccountId(), sampleParams.getCvConfigId())
              .execute()
              .body()
              .getResource();
      logger.info("Next task to process: ", dataCollectionTask);
      final CVConfig cvConfig = cvDataCollectionInfo.getCvConfig();
      SettingValue settingValue = cvDataCollectionInfo.getSettingValue();
      if (settingValue instanceof EncryptableSetting) {
        encryptionService.decrypt((EncryptableSetting) settingValue, cvDataCollectionInfo.getEncryptedDataDetails());
      }
      Connector connector = (Connector) settingValue;
      final String cvConfigId = cvConfig.getUuid();
      dataCollectionDSLService.registerDatacollectionExecutorService(dataCollectionService);
      final RuntimeParameters runtimeParameters = RuntimeParameters.builder()
                                                      .baseUrl(connector.getBaseUrl())
                                                      .commonHeaders(connector.collectionHeaders())
                                                      .commonOptions(connector.collectionParams())
                                                      .otherEnvVariables(cvConfig.getDslEnvVariables())
                                                      .endTime(Instant.now().minus(2, ChronoUnit.MINUTES))
                                                      .startTime(Instant.now().minus(7, ChronoUnit.MINUTES))
                                                      .build();

      switch (cvConfig.getVerificationType()) {
        case TIME_SERIES:
          List<TimeSeriesRecord> timeSeriesRecords = (List<TimeSeriesRecord>) dataCollectionDSLService.execute(
              cvConfig.getDataCollectionDsl(), runtimeParameters,
              new ThirdPartyCallHandler(connector.getAccountId(), cvConfig.getUuid(), delegateLogService));
          timeSeriesDataStoreService.saveTimeSeriesDataRecords(connector.getAccountId(), cvConfigId, timeSeriesRecords);
          break;
        case LOG:
          // TODO: implement log
          break;
        default:
          throw new IllegalArgumentException("Invalid type " + cvConfig.getVerificationType());
      }
      DataCollectionTaskResult result =
          DataCollectionTaskResult.builder().dataCollectionTaskId(dataCollectionTask.getUuid()).status(SUCCESS).build();
      cvNextGenServiceClient.updateTaskStatus(sampleParams.getAccountId(), result);
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
