package io.harness.perpetualtask.example;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.http.HttpTaskParameters;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskResponse;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskServiceClient;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.TaskType;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SamplePerpetualTaskServiceClient implements PerpetualTaskServiceClient {
  @Inject private PerpetualTaskService perpetualTaskService;

  private static final String COUNTRY_NAME = "countryName";

  @Override
  public SamplePerpetualTaskParams getTaskParams(PerpetualTaskClientContext clientContext) {
    Map<String, String> clientParams = clientContext.getClientParams();
    return SamplePerpetualTaskParams.newBuilder().setCountry(clientParams.get(COUNTRY_NAME)).build();
  }

  @Override
  public void onTaskStateChange(
      String taskId, PerpetualTaskResponse newPerpetualTaskResponse, PerpetualTaskResponse oldPerpetualTaskResponse) {
    logger.debug("Nothing to do !!");
  }

  @Override
  public DelegateTask getValidationTask(PerpetualTaskClientContext context, String accountId) {
    return DelegateTask.builder()
        .accountId(accountId)
        .data(TaskData.builder()
                  .async(false)
                  .taskType(TaskType.HTTP.name())
                  .parameters(new Object[] {HttpTaskParameters.builder().url("https://www.google.com").build()})
                  .timeout(TimeUnit.MINUTES.toMillis(1))
                  .build())
        .build();
  }
}