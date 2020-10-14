package io.harness.cvng.core.services.api;

import io.harness.cvng.beans.ActivityDTO;
import io.harness.cvng.core.activity.entities.Activity;
import io.harness.cvng.core.beans.DeploymentActivityResultDTO;
import io.harness.cvng.core.beans.DeploymentActivityVerificationResultDTO;

import java.util.List;

public interface ActivityService {
  void register(String accountId, String webhookToken, ActivityDTO activityDTO);

  List<DeploymentActivityVerificationResultDTO> getRecentDeploymentActivityVerifications(
      String accountId, String projectIdentifier);

  DeploymentActivityResultDTO getDeploymentActivityVerificationsByTag(
      String accountId, String projectIdentifier, String deploymentTag);

  Activity getActivityFromDTO(ActivityDTO activityDTO);

  String getDeploymentTagFromActivity(String accountId, String verificationJobInstanceId);

  String createActivity(Activity activity);
}
