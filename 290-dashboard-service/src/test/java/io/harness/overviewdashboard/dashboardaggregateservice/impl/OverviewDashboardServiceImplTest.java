package io.harness.overviewdashboard.dashboardaggregateservice.impl;

import static io.harness.overviewdashboard.bean.OverviewDashboardRequestType.GET_ACTIVE_DEPLOYMENTS_INFO;
import static io.harness.overviewdashboard.bean.OverviewDashboardRequestType.GET_CD_TOP_PROJECT_LIST;
import static io.harness.overviewdashboard.bean.OverviewDashboardRequestType.GET_DEPLOYMENT_STATS_SUMMARY;
import static io.harness.overviewdashboard.bean.OverviewDashboardRequestType.GET_ENV_COUNT;
import static io.harness.overviewdashboard.bean.OverviewDashboardRequestType.GET_MOST_ACTIVE_SERVICES;
import static io.harness.overviewdashboard.bean.OverviewDashboardRequestType.GET_PIPELINES_COUNT;
import static io.harness.overviewdashboard.bean.OverviewDashboardRequestType.GET_PROJECTS_COUNT;
import static io.harness.overviewdashboard.bean.OverviewDashboardRequestType.GET_SERVICES_COUNT;
import static io.harness.rule.OwnerRule.MEET;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.dashboards.DeploymentStatsSummary;
import io.harness.dashboards.EnvCount;
import io.harness.dashboards.GroupBy;
import io.harness.dashboards.PipelineExecutionDashboardInfo;
import io.harness.dashboards.PipelinesExecutionDashboardInfo;
import io.harness.dashboards.ProjectDashBoardInfo;
import io.harness.dashboards.ProjectsDashboardInfo;
import io.harness.dashboards.ServiceDashboardInfo;
import io.harness.dashboards.ServicesCount;
import io.harness.dashboards.ServicesDashboardInfo;
import io.harness.dashboards.SortBy;
import io.harness.dashboards.TimeBasedDeploymentInfo;
import io.harness.data.structure.UUIDGenerator;
import io.harness.ng.core.dto.ActiveProjectsCountDTO;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.overviewdashboard.bean.RestCallResponse;
import io.harness.overviewdashboard.dtos.AccountInfo;
import io.harness.overviewdashboard.dtos.ActiveServiceInfo;
import io.harness.overviewdashboard.dtos.CountChangeAndCountChangeRateInfo;
import io.harness.overviewdashboard.dtos.CountChangeDetails;
import io.harness.overviewdashboard.dtos.CountOverview;
import io.harness.overviewdashboard.dtos.CountWithSuccessFailureDetails;
import io.harness.overviewdashboard.dtos.DeploymentsOverview;
import io.harness.overviewdashboard.dtos.DeploymentsStatsOverview;
import io.harness.overviewdashboard.dtos.DeploymentsStatsSummary;
import io.harness.overviewdashboard.dtos.ExecutionResponse;
import io.harness.overviewdashboard.dtos.ExecutionStatus;
import io.harness.overviewdashboard.dtos.MostActiveServicesList;
import io.harness.overviewdashboard.dtos.OrgInfo;
import io.harness.overviewdashboard.dtos.PipelineExecutionInfo;
import io.harness.overviewdashboard.dtos.PipelineInfo;
import io.harness.overviewdashboard.dtos.ProjectInfo;
import io.harness.overviewdashboard.dtos.RateAndRateChangeInfo;
import io.harness.overviewdashboard.dtos.ServiceInfo;
import io.harness.overviewdashboard.dtos.TimeBasedStats;
import io.harness.overviewdashboard.dtos.TopProjectsDashboardInfo;
import io.harness.overviewdashboard.dtos.TopProjectsPanel;
import io.harness.overviewdashboard.rbac.impl.DashboardRBACServiceImpl;
import io.harness.overviewdashboard.remote.ParallelRestCallExecutor;
import io.harness.pipeline.dashboards.PMSLandingDashboardResourceClient;
import io.harness.pms.dashboards.PipelinesCount;
import io.harness.rule.Owner;
import io.harness.userng.remote.UserNGClient;

