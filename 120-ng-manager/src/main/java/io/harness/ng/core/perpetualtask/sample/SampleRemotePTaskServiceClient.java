package io.harness.ng.core.perpetualtask.sample;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.Message;

import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.http.HttpTaskParameters;
import io.harness.perpetualtask.PerpetualTaskResponse;
import io.harness.perpetualtask.RemotePerpetualTaskClientContext;
import io.harness.perpetualtask.example.SamplePerpetualTaskParams;
import io.harness.perpetualtask.remote.RemotePerpetualTaskServiceClient;
import io.harness.perpetualtask.remote.ValidationTaskDetails;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Singleton
@Slf4j
public class SampleRemotePTaskServiceClient implements RemotePerpetualTaskServiceClient {
  @Inject private SampleRemotePTaskManager sampleRemotePTaskManager;
  private static final String HTTP_URL_200 = "http://httpstat.us/200";

  private static final String COUNTRY_NAME = "countryName";
  @Override
  public Message getTaskParams(RemotePerpetualTaskClientContext clientContext) {
    Map<String, String> clientParams = clientContext.getTaskClientParamsMap();
    final String countryName = clientParams.get(COUNTRY_NAME);
    int population = sampleRemotePTaskManager.getPopulation(countryName);
    logger.info("task params created: Country = [{}], population = [{}]", countryName, population);
    return SamplePerpetualTaskParams.newBuilder().setCountry(countryName).setPopulation(population).build();
  }

  @Override
  public void onTaskStateChange(
      String taskId, PerpetualTaskResponse newPerpetualTaskResponse, PerpetualTaskResponse oldPerpetualTaskResponse) {
    logger.info("Nothing to do !. logging the responses taskId =[{}], newResponse = [{}], oldResponse = [{}]", taskId,
        newPerpetualTaskResponse, oldPerpetualTaskResponse);
  }

  @Override
  public ValidationTaskDetails getValidationTask(RemotePerpetualTaskClientContext clientContext, String accountId) {
    Map<String, String> setupAbstractions = ImmutableMap.of("accountId", accountId);
    TaskData taskData =
        TaskData.builder()
            .async(false)
            .taskType("HTTP")
            .timeout(TimeUnit.MINUTES.toMillis(1))
            .parameters(new Object[] {HttpTaskParameters.builder().method("GET").url(HTTP_URL_200).build()})
            .build();
    logger.info("validation task created for task");
    return ValidationTaskDetails.builder().setupAbstractions(setupAbstractions).taskData(taskData).build();
  }
}
