package io.harness.ng.cdOverview.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.models.dashboard.InstanceCountDetailsByEnvTypeAndServiceId;
import io.harness.ng.cdOverview.dto.ActiveServiceInstanceSummary;
import io.harness.ng.cdOverview.dto.DashboardWorkloadDeployment;
import io.harness.ng.cdOverview.dto.EnvBuildIdAndInstanceCountInfoList;
import io.harness.ng.cdOverview.dto.EnvIdCountPair;
import io.harness.ng.cdOverview.dto.ExecutionDeploymentInfo;
import io.harness.ng.cdOverview.dto.HealthDeploymentDashboard;
import io.harness.ng.cdOverview.dto.InstancesByBuildIdList;
import io.harness.ng.cdOverview.dto.ServiceDeploymentInfoDTO;
import io.harness.ng.cdOverview.dto.ServiceDeploymentListInfo;
import io.harness.ng.cdOverview.dto.ServiceDetailsInfoDTO;
import io.harness.ng.cdOverview.dto.ServiceHeaderInfo;
import io.harness.ng.cdOverview.dto.TimeValuePairListDTO;
import io.harness.ng.core.activityhistory.dto.TimeGroupType;
import io.harness.ng.core.dashboard.DashboardExecutionStatusInfo;
import io.harness.ng.core.dashboard.DeploymentsInfo;
import io.harness.ng.core.environment.beans.EnvironmentType;

import java.util.List;

@OwnedBy(HarnessTeam.CDC)
public interface CDOverviewDashboardService {
  HealthDeploymentDashboard getHealthDeploymentDashboard(String accountId, String orgId, String projectId,
      long startInterval, long endInterval, long previousStartInterval);

  ExecutionDeploymentInfo getExecutionDeploymentDashboard(
      String accountId, String orgId, String projectId, long startInterval, long endInterval);

  DashboardExecutionStatusInfo getDeploymentActiveFailedRunningInfo(
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

  InstanceCountDetailsByEnvTypeAndServiceId getActiveServiceInstanceCountBreakdown(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, List<String> serviceId);

  ActiveServiceInstanceSummary getActiveServiceInstanceSummary(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String serviceId, long timestampInMs);

  EnvBuildIdAndInstanceCountInfoList getEnvBuildInstanceCountByServiceId(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String serviceId);

  InstancesByBuildIdList getActiveInstancesByServiceIdEnvIdAndBuildIds(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String serviceId, String envId, List<String> buildIds);

  TimeValuePairListDTO<Integer> getInstanceGrowthTrend(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String serviceId, long startTimeInMs, long endTimeInMs);

  TimeValuePairListDTO<EnvIdCountPair> getInstanceCountHistory(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String serviceId, long startTimeInMs, long endTimeInMs);

  DeploymentsInfo getDeploymentsByServiceId(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String serviceId, long startTimeInMs, long endTimeInMs);

  ServiceHeaderInfo getServiceHeaderInfo(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String serviceId);
}