import dashboards.CDLandingDashboardResourceClient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import retrofit2.Call;

public class OverviewDashboardServiceImplTest {
  String accountIdentifier = UUIDGenerator.generateUuid();
  String userId = UUIDGenerator.generateUuid();
  String serviceIdentifier = UUIDGenerator.generateUuid();
  String serviceName = UUIDGenerator.generateUuid();
  String projectIdentifier = UUIDGenerator.generateUuid();
  String projectName = UUIDGenerator.generateUuid();
  String orgIdentifier = UUIDGenerator.generateUuid();
  String orgName = UUIDGenerator.generateUuid();
  String planExecutionId = UUIDGenerator.generateUuid();
  String pipelineName = UUIDGenerator.generateUuid();
  String pipelineIdentifier = UUIDGenerator.generateUuid();
  List<ProjectDTO> listOfAccessibleProjects = new ArrayList<>();
  Map<String, String> mapOfOrganizationIdentifierAndOrganizationName = new HashMap<>();
  ;

  @InjectMocks OverviewDashboardServiceImpl overviewDashboardService;
  @Mock ParallelRestCallExecutor parallelRestCallExecutor;
  @Mock DashboardRBACServiceImpl dashboardRBACService;
  @Mock UserNGClient userNGClient;
  @Mock CDLandingDashboardResourceClient cdLandingDashboardResourceClient;
  @Mock PMSLandingDashboardResourceClient pmsLandingDashboardResourceClient;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = MEET)
  @Category(UnitTests.class)
  public void testGetDeploymentStatsOverview() throws Exception {
    GroupBy groupBy = GroupBy.MONTH;
    SortBy sortBy = SortBy.DEPLOYMENTS;
    mapOfOrganizationIdentifierAndOrganizationName.put(orgIdentifier, orgName);
    listOfAccessibleProjects.add(
        ProjectDTO.builder().identifier(projectIdentifier).orgIdentifier(orgIdentifier).name(projectName).build());

    Call<ResponseDTO<DeploymentStatsSummary>> requestDeploymentStatsSummary = Mockito.mock(Call.class);
    Call<ResponseDTO<ServicesDashboardInfo>> requestServicesDashboardInfo = Mockito.mock(Call.class);
    Call<ResponseDTO<PipelinesExecutionDashboardInfo>> requestActiveDeploymentStats = Mockito.mock(Call.class);

    when(dashboardRBACService.listAccessibleProject(anyString(), anyString())).thenReturn(listOfAccessibleProjects);
    when(dashboardRBACService.getMapOfOrganizationIdentifierAndOrganizationName(anyString(), anyList()))
        .thenReturn(mapOfOrganizationIdentifierAndOrganizationName);
    doReturn(requestActiveDeploymentStats)
        .when(cdLandingDashboardResourceClient)
        .getActiveDeploymentStats(anyString(), anyList());
    when(cdLandingDashboardResourceClient.getDeploymentStatsSummary(
             anyString(), anyList(), eq(1628899200000L), eq(1631491200000L), any()))
        .thenReturn(requestDeploymentStatsSummary);
    when(cdLandingDashboardResourceClient.get(anyString(), anyList(), eq(1628899200000L), eq(1631491200000L), any()))
        .thenReturn(requestServicesDashboardInfo);

    List<RestCallResponse> restCallResponseList = new ArrayList<>();
    restCallResponseList.add(
        RestCallResponse.<DeploymentStatsSummary>builder()
            .response(DeploymentStatsSummary.builder()
                          .totalCount(10L)
                          .deploymentRateChangeRate(0.24)
                          .deploymentRate(0.24)
                          .totalCountChangeRate(0.24)
                          .failureRate(0.24)
                          .failureRateChangeRate(0.24)
                          .timeBasedDeploymentInfoList(Collections.singletonList(TimeBasedDeploymentInfo.builder()
                                                                                     .totalCount(10L)
                                                                                     .failedCount(10L)
                                                                                     .successCount(10L)
                                                                                     .epochTime(10L)
                                                                                     .build()))
                          .build())
            .requestType(GET_DEPLOYMENT_STATS_SUMMARY)
            .build());
    restCallResponseList.add(
        RestCallResponse.<ServicesDashboardInfo>builder()
            .response(ServicesDashboardInfo.builder()
                          .serviceDashboardInfoList(Collections.singletonList(ServiceDashboardInfo.builder()
                                                                                  .accountIdentifier(accountIdentifier)
                                                                                  .failureDeploymentsCount(10L)
                                                                                  .identifier(serviceIdentifier)
                                                                                  .name(serviceName)
                                                                                  .orgIdentifier(orgIdentifier)
                                                                                  .projectIdentifier(projectIdentifier)
                                                                                  .successDeploymentsCount(10L)
                                                                                  .totalDeploymentsChangeRate(0.24)
                                                                                  .totalDeploymentsCount(10L)
                                                                                  .build()))
                          .build())
            .requestType(GET_MOST_ACTIVE_SERVICES)
            .build());
    restCallResponseList.add(
        RestCallResponse.<PipelinesExecutionDashboardInfo>builder()
            .response(PipelinesExecutionDashboardInfo.builder()
                          .failed24HrsExecutions(Collections.singletonList(PipelineExecutionDashboardInfo.builder()
                                                                               .accountIdentifier(accountIdentifier)
                                                                               .identifier(pipelineIdentifier)
                                                                               .orgIdentifier(orgIdentifier)
                                                                               .projectIdentifier(projectIdentifier)
                                                                               .name(pipelineName)
                                                                               .planExecutionId(planExecutionId)
                                                                               .startTs(1628899200000L)
                                                                               .build()))
                          .pendingApprovalExecutions(Collections.singletonList(PipelineExecutionDashboardInfo.builder()
                                                                                   .accountIdentifier(accountIdentifier)
                                                                                   .identifier(pipelineIdentifier)
                                                                                   .orgIdentifier(orgIdentifier)
                                                                                   .projectIdentifier(projectIdentifier)
                                                                                   .name(pipelineName)
                                                                                   .planExecutionId(planExecutionId)
                                                                                   .startTs(1628899200000L)
                                                                                   .build()))
                          .pendingManualInterventionExecutions(
                              Collections.singletonList(PipelineExecutionDashboardInfo.builder()
                                                            .accountIdentifier(accountIdentifier)
                                                            .identifier(pipelineIdentifier)
                                                            .orgIdentifier(orgIdentifier)
                                                            .projectIdentifier(projectIdentifier)
                                                            .name(pipelineName)
                                                            .planExecutionId(planExecutionId)
                                                            .startTs(1628899200000L)
                                                            .build()))
                          .runningExecutions(Collections.singletonList(PipelineExecutionDashboardInfo.builder()
                                                                           .accountIdentifier(accountIdentifier)
                                                                           .identifier(pipelineIdentifier)
                                                                           .orgIdentifier(orgIdentifier)
                                                                           .projectIdentifier(projectIdentifier)
                                                                           .name(pipelineName)
                                                                           .planExecutionId(planExecutionId)
                                                                           .startTs(1628899200000L)
                                                                           .build()))
                          .build())
            .requestType(GET_ACTIVE_DEPLOYMENTS_INFO)
            .build());

    when(parallelRestCallExecutor.executeRestCalls(anyList())).thenReturn(restCallResponseList);

    DeploymentsStatsOverview expectedResponse =
        DeploymentsStatsOverview.builder()
            .deploymentsStatsSummary(
                DeploymentsStatsSummary.builder()
                    .deploymentRateAndChangeRate(
                        RateAndRateChangeInfo.builder().rateChangeRate(0.24).rate(0.24).build())
                    .failureRateAndChangeRate(RateAndRateChangeInfo.builder().rateChangeRate(0.24).rate(0.24).build())
                    .countAndChangeRate(
                        CountChangeDetails.builder()
                            .count(10L)
                            .countChangeAndCountChangeRateInfo(
                                CountChangeAndCountChangeRateInfo.builder().countChangeRate(0.24).build())
                            .build())
                    .deploymentStats(Collections.singletonList(
                        TimeBasedStats.builder()
                            .time(10L)
                            .countWithSuccessFailureDetails(CountWithSuccessFailureDetails.builder()
                                                                .successCount(10L)
                                                                .failureCount(10L)
                                                                .count(10L)
                                                                .build())
                            .build()))
                    .build())
            .mostActiveServicesList(
                MostActiveServicesList.builder()
                    .activeServices(Collections.singletonList(
                        ActiveServiceInfo.builder()
                            .serviceInfo(ServiceInfo.builder()
                                             .serviceName(serviceName)
                                             .serviceIdentifier(serviceIdentifier)
                                             .build())
                            .accountInfo(AccountInfo.builder().accountIdentifier(accountIdentifier).build())
                            .orgInfo(OrgInfo.builder().orgIdentifier(orgIdentifier).orgName(orgName).build())
                            .projectInfo(ProjectInfo.builder()
                                             .projectIdentifier(projectIdentifier)
                                             .projectName(projectName)
                                             .build())
                            .countWithSuccessFailureDetails(
                                CountWithSuccessFailureDetails.builder()
                                    .count(10L)
                                    .successCount(10L)
                                    .failureCount(10L)
                                    .countChangeAndCountChangeRateInfo(
                                        CountChangeAndCountChangeRateInfo.builder().countChangeRate(0.24).build())
                                    .build())
                            .build()))
                    .build())
            .deploymentsOverview(
                DeploymentsOverview.builder()
                    .failed24HrsExecutions(Collections.singletonList(
                        PipelineExecutionInfo.builder()
                            .accountInfo(AccountInfo.builder().accountIdentifier(accountIdentifier).build())
                            .pipelineInfo(PipelineInfo.builder()
                                              .pipelineName(pipelineName)
                                              .pipelineIdentifier(pipelineIdentifier)
                                              .build())
                            .orgInfo(OrgInfo.builder().orgIdentifier(orgIdentifier).orgName(orgName).build())
                            .projectInfo(ProjectInfo.builder()
                                             .projectName(projectName)
                                             .projectIdentifier(projectIdentifier)
                                             .build())
                            .planExecutionId(planExecutionId)
                            .startTs(1628899200000L)
                            .build()))
                    .pendingApprovalExecutions(Collections.singletonList(
                        PipelineExecutionInfo.builder()
                            .accountInfo(AccountInfo.builder().accountIdentifier(accountIdentifier).build())
                            .pipelineInfo(PipelineInfo.builder()
                                              .pipelineName(pipelineName)
                                              .pipelineIdentifier(pipelineIdentifier)
                                              .build())
                            .orgInfo(OrgInfo.builder().orgIdentifier(orgIdentifier).orgName(orgName).build())
                            .projectInfo(ProjectInfo.builder()
                                             .projectName(projectName)
                                             .projectIdentifier(projectIdentifier)
                                             .build())
                            .planExecutionId(planExecutionId)
                            .startTs(1628899200000L)
                            .build()))
                    .pendingManualInterventionExecutions(Collections.singletonList(
                        PipelineExecutionInfo.builder()
                            .accountInfo(AccountInfo.builder().accountIdentifier(accountIdentifier).build())
                            .pipelineInfo(PipelineInfo.builder()
                                              .pipelineName(pipelineName)
                                              .pipelineIdentifier(pipelineIdentifier)
                                              .build())
                            .orgInfo(OrgInfo.builder().orgIdentifier(orgIdentifier).orgName(orgName).build())
                            .projectInfo(ProjectInfo.builder()
                                             .projectName(projectName)
                                             .projectIdentifier(projectIdentifier)
                                             .build())
                            .planExecutionId(planExecutionId)
                            .startTs(1628899200000L)
                            .build()))
                    .runningExecutions(Collections.singletonList(
                        PipelineExecutionInfo.builder()
                            .accountInfo(AccountInfo.builder().accountIdentifier(accountIdentifier).build())
                            .pipelineInfo(PipelineInfo.builder()
                                              .pipelineName(pipelineName)
                                              .pipelineIdentifier(pipelineIdentifier)
                                              .build())
                            .orgInfo(OrgInfo.builder().orgIdentifier(orgIdentifier).orgName(orgName).build())
                            .projectInfo(ProjectInfo.builder()
                                             .projectName(projectName)
                                             .projectIdentifier(projectIdentifier)
                                             .build())
                            .planExecutionId(planExecutionId)
                            .startTs(1628899200000L)
                            .build()))
                    .build())
            .build();

    ExecutionResponse<DeploymentsStatsOverview> actualResponse = overviewDashboardService.getDeploymentStatsOverview(
        accountIdentifier, userId, anyLong(), anyLong(), groupBy, sortBy);
    assertThat(actualResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(actualResponse.getResponse()).isEqualTo(expectedResponse);
  }

  @Test
  @Owner(developers = MEET)
  @Category(UnitTests.class)
  public void testGetCountOverview() throws Exception {
    Call<ResponseDTO<ServicesCount>> requestServicesCount = Mockito.mock(Call.class);
    Call<ResponseDTO<EnvCount>> requestEnvCount = Mockito.mock(Call.class);
    Call<ResponseDTO<PipelinesCount>> requestPipelinesCount = Mockito.mock(Call.class);
    Call<ResponseDTO<ActiveProjectsCountDTO>> requestProjectsCount = Mockito.mock(Call.class);

    when(dashboardRBACService.listAccessibleProject(anyString(), anyString())).thenReturn(listOfAccessibleProjects);
    when(cdLandingDashboardResourceClient.getServicesCount(
             anyString(), anyList(), eq(1628899200000L), eq(1631491200000L)))
        .thenReturn(requestServicesCount);
    when(cdLandingDashboardResourceClient.getEnvCount(anyString(), anyList(), eq(1628899200000L), eq(1631491200000L)))
        .thenReturn(requestEnvCount);
    when(pmsLandingDashboardResourceClient.getPipelinesCount(
             anyString(), anyList(), eq(1628899200000L), eq(1631491200000L)))
        .thenReturn(requestPipelinesCount);
    when(userNGClient.getAccessibleProjectsCount(anyString(), anyString(), eq(1628899200000L), eq(1631491200000L)))
        .thenReturn(requestProjectsCount);

    List<RestCallResponse> restCallResponseList = new ArrayList<>();
    restCallResponseList.add(RestCallResponse.<ServicesCount>builder()
                                 .response(ServicesCount.builder().newCount(10L).totalCount(12L).build())
                                 .requestType(GET_SERVICES_COUNT)
                                 .build());
    restCallResponseList.add(RestCallResponse.<EnvCount>builder()
                                 .response(EnvCount.builder().newCount(10L).totalCount(12L).build())
                                 .requestType(GET_ENV_COUNT)
                                 .build());
    restCallResponseList.add(RestCallResponse.<PipelinesCount>builder()
                                 .response(PipelinesCount.builder().newCount(10L).totalCount(12L).build())
                                 .requestType(GET_PIPELINES_COUNT)
                                 .build());
    restCallResponseList.add(RestCallResponse.<ActiveProjectsCountDTO>builder()
                                 .response(ActiveProjectsCountDTO.builder().count(10).build())
                                 .requestType(GET_PROJECTS_COUNT)
                                 .build());

    when(parallelRestCallExecutor.executeRestCalls(anyList())).thenReturn(restCallResponseList);
    CountOverview expectedResponse =
        CountOverview.builder()
            .servicesCountDetail(CountChangeDetails.builder()
                                     .countChangeAndCountChangeRateInfo(
                                         CountChangeAndCountChangeRateInfo.builder().countChange(10L).build())
                                     .count(12L)
                                     .build())
            .envCountDetail(CountChangeDetails.builder()
                                .countChangeAndCountChangeRateInfo(
                                    CountChangeAndCountChangeRateInfo.builder().countChange(10L).build())
                                .count(12L)
                                .build())
            .pipelinesCountDetail(CountChangeDetails.builder()
                                      .countChangeAndCountChangeRateInfo(
                                          CountChangeAndCountChangeRateInfo.builder().countChange(10L).build())
                                      .count(12L)
                                      .build())
            .projectsCountDetail(CountChangeDetails.builder()
                                     .count(12L)
                                     .countChangeAndCountChangeRateInfo(
                                         CountChangeAndCountChangeRateInfo.builder().countChange(10L).build())
                                     .build())
            .build();

    ExecutionResponse<CountOverview> actualResponse =
        overviewDashboardService.getCountOverview(accountIdentifier, userId, 1628899200000L, 1631491200000L);
    assertThat(actualResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(actualResponse.getResponse()).isEqualTo(expectedResponse);
  }

  @Test
  @Owner(developers = MEET)
  @Category(UnitTests.class)
  public void testGetTopProjectsPanel() throws Exception {
    Call<ResponseDTO<ProjectsDashboardInfo>> requestProjectsDashboardInfo = Mockito.mock(Call.class);

    when(dashboardRBACService.listAccessibleProject(anyString(), anyString())).thenReturn(listOfAccessibleProjects);
    when(
        cdLandingDashboardResourceClient.getTopProjects(anyString(), anyList(), eq(1628899200000L), eq(1631491200000L)))
        .thenReturn(requestProjectsDashboardInfo);

    List<RestCallResponse> restCallResponseList = new ArrayList<>();
    restCallResponseList.add(
        RestCallResponse.<ProjectsDashboardInfo>builder()
            .response(ProjectsDashboardInfo.builder()
                          .projectDashBoardInfoList(Collections.singletonList(ProjectDashBoardInfo.builder()
                                                                                  .accountId(accountIdentifier)
                                                                                  .projectIdentifier(projectIdentifier)
                                                                                  .orgIdentifier(orgIdentifier)
                                                                                  .deploymentsCount(10L)
                                                                                  .deploymentsCountChangeRate(0.24)
                                                                                  .build()))
                          .build())
            .requestType(GET_CD_TOP_PROJECT_LIST)
            .build());

    when(parallelRestCallExecutor.executeRestCalls(anyList())).thenReturn(restCallResponseList);

    TopProjectsPanel expectedResponse =
        TopProjectsPanel.builder()
            .CDTopProjectsInfo(
                ExecutionResponse.<List<TopProjectsDashboardInfo<CountWithSuccessFailureDetails>>>builder()
                    .response(Collections.singletonList(
                        TopProjectsDashboardInfo.<CountWithSuccessFailureDetails>builder()
                            .accountInfo(AccountInfo.builder().accountIdentifier(accountIdentifier).build())
                            .orgInfo(OrgInfo.builder().orgIdentifier(orgIdentifier).build())
                            .projectInfo(ProjectInfo.builder().projectIdentifier(projectIdentifier).build())
                            .countDetails(
                                CountWithSuccessFailureDetails.builder()
                                    .count(10L)
                                    .countChangeAndCountChangeRateInfo(
                                        CountChangeAndCountChangeRateInfo.builder().countChangeRate(0.24).build())
                                    .build())
                            .build()))
                    .build())
            .build();
    ExecutionResponse<TopProjectsPanel> actualResponse =
        overviewDashboardService.getTopProjectsPanel(accountIdentifier, userId, 1628899200000L, 1631491200000L);
    assertThat(actualResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(actualResponse.getResponse().getCDTopProjectsInfo().getResponse())
        .isEqualTo(expectedResponse.getCDTopProjectsInfo().getResponse());
  }
}