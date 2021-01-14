package io.harness.cvng.activity.services.api;

import io.harness.cvng.activity.beans.ActivityDashboardDTO;
import io.harness.cvng.activity.beans.ActivityVerificationResultDTO;
import io.harness.cvng.activity.beans.DeploymentActivityPopoverResultDTO;
import io.harness.cvng.activity.beans.DeploymentActivityResultDTO;
import io.harness.cvng.activity.beans.DeploymentActivityResultDTO.DeploymentVerificationJobInstanceSummary;
import io.harness.cvng.activity.beans.DeploymentActivityVerificationResultDTO;
import io.harness.cvng.activity.entities.Activity;
import io.harness.cvng.beans.activity.ActivityDTO;
import io.harness.cvng.beans.activity.ActivityStatusDTO;
import io.harness.ng.core.dto.ResponseDTO;

import java.time.Instant;
import java.util.List;

public interface ActivityService {
  Activity get(String activityId);
  Activity getByVerificationJobInstanceId(String verificationJobInstanceId);
  String register(String accountId, String webhookToken, ActivityDTO activityDTO);
  String register(String accountId, ActivityDTO activityDTO);

  void updateActivityStatus(Activity activity);

  List<DeploymentActivityVerificationResultDTO> getRecentDeploymentActivityVerifications(
      String accountId, String orgIdentifier, String projectIdentifier);

  DeploymentActivityResultDTO getDeploymentActivityVerificationsByTag(
      String accountId, String orgIdentifier, String projectIdentifier, String serviceIdentifier, String deploymentTag);
  DeploymentActivityPopoverResultDTO getDeploymentActivityVerificationsPopoverSummary(
      String accountId, String orgIdentifier, String projectIdentifier, String serviceIdentifier, String deploymentTag);
  Activity getActivityFromDTO(ActivityDTO activityDTO);

  String getDeploymentTagFromActivity(String accountId, String verificationJobInstanceId);

  String createActivity(Activity activity);

  List<ActivityDashboardDTO> listActivitiesInTimeRange(String accountId, String orgIdentifier, String projectIdentifier,
      String environmentIdentifier, Instant startTime, Instant endTime);

  List<ActivityVerificationResultDTO> getRecentActivityVerificationResults(
      String accountId, String orgIdentifier, String projectIdentifier, int size);
  ActivityVerificationResultDTO getActivityVerificationResult(String accountId, String activityId);

  DeploymentVerificationJobInstanceSummary getDeploymentSummary(String activityId);

  ActivityStatusDTO getActivityStatus(String accountId, String activityId);
  List<String> createVerificationJobInstancesForActivity(Activity activity);
  ResponseDTO<List<String>> getActivityDetails(
      String accountId, String orgIdentifier, String projectIdentifier, String activityId);
}
