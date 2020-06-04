package io.harness.perpetualtask.example;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskResponse;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskServiceClient;
import io.harness.perpetualtask.PerpetualTaskServiceInprocClient;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class SamplePerpetualTaskServiceClient
    implements PerpetualTaskServiceClient, PerpetualTaskServiceInprocClient<SamplePerpetualTaskClientParams> {
  @Inject private PerpetualTaskService perpetualTaskService;

  private static final String COUNTRY_NAME = "countryName";

  @Override
  public String create(String accountId, SamplePerpetualTaskClientParams clientParams) {
    return null;
  }

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
    // TODO: implement this
    return null;
  }
}