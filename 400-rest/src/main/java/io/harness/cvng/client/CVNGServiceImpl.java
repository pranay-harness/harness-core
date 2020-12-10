package io.harness.cvng.client;

import io.harness.cvng.beans.activity.ActivityDTO;
import io.harness.cvng.beans.activity.ActivityStatusDTO;
import io.harness.cvng.state.CVNGRequestExecutor;

import com.google.inject.Inject;

public class CVNGServiceImpl implements CVNGService {
  @Inject private CVNGRequestExecutor requestExecutor;
  @Inject private CVNGServiceClient cvngServiceClient;

  @Override
  public String registerActivity(String accountId, ActivityDTO activityDTO) {
    return requestExecutor.execute(cvngServiceClient.registerActivity(accountId, activityDTO)).getResource();
  }

  @Override
  public ActivityStatusDTO getActivityStatus(String accountId, String activityId) {
    return requestExecutor.execute(cvngServiceClient.getActivityStatus(accountId, activityId)).getResource();
  }
}
