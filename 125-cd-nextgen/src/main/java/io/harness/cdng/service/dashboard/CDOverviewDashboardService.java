package io.harness.cdng.service.dashboard;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.Deployment.DashboardDeploymentActiveFailedRunningInfo;
import io.harness.cdng.Deployment.DashboardWorkloadDeployment;
import io.harness.cdng.Deployment.ExecutionDeploymentInfo;
import io.harness.cdng.Deployment.HealthDeploymentDashboard;
import io.harness.cdng.Deployment.ServiceDeploymentInfoDTO;
import io.harness.cdng.Deployment.ServiceDeploymentListInfo;
import io.harness.cdng.Deployment.ServiceDetailsInfoDTO;
import io.harness.cdng.Deployment.TimeValuePairListDTO;
import io.harness.ng.core.activityhistory.dto.TimeGroupType;
import io.harness.ng.core.environment.beans.EnvironmentType;

@OwnedBy(HarnessTeam.CDC)
public interface CDOverviewDashboardService {
  HealthDeploymentDashboard getHealthDeploymentDashboard(String accountId, String orgId, String projectId,
      long startInterval, long endInterval, long previousStartInterval);

  ExecutionDeploymentInfo getExecutionDeploymentDashboard(
      String accountId, String orgId, String projectId, long startInterval, long endInterval);

  DashboardDeploymentActiveFailedRunningInfo getDeploymentActiveFailedRunningInfo(
      String accountId, String orgId, String projectId, long days);

  DashboardWorkloadDeployment getDashboardWorkloadDeployment(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, long startInterval, long endInterval, long previousStartInterval,
      EnvironmentType envType);

  ServiceDeploymentListInfo getServiceDeploymentsInfo(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, long startTime, long endTime, String serviceIdentifier, long bucketSizeInDays)
      throws Exception;

  ServiceDeploymentInfoDTO getServiceDeployments(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, long startTime, long endTime, String serviceIdentifier, long bucketSizeInDays);

  ServiceDetailsInfoDTO getServiceDetailsList(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      long startTime, long endTime) throws Exception;

  TimeValuePairListDTO<Integer> getServicesGrowthTrend(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, long startTimeInMs, long endTimeInMs, TimeGroupType timeGroupType);
}
