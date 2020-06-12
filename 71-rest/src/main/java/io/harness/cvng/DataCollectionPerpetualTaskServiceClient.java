package io.harness.cvng;

import com.google.protobuf.Message;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.TaskData;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskResponse;
import io.harness.perpetualtask.PerpetualTaskServiceClient;
import io.harness.perpetualtask.PerpetualTaskServiceInprocClient;
import io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.TaskType;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DataCollectionPerpetualTaskServiceClient
    implements PerpetualTaskServiceClient, PerpetualTaskServiceInprocClient<DataCollectionInfo> {
  @Override
  public String create(String accountId, DataCollectionInfo clientParams) {
    throw new UnsupportedOperationException("This is implemented in the service layer.");
  }

  @Override
  public Message getTaskParams(PerpetualTaskClientContext clientContext) {
    Map<String, String> clientParams = clientContext.getClientParams();
    String cvConfigId = clientParams.get("cvConfigId");
    return DataCollectionPerpetualTaskParams.newBuilder().setCvConfigId(cvConfigId).build();
  }

  @Override
  public void onTaskStateChange(
      String taskId, PerpetualTaskResponse newPerpetualTaskResponse, PerpetualTaskResponse oldPerpetualTaskResponse) {
    // ignore - no state change implementation needed for this.
  }

  @Override
  public DelegateTask getValidationTask(PerpetualTaskClientContext clientContext, String accountId) {
    logger.info("Get validation task {} {}", accountId, clientContext);
    Map<String, String> clientParams = clientContext.getClientParams();
    logger.info("Client params {}", clientParams);
    // TODO: move this to capability framework. For now the validation will always pass.
    return DelegateTask.builder()
        .accountId(accountId)
        .data(TaskData.builder()
                  .async(false)
                  .taskType(TaskType.DATA_COLLECTION_NEXT_GEN_VALIDATION.name())
                  .parameters(new Object[] {"test"})
                  .timeout(TimeUnit.MINUTES.toMillis(1))
                  .build())
        .build();
  }
}
