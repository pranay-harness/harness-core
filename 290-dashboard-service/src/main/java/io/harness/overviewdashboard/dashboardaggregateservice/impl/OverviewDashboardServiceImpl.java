package io.harness.overviewdashboard.dashboardaggregateservice.impl;

import static io.harness.dashboards.SortBy.DEPLOYMENTS;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dashboards.DeploymentStatsSummary;
import io.harness.dashboards.EnvCount;
import io.harness.dashboards.GroupBy;
import io.harness.dashboards.ProjectDashBoardInfo;
import io.harness.dashboards.ProjectsDashboardInfo;
import io.harness.dashboards.ServiceDashboardInfo;
import io.harness.dashboards.ServicesCount;
import io.harness.dashboards.ServicesDashboardInfo;
import io.harness.dashboards.SortBy;
import io.harness.dashboards.TimeBasedDeploymentInfo;
import io.harness.ng.core.OrgProjectIdentifier;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.overviewdashboard.bean.OverviewDashboardRequestType;
import io.harness.overviewdashboard.bean.RestCallRequest;
import io.harness.overviewdashboard.bean.RestCallResponse;
import io.harness.overviewdashboard.dashboardaggregateservice.service.OverviewDashboardService;
import io.harness.overviewdashboard.dtos.AccountInfo;
import io.harness.overviewdashboard.dtos.ActiveServiceInfo;
import io.harness.overviewdashboard.dtos.CountChangeAndCountChangeRateInfo;
import io.harness.overviewdashboard.dtos.CountChangeDetails;
import io.harness.overviewdashboard.dtos.CountInfo;
import io.harness.overviewdashboard.dtos.CountOverview;
import io.harness.overviewdashboard.dtos.CountWithSuccessFailureDetails;
import io.harness.overviewdashboard.dtos.DeploymentsOverview;
import io.harness.overviewdashboard.dtos.DeploymentsStatsOverview;
import io.harness.overviewdashboard.dtos.DeploymentsStatsSummary;
import io.harness.overviewdashboard.dtos.ExecutionResponse;
import io.harness.overviewdashboard.dtos.ExecutionStatus;
import io.harness.overviewdashboard.dtos.MostActiveServicesList;
import io.harness.overviewdashboard.dtos.OrgInfo;
import io.harness.overviewdashboard.dtos.ProjectInfo;
import io.harness.overviewdashboard.dtos.RateAndRateChangeInfo;
import io.harness.overviewdashboard.dtos.ServiceInfo;
import io.harness.overviewdashboard.dtos.TimeBasedStats;
import io.harness.overviewdashboard.dtos.TopProjectsDashboardInfo;
import io.harness.overviewdashboard.dtos.TopProjectsPanel;
import io.harness.overviewdashboard.rbac.service.DashboardRBACService;
import io.harness.overviewdashboard.remote.ParallelRestCallExecutor;
import io.harness.pipeline.dashboards.PMSLandingDashboardResourceClient;
import io.harness.pms.dashboards.PipelinesCount;

import com.google.inject.Inject;
import dashboards.CDLandingDashboardResourceClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@OwnedBy(HarnessTeam.PL)
public class OverviewDashboardServiceImpl implements OverviewDashboardService {
  private final String SUCCESS_MESSAGE = "succes";
  private final String FAILURE_MESSAGE = "failed to get data";

  @Inject DashboardRBACService dashboardRBACService;
  @Inject CDLandingDashboardResourceClient cdLandingDashboardResourceClient;
  @Inject PMSLandingDashboardResourceClient pmsLandingDashboardResourceClient;
  @Inject ParallelRestCallExecutor parallelRestCallExecutor;

