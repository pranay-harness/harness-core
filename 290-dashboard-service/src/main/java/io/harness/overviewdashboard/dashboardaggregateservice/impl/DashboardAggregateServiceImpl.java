package io.harness.overviewdashboard.dashboardaggregateservice.impl;

import static io.harness.dashboards.SortBy.DEPLOYMENTS;
import static io.harness.remote.client.NGRestUtils.getResponse;

import io.harness.dashboards.*;
import io.harness.dashboards.ServiceDashboardInfo;
import io.harness.ng.core.OrgProjectIdentifier;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.overviewdashboard.bean.OverviewDashboardRequestType;
import io.harness.overviewdashboard.bean.RestCallRequest;
import io.harness.overviewdashboard.bean.RestCallResponse;
import io.harness.overviewdashboard.dashboardaggregateservice.service.DashboardAggregateService;
import io.harness.overviewdashboard.dtos.*;
import io.harness.overviewdashboard.dtos.ActiveServiceInfo;
import io.harness.overviewdashboard.rbac.service.DashboardRBACService;
import io.harness.overviewdashboard.remote.ParallelRestCallExecutor;
import io.harness.pipeline.dashboards.PMSLandingDashboardResourceClient;
import io.harness.pms.dashboards.PipelinesCount;
import io.harness.user.remote.UserClient;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import dashboards.CDLandingDashboardResourceClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DashboardAggregateServiceImpl implements DashboardAggregateService {
  @Inject  DashboardRBACService dashboardRBACService;
  @Inject  CDLandingDashboardResourceClient cdLandingDashboardResourceClient;
  @Inject  PMSLandingDashboardResourceClient pmsLandingDashboardResourceClient;
  @Inject ParallelRestCallExecutor parallelRestCallExecutor;

  @Override
  public TopProjectsPanel getTopProjectsPanel(String accountId, String userId, long startInterval, long endInterval) {
    List<OrgProjectIdentifier> orgProjectIdentifierList = getOrgProjectIdentifier(accountId, userId);
    List<RestCallRequest> restCallRequestList = new ArrayList<>();
    restCallRequestList.add(RestCallRequest.<ProjectsDashboardInfo>builder()
                                .request(cdLandingDashboardResourceClient.getTopProjects(
                                    accountId, orgProjectIdentifierList, startInterval, endInterval))
                                .requestType(OverviewDashboardRequestType.GET_PROJECT_LIST)
                                .build());
    List<RestCallResponse> restCallResponses = parallelRestCallExecutor.executeRestCalls(restCallRequestList);
    Optional<RestCallResponse> projectsDashBoardInfoOptional =
        restCallResponses.stream()
            .filter(k -> k.getRequestType() == OverviewDashboardRequestType.GET_PROJECT_LIST)
            .findAny();
    if (projectsDashBoardInfoOptional.isPresent()) {
      ProjectsDashboardInfo projectsDashBoardInfo =
          (ProjectsDashboardInfo) projectsDashBoardInfoOptional.get().getResponse();
      List<TopProjectsDashboardInfo<CountWithSuccessFailureDetails>> cdTopProjectsInfoList = new ArrayList<>();
      for (ProjectDashBoardInfo projectDashBoardInfo : projectsDashBoardInfo.getProjectDashBoardInfoList()) {
        cdTopProjectsInfoList.add(
            TopProjectsDashboardInfo.<CountWithSuccessFailureDetails>builder()
                .accountInfo(AccountInfo.builder().accountIdentifier(projectDashBoardInfo.getAccountId()).build())
                .orgInfo(OrgInfo.builder().orgIdentifier(projectDashBoardInfo.getOrgIdentifier()).build())
                .projectInfo(
                    ProjectInfo.builder().projectIdentifier(projectDashBoardInfo.getProjectIdentifier()).build())
                .countDetails(
                    CountWithSuccessFailureDetails.builder()
                        .count(projectDashBoardInfo.getDeploymentsCount())
                        .countChangeAndCountChangeRateInfo(CountChangeAndCountChangeRateInfo.builder().build())
                        .build())
                .build());
      }
      return TopProjectsPanel.builder().CDTopProjectsInfo(cdTopProjectsInfoList).build();
    }
    return TopProjectsPanel.builder().build();
  }

  @Override
  public DeploymentsStatsOverview getDeploymentStatsSummary(
      String accountId, String userId, long startInterval, long endInterval, GroupBy groupBy, SortBy sortBy) {
    List<OrgProjectIdentifier> orgProjectIdentifierList = getOrgProjectIdentifier(accountId, userId);
    List<RestCallRequest> restCallRequestList = new ArrayList<>();
    restCallRequestList.add(RestCallRequest.<DeploymentStatsSummary>builder()
                                .request(cdLandingDashboardResourceClient.getDeploymentStatsSummary(
                                    accountId, orgProjectIdentifierList, startInterval, endInterval))
                                .requestType(OverviewDashboardRequestType.GET_DEPLOYMENTS_STATS_SUMMARY)
                                .build());
    restCallRequestList.add(RestCallRequest.<List<TimeBasedDeploymentInfo>>builder()
                                .request(cdLandingDashboardResourceClient.getTimeWiseDeploymentInfo(
                                    accountId, orgProjectIdentifierList, startInterval, endInterval, groupBy))
                                .requestType(OverviewDashboardRequestType.GET_TIME_WISE_DEPLOYMENT_INFO)
                                .build());
    restCallRequestList.add(RestCallRequest.<ServicesDashboardInfo>builder()
                                .request(cdLandingDashboardResourceClient.get(
                                    accountId, orgProjectIdentifierList, startInterval, endInterval, sortBy))
                                .requestType(OverviewDashboardRequestType.GET_MOST_ACTIVE_SERVICES)
                                .build());
    List<RestCallResponse> restCallResponses = parallelRestCallExecutor.executeRestCalls(restCallRequestList);
    Optional<RestCallResponse> deploymentStatsSummaryOptional =
        restCallResponses.stream()
            .filter(k -> k.getRequestType() == OverviewDashboardRequestType.GET_DEPLOYMENTS_STATS_SUMMARY)
            .findAny();
    Optional<RestCallResponse> timeBasedDeploymentsInfoOptional =
        restCallResponses.stream()
            .filter(k -> k.getRequestType() == OverviewDashboardRequestType.GET_TIME_WISE_DEPLOYMENT_INFO)
            .findAny();
    Optional<RestCallResponse> mostActiveServicesOptional =
        restCallResponses.stream()
            .filter(k -> k.getRequestType() == OverviewDashboardRequestType.GET_MOST_ACTIVE_SERVICES)
            .findAny();
    if (deploymentStatsSummaryOptional.isPresent() && timeBasedDeploymentsInfoOptional.isPresent()
        && mostActiveServicesOptional.isPresent()) {
      DeploymentStatsSummary deploymentStatsSummary =
          (DeploymentStatsSummary) deploymentStatsSummaryOptional.get().getResponse();
      List<TimeBasedDeploymentInfo> timeBasedDeploymentInfoList =
          (List<TimeBasedDeploymentInfo>) timeBasedDeploymentsInfoOptional.get().getResponse();
      ServicesDashboardInfo servicesDashboardInfo =
          (ServicesDashboardInfo) mostActiveServicesOptional.get().getResponse();
      return DeploymentsStatsOverview.builder()
          .deploymentsStatsSummary(
              DeploymentsStatsSummary.builder()
                  .countAndChangeRate(CountChangeDetails.builder()
                                          .count(deploymentStatsSummary.getCount())
                                          .countChangeAndCountChangeRateInfo(
                                              CountChangeAndCountChangeRateInfo.builder()
                                                  .countChangeRate(deploymentStatsSummary.getCountChangeRate())
                                                  .build())
                                          .build())
                  .failureCountAndChangeRate(CountChangeDetails.builder()
                                                 .count(deploymentStatsSummary.getFailureCount())
                                                 .countChangeAndCountChangeRateInfo(
                                                     CountChangeAndCountChangeRateInfo.builder()
                                                         .countChangeRate(deploymentStatsSummary.getFailureChangeRate())
                                                         .build())
                                                 .build())
                  .failureRateAndChangeRate(RateAndRateChangeInfo.builder()
                                                .rate(deploymentStatsSummary.getFailureRate())
                                                .rateChangeRate(deploymentStatsSummary.getFailureRateChangeRate())
                                                .build())
                  .deploymentRateAndChangeRate(RateAndRateChangeInfo.builder()
                                                   .rate(deploymentStatsSummary.getDeploymentRate())
                                                   .rateChangeRate(deploymentStatsSummary.getDeploymentRateChangeRate())
                                                   .build())
                  .deploymentsOverview(
                      DeploymentsOverview.builder()
                          .failedCount(deploymentStatsSummary.getFailed24HoursCount())
                          .manualInterventionsCount(deploymentStatsSummary.getManualInterventionsCount())
                          .pendingApprovalsCount(deploymentStatsSummary.getPendingApprovalsCount())
                          .runningCount(deploymentStatsSummary.getRunningCount())
                          .build())
                  .deploymentStats(getTimeWiseDeploymentInfo(
                      accountId, userId, startInterval, endInterval, groupBy, timeBasedDeploymentInfoList))
                  .build())
          .mostActiveServicesList(
              getMostActiveServicesList(accountId, userId, startInterval, endInterval, sortBy, servicesDashboardInfo))
          .build();
    }
    return DeploymentsStatsOverview.builder().build();
  }

  @Override
  public CountOverview getCountOverview(String accountId, String userId, long startInterval, long endInterval) {
    List<OrgProjectIdentifier> orgProjectIdentifierList = getOrgProjectIdentifier(accountId, userId);
    List<RestCallRequest> restCallRequestList = new ArrayList<>();
    restCallRequestList.add(RestCallRequest.<ServicesCount>builder()
                                .request(cdLandingDashboardResourceClient.getServicesCount(
                                    accountId, orgProjectIdentifierList, startInterval, endInterval))
                                .requestType(OverviewDashboardRequestType.GET_SERVICES_COUNT)
                                .build());
    restCallRequestList.add(RestCallRequest.<EnvCount>builder()
                                .request(cdLandingDashboardResourceClient.getEnvCount(
                                    accountId, orgProjectIdentifierList, startInterval, endInterval))
                                .requestType(OverviewDashboardRequestType.GET_ENV_COUNT)
                                .build());
    restCallRequestList.add(RestCallRequest.<PipelinesCount>builder()
                                .request(pmsLandingDashboardResourceClient.getPipelinesCount(
                                    accountId, orgProjectIdentifierList, startInterval, endInterval))
                                .requestType(OverviewDashboardRequestType.GET_PIPELINES_COUNT)
                                .build());
    List<RestCallResponse> restCallResponses = parallelRestCallExecutor.executeRestCalls(restCallRequestList);
    Optional<RestCallResponse> servicesCountOptional =
        restCallResponses.stream()
            .filter(k -> k.getRequestType() == OverviewDashboardRequestType.GET_SERVICES_COUNT)
            .findAny();
    Optional<RestCallResponse> envCountOptional =
        restCallResponses.stream()
            .filter(k -> k.getRequestType() == OverviewDashboardRequestType.GET_ENV_COUNT)
            .findAny();
    Optional<RestCallResponse> pipelinesCountOptional =
        restCallResponses.stream()
            .filter(k -> k.getRequestType() == OverviewDashboardRequestType.GET_PIPELINES_COUNT)
            .findAny();
    if (servicesCountOptional.isPresent() && envCountOptional.isPresent() && pipelinesCountOptional.isPresent()) {
      ServicesCount servicesCount = (ServicesCount) servicesCountOptional.get().getResponse();
      EnvCount envCount = (EnvCount) envCountOptional.get().getResponse();
      PipelinesCount pipelinesCount = (PipelinesCount) pipelinesCountOptional.get().getResponse();
      return CountOverview.builder()
          .servicesCountDetail(getServicesCount(servicesCount))
          .envCountDetail(getEnvCount(envCount))
          .pipelinesCountDetail(getPipelinesCount(pipelinesCount))
          .build();
    }
    return CountOverview.builder().build();
  }

  @VisibleForTesting
  public List<TimeBasedStats> getTimeWiseDeploymentInfo(String accountId, String userId, long startInterval,
      long endInterval, GroupBy groupBy, List<TimeBasedDeploymentInfo> timeBasedDeploymentInfoList) {
    List<OrgProjectIdentifier> orgProjectIdentifierList = getOrgProjectIdentifier(accountId, userId);
    List<TimeBasedStats> timeBasedStatsList = new ArrayList<>();
    for (TimeBasedDeploymentInfo timeBasedDeploymentInfo : timeBasedDeploymentInfoList) {
      timeBasedStatsList.add(
          TimeBasedStats.builder()
              .time(timeBasedDeploymentInfo.getTime())
              .countWithSuccessFailureDetails(CountWithSuccessFailureDetails.builder()
                                                  .count(timeBasedDeploymentInfo.getCount())
                                                  .failureCount(timeBasedDeploymentInfo.getFailedCount())
                                                  .successCount(timeBasedDeploymentInfo.getSuccessCount())
                                                  .build())
              .build());
    }
    return timeBasedStatsList;
  }

  @VisibleForTesting
  public MostActiveServicesList getMostActiveServicesList(String accountId, String userId, long startInterval,
      long endInterval, SortBy sortBy, ServicesDashboardInfo servicesDashboardInfo) {
    List<OrgProjectIdentifier> orgProjectIdentifierList = getOrgProjectIdentifier(accountId, userId);
    List<ActiveServiceInfo> activeServiceInfoList = new ArrayList<>();
    for (ServiceDashboardInfo serviceDashboardInfo : servicesDashboardInfo.getServiceDashboardInfoList()) {
      activeServiceInfoList.add(
          ActiveServiceInfo.builder()
              .accountInfo(AccountInfo.builder().accountIdentifier(serviceDashboardInfo.getAccountIdentifier()).build())
              .orgInfo(OrgInfo.builder().orgIdentifier(serviceDashboardInfo.getOrgIdentifier()).build())
              .projectInfo(ProjectInfo.builder().projectIdentifier(serviceDashboardInfo.getProjectIdentifier()).build())
              .countWithSuccessFailureDetails(
                  CountWithSuccessFailureDetails.builder()
                      .failureCount(serviceDashboardInfo.getFailureDeploymentsCount())
                      .successCount(serviceDashboardInfo.getSuccessDeploymentsCount())
                      .countChangeAndCountChangeRateInfo(
                          CountChangeAndCountChangeRateInfo.builder()
                              .countChangeRate((sortBy == DEPLOYMENTS)
                                      ? serviceDashboardInfo.getTotalDeploymentsChangeRate()
                                      : serviceDashboardInfo.getInstancesCountChangeRate())
                              .build())
                      .count((sortBy == DEPLOYMENTS) ? serviceDashboardInfo.getTotalDeploymentsCount()
                                                     : serviceDashboardInfo.getInstancesCount())
                      .build())
              .build());
    }
    return MostActiveServicesList.builder().build(); // activeServices(activeServiceInfoList).build();
  }

  @VisibleForTesting
  public CountChangeDetails getServicesCount(ServicesCount servicesCount) {
    return CountChangeDetails.builder()
        .countChangeAndCountChangeRateInfo(
            CountChangeAndCountChangeRateInfo.builder().countChange(servicesCount.getNewCount()).build())
        .count(servicesCount.getTotalCount())
        .build();
  }

  @VisibleForTesting
  public CountChangeDetails getEnvCount(EnvCount envCount) {
    return CountChangeDetails.builder()
        .countChangeAndCountChangeRateInfo(
            CountChangeAndCountChangeRateInfo.builder().countChange(envCount.getNewCount()).build())
        .count(envCount.getTotalCount())
        .build();
  }

  @VisibleForTesting
  public CountChangeDetails getPipelinesCount(PipelinesCount pipelinesCount) {
    return CountChangeDetails.builder()
        .countChangeAndCountChangeRateInfo(
            CountChangeAndCountChangeRateInfo.builder().countChange(pipelinesCount.getNewCount()).build())
        .count(pipelinesCount.getTotalCount())
        .build();
  }

  private List<OrgProjectIdentifier> getOrgProjectIdentifier(String accountId, String userId) {
    List<ProjectDTO> listOfAccessibleProject = dashboardRBACService.listAccessibleProject(accountId, userId);
    List<OrgProjectIdentifier> orgProjectId = new ArrayList<>();
    for (ProjectDTO projectDTO : listOfAccessibleProject) {
      orgProjectId.add(new OrgProjectIdentifier(projectDTO.getOrgIdentifier() + ":" + projectDTO.getIdentifier()));
    }
    return orgProjectId;
  }
}
