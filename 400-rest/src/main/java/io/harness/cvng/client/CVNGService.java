package io.harness.cvng.client;

import io.harness.cvng.beans.activity.ActivityDTO;
import io.harness.cvng.beans.activity.ActivityStatusDTO;

public interface CVNGService {
  String registerActivity(String accountId, ActivityDTO activityDTO);
  ActivityStatusDTO getActivityStatus(String accountId, String activityId);
}