  @Override
  public ExecutionResponse<TopProjectsPanel> getTopProjectsPanel(
      String accountIdentifier, String userId, long startInterval, long endInterval) {
    List<OrgProjectIdentifier> orgProjectIdentifierList = getOrgProjectIdentifier(accountIdentifier, userId);
    List<RestCallRequest> restCallRequestList = getRestCallRequestListForTopProjectsPanel(
        accountIdentifier, startInterval, endInterval, orgProjectIdentifierList);
    List<RestCallResponse> restCallResponses = parallelRestCallExecutor.executeRestCalls(restCallRequestList);

    Optional<RestCallResponse> cdProjectsDashBoardInfoOptional =
        getResponseOptional(restCallResponses, OverviewDashboardRequestType.GET_CD_TOP_PROJECT_LIST);

    ExecutionResponse<List<TopProjectsDashboardInfo<CountWithSuccessFailureDetails>>>
        executionResponseCDTopProjectsInfoList =
            getExecutionResponseCDTopProjectsInfoList(cdProjectsDashBoardInfoOptional);
    ExecutionResponse<List<TopProjectsDashboardInfo<CountWithSuccessFailureDetails>>>
        executionResponseCITopProjectsInfoList = getExecutionResponseCITopProjectsInfoList();
    ExecutionResponse<List<TopProjectsDashboardInfo<CountInfo>>> executionResponseCFTopProjectsInfoList =
        getExecutionResponseCFTopProjectsInfoList();

    if (executionResponseCDTopProjectsInfoList.getExecutionStatus() == ExecutionStatus.SUCCESS
        || executionResponseCITopProjectsInfoList.getExecutionStatus() == ExecutionStatus.SUCCESS
        || executionResponseCFTopProjectsInfoList.getExecutionStatus() == ExecutionStatus.SUCCESS) {
      return ExecutionResponse.<TopProjectsPanel>builder()
          .response((TopProjectsPanel.builder()
                         .CDTopProjectsInfo(executionResponseCDTopProjectsInfoList)
                         .CITopProjectsInfo(executionResponseCITopProjectsInfoList)
                         .CFTopProjectsInfo(executionResponseCFTopProjectsInfoList)
                         .build()))
          .executionStatus(ExecutionStatus.SUCCESS)
          .executionMessage(SUCCESS_MESSAGE)
          .build();
    } else {
      return ExecutionResponse.<TopProjectsPanel>builder()
          .response((TopProjectsPanel.builder()
                         .CDTopProjectsInfo(executionResponseCDTopProjectsInfoList)
                         .CITopProjectsInfo(executionResponseCITopProjectsInfoList)
                         .CFTopProjectsInfo(executionResponseCFTopProjectsInfoList)
                         .build()))
          .executionStatus(ExecutionStatus.FAILURE)
          .executionMessage(FAILURE_MESSAGE)
          .build();
    }
  }

  @Override
  public ExecutionResponse<DeploymentsStatsOverview> getDeploymentStatsOverview(
      String accountIdentifier, String userId, long startInterval, long endInterval, GroupBy groupBy, SortBy sortBy) {
    List<OrgProjectIdentifier> orgProjectIdentifierList = getOrgProjectIdentifier(accountIdentifier, userId);
    List<RestCallRequest> restCallRequestList = getRestCallRequestListForDeploymentStatsOverview(
        accountIdentifier, startInterval, endInterval, orgProjectIdentifierList, groupBy, sortBy);
    List<RestCallResponse> restCallResponses = parallelRestCallExecutor.executeRestCalls(restCallRequestList);

    Optional<RestCallResponse> deploymentStatsSummaryOptional =
        getResponseOptional(restCallResponses, OverviewDashboardRequestType.GET_DEPLOYMENTS_STATS_SUMMARY);
    Optional<RestCallResponse> timeBasedDeploymentsInfoOptional =
        getResponseOptional(restCallResponses, OverviewDashboardRequestType.GET_TIME_WISE_DEPLOYMENT_INFO);
    Optional<RestCallResponse> mostActiveServicesOptional =
        getResponseOptional(restCallResponses, OverviewDashboardRequestType.GET_MOST_ACTIVE_SERVICES);

    if (deploymentStatsSummaryOptional.isPresent() && timeBasedDeploymentsInfoOptional.isPresent()
        && mostActiveServicesOptional.isPresent()) {
      if (deploymentStatsSummaryOptional.get().isCallFailed() || timeBasedDeploymentsInfoOptional.get().isCallFailed()
          || mostActiveServicesOptional.get().isCallFailed()) {
        return ExecutionResponse.<DeploymentsStatsOverview>builder()
            .executionStatus(ExecutionStatus.FAILURE)
            .executionMessage(FAILURE_MESSAGE)
            .build();
      } else {
        DeploymentStatsSummary deploymentStatsSummary =
            (DeploymentStatsSummary) deploymentStatsSummaryOptional.get().getResponse();
        List<TimeBasedDeploymentInfo> timeBasedDeploymentInfoList =
            (List<TimeBasedDeploymentInfo>) timeBasedDeploymentsInfoOptional.get().getResponse();
        ServicesDashboardInfo servicesDashboardInfo =
            (ServicesDashboardInfo) mostActiveServicesOptional.get().getResponse();
        return ExecutionResponse.<DeploymentsStatsOverview>builder()
            .response(DeploymentsStatsOverview.builder()
                          .deploymentsStatsSummary(
                              getDeploymentStatsSummary(deploymentStatsSummary, timeBasedDeploymentInfoList))
                          .mostActiveServicesList(getMostActiveServicesList(sortBy, servicesDashboardInfo))
                          .build())
            .executionStatus(ExecutionStatus.SUCCESS)
            .executionMessage(SUCCESS_MESSAGE)
            .build();
      }
    }
    return ExecutionResponse.<DeploymentsStatsOverview>builder()
        .executionStatus(ExecutionStatus.FAILURE)
        .executionMessage(FAILURE_MESSAGE)
        .build();
  }

