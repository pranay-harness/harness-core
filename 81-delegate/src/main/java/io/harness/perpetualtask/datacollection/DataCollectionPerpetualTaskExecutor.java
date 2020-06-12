package io.harness.perpetualtask.datacollection;

import io.harness.grpc.utils.AnyUtils;
import io.harness.perpetualtask.PerpetualTaskExecutionParams;
import io.harness.perpetualtask.PerpetualTaskExecutor;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskResponse;
import io.harness.perpetualtask.PerpetualTaskState;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
@Slf4j
public class DataCollectionPerpetualTaskExecutor implements PerpetualTaskExecutor {
  @Override
  public PerpetualTaskResponse runOnce(
      PerpetualTaskId taskId, PerpetualTaskExecutionParams params, Instant heartbeatTime) {
    DataCollectionPerpetualTaskParams sampleParams =
        AnyUtils.unpack(params.getCustomizedParams(), DataCollectionPerpetualTaskParams.class);
    logger.info("Hello there !! {} ", sampleParams.getCvConfigId());
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
