package io.harness.perpetualtask.datacollection;

import static io.harness.cvng.core.services.entities.DataCollectionTask.ExecutionStatus.SUCCESS;

import com.google.inject.Inject;

import io.harness.cvng.core.services.entities.DataCollectionTask.DataCollectionTaskResult;
import io.harness.cvng.perpetualtask.CVDataCollectionInfo;
import io.harness.grpc.utils.AnyUtils;
import io.harness.perpetualtask.PerpetualTaskExecutionParams;
import io.harness.perpetualtask.PerpetualTaskExecutor;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskResponse;
import io.harness.perpetualtask.PerpetualTaskState;
import io.harness.serializer.KryoUtils;
import io.harness.verificationclient.CVNextGenServiceClient;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Instant;
@Slf4j
public class DataCollectionPerpetualTaskExecutor implements PerpetualTaskExecutor {
  @Inject private CVNextGenServiceClient cvNextGenServiceClient;
  @Override
  public PerpetualTaskResponse runOnce(
      PerpetualTaskId taskId, PerpetualTaskExecutionParams params, Instant heartbeatTime) {
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
