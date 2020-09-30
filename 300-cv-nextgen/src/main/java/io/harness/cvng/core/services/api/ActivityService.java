package io.harness.cvng.core.services.api;

import io.harness.cvng.core.beans.ActivityDTO;
import io.harness.cvng.core.beans.DeploymentActivityVerificationResultDTO;

import java.util.List;

public interface ActivityService {
  void register(String accountId, String webhookToken, ActivityDTO activityDTO);

  List<DeploymentActivityVerificationResultDTO> getRecentDeploymentActivityVerifications(
      String accountId, String projectIdentifier);
}
