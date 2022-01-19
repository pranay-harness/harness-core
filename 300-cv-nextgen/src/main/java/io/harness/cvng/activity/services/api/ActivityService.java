/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.activity.services.api;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.activity.beans.ActivityVerificationResultDTO;
import io.harness.cvng.activity.beans.DeploymentActivitySummaryDTO;
import io.harness.cvng.activity.entities.Activity;
import io.harness.cvng.activity.entities.DeploymentActivity;
import io.harness.cvng.analysis.beans.LogAnalysisClusterChartDTO;
import io.harness.cvng.analysis.beans.LogAnalysisClusterDTO;
import io.harness.cvng.analysis.beans.TransactionMetricInfoSummaryPageDTO;
import io.harness.cvng.beans.activity.ActivityDTO;
import io.harness.cvng.beans.activity.ActivityStatusDTO;
import io.harness.cvng.beans.activity.ActivityType;
import io.harness.cvng.beans.activity.ActivityVerificationStatus;
import io.harness.cvng.core.beans.DatasourceTypeDTO;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.HealthSourceDTO;
import io.harness.cvng.core.beans.params.PageParams;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.beans.params.filterParams.DeploymentLogAnalysisFilter;
import io.harness.cvng.core.beans.params.filterParams.DeploymentTimeSeriesAnalysisFilter;
import io.harness.ng.beans.PageResponse;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@OwnedBy(HarnessTeam.CV)
public interface ActivityService {
  Activity get(String activityId);
  Activity getByVerificationJobInstanceId(String verificationJobInstanceId);
  @Deprecated String register(String accountId, ActivityDTO activityDTO);
  @Deprecated String register(Activity activity);

  void updateActivityStatus(Activity activity);

  Activity getActivityFromDTO(ActivityDTO activityDTO);

  String getDeploymentTagFromActivity(String accountId, String verificationJobInstanceId);

  String createActivity(Activity activity);

  List<ActivityVerificationResultDTO> getRecentActivityVerificationResults(
      String accountId, String orgIdentifier, String projectIdentifier, int size);
  ActivityVerificationResultDTO getActivityVerificationResult(String accountId, String activityId);

  @Deprecated DeploymentActivitySummaryDTO getDeploymentSummary(String activityId);

  ActivityStatusDTO getActivityStatus(String accountId, String activityId);
  List<String> createVerificationJobInstancesForActivity(Activity activity);

  @Deprecated
  TransactionMetricInfoSummaryPageDTO getDeploymentActivityTimeSeriesData(String accountId, String activityId,
      DeploymentTimeSeriesAnalysisFilter deploymentTimeSeriesAnalysisFilter, PageParams pageParams);
  Set<DatasourceTypeDTO> getDataSourcetypes(String accountId, String activityId);

  @Deprecated
  List<LogAnalysisClusterChartDTO> getDeploymentActivityLogAnalysisClusters(
      String accountId, String activityId, DeploymentLogAnalysisFilter deploymentLogAnalysisFilter);

  @Deprecated
  PageResponse<LogAnalysisClusterDTO> getDeploymentActivityLogAnalysisResult(String accountId, String activityId,
      Integer label, DeploymentLogAnalysisFilter deploymentLogAnalysisFilter, PageParams pageParams);

  void abort(String activityId);

  @Deprecated Set<HealthSourceDTO> healthSources(String accountId, String activityId);

  String upsert(Activity activity);

  List<Activity> get(ServiceEnvironmentParams serviceEnvironmentParams, List<String> changeSourceIdentifiers,
      Instant startTime, Instant endTime, List<ActivityType> activityTypes);

  Long getCount(ServiceEnvironmentParams serviceEnvironmentParams, List<String> changeSourceIdentifiers,
      Instant startTime, Instant endTime, List<ActivityType> activityTypes);

  Long getCount(ProjectParams projectParams, List<String> serviceIdentifiers, List<String> environmentIdentifiers,
      Instant startTime, Instant endTime, List<ActivityType> activityTypes);
  @Deprecated String createActivityForDemo(DeploymentActivity activity, ActivityVerificationStatus verificationStatus);
  List<DeploymentActivity> getDemoDeploymentActivity(
      ServiceEnvironmentParams serviceEnvironmentParams, Instant startTime, Instant endTime);
}
