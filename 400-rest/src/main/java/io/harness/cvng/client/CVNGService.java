package io.harness.cvng.client;

import io.harness.cvng.beans.activity.ActivityDTO;
import io.harness.cvng.beans.activity.ActivityStatusDTO;
import io.harness.cvng.beans.activity.cd10.CD10RegisterActivityDTO;
import io.harness.cvng.beans.job.VerificationJobDTO;

public interface CVNGService {
  CD10RegisterActivityDTO registerActivity(String accountId, ActivityDTO activityDTO);
  ActivityStatusDTO getActivityStatus(String accountId, String activityId);
  VerificationJobDTO getVerificationJobs(String accountId, String webhookUrl);
}
