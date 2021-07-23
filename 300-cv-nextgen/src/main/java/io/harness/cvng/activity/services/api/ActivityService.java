package io.harness.cvng.activity.services.api;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.activity.beans.ActivityDashboardDTO;
import io.harness.cvng.activity.beans.ActivityVerificationResultDTO;
import io.harness.cvng.activity.beans.DeploymentActivityPopoverResultDTO;
import io.harness.cvng.activity.beans.DeploymentActivityResultDTO;
import io.harness.cvng.activity.beans.DeploymentActivitySummaryDTO;
import io.harness.cvng.activity.beans.DeploymentActivityVerificationResultDTO;
import io.harness.cvng.activity.entities.Activity;
import io.harness.cvng.analysis.beans.TransactionMetricInfoSummaryPageDTO;
import io.harness.cvng.beans.activity.ActivityDTO;
import io.harness.cvng.beans.activity.ActivityStatusDTO;
import io.harness.cvng.core.beans.DatasourceTypeDTO;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@OwnedBy(HarnessTeam.CV)
public interface ActivityService {
  Activity get(String activityId);
  Activity getByVerificationJobInstanceId(String verificationJobInstanceId);
  String register(String accountId, String webhookToken, ActivityDTO activityDTO);

  String register(String accountId, ActivityDTO activityDTO);
  String register(Activity activity);

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
      String environmentIdentifier, String serviceIdentifier, Instant startTime, Instant endTime);

  List<ActivityVerificationResultDTO> getRecentActivityVerificationResults(
      String accountId, String orgIdentifier, String projectIdentifier, int size);
  ActivityVerificationResultDTO getActivityVerificationResult(String accountId, String activityId);

  DeploymentActivitySummaryDTO getDeploymentSummary(String activityId);

  ActivityStatusDTO getActivityStatus(String accountId, String activityId);
  List<String> createVerificationJobInstancesForActivity(Activity activity);
  TransactionMetricInfoSummaryPageDTO getDeploymentActivityTimeSeriesData(String accountId, String activityId,
      boolean anomalousMetricsOnly, String hostName, String filter, int pageNumber, int pageSize);
  Set<DatasourceTypeDTO> getDataSourcetypes(String accountId, String activityId);
}