  @Override
  public ExecutionResponse<CountOverview> getCountOverview(
      String accountIdentifier, String userId, long startInterval, long endInterval) {
    List<OrgProjectIdentifier> orgProjectIdentifierList = getOrgProjectIdentifier(accountIdentifier, userId);
    List<RestCallRequest> restCallRequestList =
        getRestCallRequestListForCountOverview(accountIdentifier, startInterval, endInterval, orgProjectIdentifierList);
    List<RestCallResponse> restCallResponses = parallelRestCallExecutor.executeRestCalls(restCallRequestList);

    Optional<RestCallResponse> servicesCountOptional =
        getResponseOptional(restCallResponses, OverviewDashboardRequestType.GET_SERVICES_COUNT);
    Optional<RestCallResponse> envCountOptional =
        getResponseOptional(restCallResponses, OverviewDashboardRequestType.GET_ENV_COUNT);
    Optional<RestCallResponse> pipelinesCountOptional =
        getResponseOptional(restCallResponses, OverviewDashboardRequestType.GET_PIPELINES_COUNT);

    if (servicesCountOptional.isPresent() && envCountOptional.isPresent() && pipelinesCountOptional.isPresent()) {
      if (servicesCountOptional.get().isCallFailed() || envCountOptional.get().isCallFailed()
          || pipelinesCountOptional.get().isCallFailed()) {
        return ExecutionResponse.<CountOverview>builder()
            .executionStatus(ExecutionStatus.FAILURE)
            .executionMessage(FAILURE_MESSAGE)
            .build();
      } else {
        ServicesCount servicesCount = (ServicesCount) servicesCountOptional.get().getResponse();
        EnvCount envCount = (EnvCount) envCountOptional.get().getResponse();
        PipelinesCount pipelinesCount = (PipelinesCount) pipelinesCountOptional.get().getResponse();
        return ExecutionResponse.<CountOverview>builder()
            .response(CountOverview.builder()
                          .servicesCountDetail(getServicesCount(servicesCount))
                          .envCountDetail(getEnvCount(envCount))
                          .pipelinesCountDetail(getPipelinesCount(pipelinesCount))
                          .build())
            .executionStatus(ExecutionStatus.SUCCESS)
            .executionMessage(SUCCESS_MESSAGE)
            .build();
      }
    }
    return ExecutionResponse.<CountOverview>builder()
        .executionStatus(ExecutionStatus.FAILURE)
        .executionMessage(FAILURE_MESSAGE)
        .build();
  }

  private DeploymentsStatsSummary getDeploymentStatsSummary(
      DeploymentStatsSummary deploymentStatsSummary, List<TimeBasedDeploymentInfo> timeBasedDeploymentInfoList) {
    return DeploymentsStatsSummary.builder()
        .countAndChangeRate(
            CountChangeDetails.builder()
                .count(deploymentStatsSummary.getCount())
                .countChangeAndCountChangeRateInfo(CountChangeAndCountChangeRateInfo.builder()
                                                       .countChangeRate(deploymentStatsSummary.getCountChangeRate())
                                                       .build())
                .build())
        .failureCountAndChangeRate(
            CountChangeDetails.builder()
                .count(deploymentStatsSummary.getFailureCount())
                .countChangeAndCountChangeRateInfo(CountChangeAndCountChangeRateInfo.builder()
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
        .deploymentsOverview(DeploymentsOverview.builder()
                                 .failedCount(deploymentStatsSummary.getFailed24HoursCount())
                                 .manualInterventionsCount(deploymentStatsSummary.getManualInterventionsCount())
                                 .pendingApprovalsCount(deploymentStatsSummary.getPendingApprovalsCount())
                                 .runningCount(deploymentStatsSummary.getRunningCount())
                                 .build())
        .deploymentStats(getTimeWiseDeploymentInfo(timeBasedDeploymentInfoList))
        .build();
  }
  private List<TimeBasedStats> getTimeWiseDeploymentInfo(List<TimeBasedDeploymentInfo> timeBasedDeploymentInfoList) {
    List<TimeBasedStats> timeBasedStatsList = new ArrayList<>();
    for (TimeBasedDeploymentInfo timeBasedDeploymentInfo : timeBasedDeploymentInfoList) {
      TimeBasedStats timeBasedStats =
          TimeBasedStats.builder()
              .time(timeBasedDeploymentInfo.getTime())
              .countWithSuccessFailureDetails(CountWithSuccessFailureDetails.builder()
                                                  .count(timeBasedDeploymentInfo.getCount())
                                                  .failureCount(timeBasedDeploymentInfo.getFailedCount())
                                                  .successCount(timeBasedDeploymentInfo.getSuccessCount())
                                                  .build())
              .build();
      timeBasedStatsList.add(timeBasedStats);
    }
    return timeBasedStatsList;
  }

  private MostActiveServicesList getMostActiveServicesList(SortBy sortBy, ServicesDashboardInfo servicesDashboardInfo) {
    List<ActiveServiceInfo> activeServiceInfoList = new ArrayList<>();
    for (ServiceDashboardInfo serviceDashboardInfo : servicesDashboardInfo.getServiceDashboardInfoList()) {
      ActiveServiceInfo activeServiceInfo =
          ActiveServiceInfo.builder()
              .accountInfo(AccountInfo.builder().accountIdentifier(serviceDashboardInfo.getAccountIdentifier()).build())
              .orgInfo(OrgInfo.builder().orgIdentifier(serviceDashboardInfo.getOrgIdentifier()).build())
              .projectInfo(ProjectInfo.builder().projectIdentifier(serviceDashboardInfo.getProjectIdentifier()).build())
              .serviceInfo(ServiceInfo.builder()
                               .serviceInfo(serviceDashboardInfo.getIdentifier())
                               .serviceName(serviceDashboardInfo.getName())
                               .build())
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
              .build();
      activeServiceInfoList.add(activeServiceInfo);
    }
    return MostActiveServicesList.builder().activeServices(activeServiceInfoList).build();
  }

  private CountChangeDetails getServicesCount(ServicesCount servicesCount) {
    return CountChangeDetails.builder()
        .countChangeAndCountChangeRateInfo(
            CountChangeAndCountChangeRateInfo.builder().countChange(servicesCount.getNewCount()).build())
        .count(servicesCount.getTotalCount())
        .build();
  }

  private CountChangeDetails getEnvCount(EnvCount envCount) {
    return CountChangeDetails.builder()
        .countChangeAndCountChangeRateInfo(
            CountChangeAndCountChangeRateInfo.builder().countChange(envCount.getNewCount()).build())
        .count(envCount.getTotalCount())
        .build();
  }

  private CountChangeDetails getPipelinesCount(PipelinesCount pipelinesCount) {
    return CountChangeDetails.builder()
        .countChangeAndCountChangeRateInfo(
            CountChangeAndCountChangeRateInfo.builder().countChange(pipelinesCount.getNewCount()).build())
        .count(pipelinesCount.getTotalCount())
        .build();
  }

  private List<OrgProjectIdentifier> getOrgProjectIdentifier(String accountIdentifier, String userId) {
    List<ProjectDTO> listOfAccessibleProject = dashboardRBACService.listAccessibleProject(accountIdentifier, userId);
    return emptyIfNull(listOfAccessibleProject)
        .stream()
        .map(projectDTO -> new OrgProjectIdentifier(projectDTO.getOrgIdentifier() + ":" + projectDTO.getIdentifier()))
        .collect(Collectors.toList());
  }

  private Optional<RestCallResponse> getResponseOptional(
      List<RestCallResponse> restCallResponses, OverviewDashboardRequestType overviewDashboardRequestType) {
    return emptyIfNull(restCallResponses)
        .stream()
        .filter(k -> k.getRequestType() == overviewDashboardRequestType)
        .findAny();
  }

  private List<RestCallRequest> getRestCallRequestListForTopProjectsPanel(String accountIdentifier, long startInterval,
      long endInterval, List<OrgProjectIdentifier> orgProjectIdentifierList) {
    List<RestCallRequest> restCallRequestList = new ArrayList<>();
    restCallRequestList.add(RestCallRequest.<ProjectsDashboardInfo>builder()
                                .request(cdLandingDashboardResourceClient.getTopProjects(
                                    accountIdentifier, orgProjectIdentifierList, startInterval, endInterval))
                                .requestType(OverviewDashboardRequestType.GET_CD_TOP_PROJECT_LIST)
                                .build());
    return restCallRequestList;
  }

  private List<TopProjectsDashboardInfo<CountWithSuccessFailureDetails>> getCDTopProjectsInfoList(
      ProjectsDashboardInfo cdProjectsDashBoardInfo) {
    List<TopProjectsDashboardInfo<CountWithSuccessFailureDetails>> cdTopProjectsInfoList = new ArrayList<>();
    for (ProjectDashBoardInfo projectDashBoardInfo : cdProjectsDashBoardInfo.getProjectDashBoardInfoList()) {
      cdTopProjectsInfoList.add(
          TopProjectsDashboardInfo.<CountWithSuccessFailureDetails>builder()
              .accountInfo(AccountInfo.builder().accountIdentifier(projectDashBoardInfo.getAccountId()).build())
              .orgInfo(OrgInfo.builder().orgIdentifier(projectDashBoardInfo.getOrgIdentifier()).build())
              .projectInfo(ProjectInfo.builder().projectIdentifier(projectDashBoardInfo.getProjectIdentifier()).build())
              .countDetails(CountWithSuccessFailureDetails.builder()
                                .count(projectDashBoardInfo.getDeploymentsCount())
                                .countChangeAndCountChangeRateInfo(CountChangeAndCountChangeRateInfo.builder().build())
                                .build())
              .build());
    }
    return cdTopProjectsInfoList;
  }

  private List<RestCallRequest> getRestCallRequestListForDeploymentStatsOverview(String accountIdentifier,
      long startInterval, long endInterval, List<OrgProjectIdentifier> orgProjectIdentifierList, GroupBy groupBy,
      SortBy sortBy) {
    List<RestCallRequest> restCallRequestList = new ArrayList<>();
    restCallRequestList.add(RestCallRequest.<DeploymentStatsSummary>builder()
                                .request(cdLandingDashboardResourceClient.getDeploymentStatsSummary(
                                    accountIdentifier, orgProjectIdentifierList, startInterval, endInterval))
                                .requestType(OverviewDashboardRequestType.GET_DEPLOYMENTS_STATS_SUMMARY)
                                .build());
    restCallRequestList.add(RestCallRequest.<List<TimeBasedDeploymentInfo>>builder()
                                .request(cdLandingDashboardResourceClient.getTimeWiseDeploymentInfo(
                                    accountIdentifier, orgProjectIdentifierList, startInterval, endInterval, groupBy))
                                .requestType(OverviewDashboardRequestType.GET_TIME_WISE_DEPLOYMENT_INFO)
                                .build());
    restCallRequestList.add(RestCallRequest.<ServicesDashboardInfo>builder()
                                .request(cdLandingDashboardResourceClient.get(
                                    accountIdentifier, orgProjectIdentifierList, startInterval, endInterval, sortBy))
                                .requestType(OverviewDashboardRequestType.GET_MOST_ACTIVE_SERVICES)
                                .build());
    return restCallRequestList;
  }

  private List<RestCallRequest> getRestCallRequestListForCountOverview(String accountIdentifier, long startInterval,
      long endInterval, List<OrgProjectIdentifier> orgProjectIdentifierList) {
    List<RestCallRequest> restCallRequestList = new ArrayList<>();
    restCallRequestList.add(RestCallRequest.<ServicesCount>builder()
                                .request(cdLandingDashboardResourceClient.getServicesCount(
                                    accountIdentifier, orgProjectIdentifierList, startInterval, endInterval))
                                .requestType(OverviewDashboardRequestType.GET_SERVICES_COUNT)
                                .build());
    restCallRequestList.add(RestCallRequest.<EnvCount>builder()
                                .request(cdLandingDashboardResourceClient.getEnvCount(
                                    accountIdentifier, orgProjectIdentifierList, startInterval, endInterval))
                                .requestType(OverviewDashboardRequestType.GET_ENV_COUNT)
                                .build());
    restCallRequestList.add(RestCallRequest.<PipelinesCount>builder()
                                .request(pmsLandingDashboardResourceClient.getPipelinesCount(
                                    accountIdentifier, orgProjectIdentifierList, startInterval, endInterval))
                                .requestType(OverviewDashboardRequestType.GET_PIPELINES_COUNT)
                                .build());
    return restCallRequestList;
  }

  private ExecutionResponse<List<TopProjectsDashboardInfo<CountWithSuccessFailureDetails>>>
  getExecutionResponseCDTopProjectsInfoList(Optional<RestCallResponse> cdProjectsDashBoardInfoOptional) {
    ExecutionResponse<List<TopProjectsDashboardInfo<CountWithSuccessFailureDetails>>>
        executionResponseCDTopProjectsInfoList;
    if (cdProjectsDashBoardInfoOptional.isPresent()) {
      if (cdProjectsDashBoardInfoOptional.get().isCallFailed()) {
        executionResponseCDTopProjectsInfoList =
            ExecutionResponse.<List<TopProjectsDashboardInfo<CountWithSuccessFailureDetails>>>builder()
                .executionStatus(ExecutionStatus.FAILURE)
                .executionMessage(FAILURE_MESSAGE)
                .build();
      } else {
        ProjectsDashboardInfo cdProjectsDashBoardInfo =
            (ProjectsDashboardInfo) cdProjectsDashBoardInfoOptional.get().getResponse();
        executionResponseCDTopProjectsInfoList =
            ExecutionResponse.<List<TopProjectsDashboardInfo<CountWithSuccessFailureDetails>>>builder()
                .response(getCDTopProjectsInfoList(cdProjectsDashBoardInfo))
                .executionStatus(ExecutionStatus.SUCCESS)
                .executionMessage(SUCCESS_MESSAGE)
                .build();
      }
    } else {
      executionResponseCDTopProjectsInfoList =
          ExecutionResponse.<List<TopProjectsDashboardInfo<CountWithSuccessFailureDetails>>>builder()
              .executionStatus(ExecutionStatus.FAILURE)
              .executionMessage(FAILURE_MESSAGE)
              .build();
    }
    return executionResponseCDTopProjectsInfoList;
  }
  private ExecutionResponse<List<TopProjectsDashboardInfo<CountWithSuccessFailureDetails>>>
  getExecutionResponseCITopProjectsInfoList() {
    return ExecutionResponse.<List<TopProjectsDashboardInfo<CountWithSuccessFailureDetails>>>builder()
        .executionStatus(ExecutionStatus.FAILURE)
        .executionMessage(FAILURE_MESSAGE)
        .build();
  }
  private ExecutionResponse<List<TopProjectsDashboardInfo<CountInfo>>> getExecutionResponseCFTopProjectsInfoList() {
    return ExecutionResponse.<List<TopProjectsDashboardInfo<CountInfo>>>builder()
        .executionStatus(ExecutionStatus.FAILURE)
        .executionMessage(FAILURE_MESSAGE)
        .build();
  }
}
