package io.harness.cdng.Dashboard;

import static io.harness.rule.OwnerRule.MEENAKSHI;
import static io.harness.rule.OwnerRule.PRASHANTSHARMA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.Deployment.DashboardDeploymentActiveFailedRunningInfo;
import io.harness.cdng.Deployment.DashboardWorkloadDeployment;
import io.harness.cdng.Deployment.Deployment;
import io.harness.cdng.Deployment.DeploymentCount;
import io.harness.cdng.Deployment.DeploymentDateAndCount;
import io.harness.cdng.Deployment.DeploymentInfo;
import io.harness.cdng.Deployment.DeploymentStatusInfo;
import io.harness.cdng.Deployment.DeploymentStatusInfoList;
import io.harness.cdng.Deployment.ExecutionDeployment;
import io.harness.cdng.Deployment.ExecutionDeploymentDetailInfo;
import io.harness.cdng.Deployment.ExecutionDeploymentInfo;
import io.harness.cdng.Deployment.HealthDeploymentDashboard;
import io.harness.cdng.Deployment.HealthDeploymentInfo;
import io.harness.cdng.Deployment.ServiceDeploymentInfo;
import io.harness.cdng.Deployment.TimeAndStatusDeployment;
import io.harness.cdng.Deployment.TotalDeploymentInfo;
import io.harness.cdng.Deployment.WorkloadCountInfo;
import io.harness.cdng.Deployment.WorkloadDateCountInfo;
import io.harness.cdng.Deployment.WorkloadDeploymentInfo;
import io.harness.cdng.service.dashboard.CDOverviewDashboardServiceImpl;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.rule.Owner;
import io.harness.timescaledb.TimeScaleDBService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

@OwnedBy(HarnessTeam.CDC)
public class CDDashboardApisTest {
  @Mock TimeScaleDBService timeScaleDBService;
  @InjectMocks @Spy private CDOverviewDashboardServiceImpl cdOverviewDashboardServiceImpl;

  private List<String> failedStatusList =
      Arrays.asList(ExecutionStatus.FAILED.name(), ExecutionStatus.ABORTED.name(), ExecutionStatus.EXPIRED.name());
  private List<String> activeStatusList = Arrays.asList(ExecutionStatus.RUNNING.name(), ExecutionStatus.PAUSED.name());
  private List<String> pendingStatusList = Arrays.asList(ExecutionStatus.INTERVENTION_WAITING.name(),
      ExecutionStatus.APPROVAL_WAITING.name(), ExecutionStatus.WAITING.name());

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGetHealthDeploymentDashboard() {
    String startInterval = "2021-04-28";
    String endInterval = "2021-05-02";
    String previousInterval = "2021-04-23";

    List<String> status = Arrays.asList(ExecutionStatus.SUCCESS.name(), ExecutionStatus.EXPIRED.name(),
        ExecutionStatus.RUNNING.name(), ExecutionStatus.ABORTED.name(), ExecutionStatus.SUCCESS.name(),
        ExecutionStatus.FAILED.name(), ExecutionStatus.FAILED.name(), ExecutionStatus.SUCCESS.name(),
        ExecutionStatus.SUCCESS.name(), ExecutionStatus.WAITING.name(), ExecutionStatus.SUCCESS.name(),
        ExecutionStatus.EXPIRED.name(), ExecutionStatus.RUNNING.name(), ExecutionStatus.ABORTED.name(),
        ExecutionStatus.SUCCESS.name(), ExecutionStatus.SUCCESS.name(), ExecutionStatus.FAILED.name(),
        ExecutionStatus.SUCCESS.name(), ExecutionStatus.SUCCESS.name(), ExecutionStatus.FAILED.name());

    List<String> time = Arrays.asList("2021-04-28 21:50:02.64", "2021-05-01 21:49:11.222", "2021-05-01 21:48:45.39",
        "2021-04-30 21:47:49.771", "2021-05-01 21:46:55.556", "2021-05-02 21:45:27.619", "2021-04-30 21:44:59.73",
        "2021-05-01 21:43:52.675", "2021-04-30 21:43:49.537", "2021-04-28 21:43:40.053", "2021-04-24 21:50:02.64",
        "2021-04-27 21:49:11.222", "2021-04-24 21:48:45.39", "2021-04-25 21:47:49.771", "2021-04-23 21:46:55.556",
        "2021-04-26 21:45:27.619", "2021-04-26 21:44:59.73", "2021-04-24 21:43:52.675", "2021-04-24 21:43:49.537",
        "2021-04-26 21:43:40.053");

    List<String> env_type = Arrays.asList(EnvironmentType.Production.name(), EnvironmentType.Production.name(),
        EnvironmentType.PreProduction.name(), EnvironmentType.PreProduction.name(),
        EnvironmentType.PreProduction.name(), EnvironmentType.PreProduction.name(), EnvironmentType.Production.name(),
        EnvironmentType.PreProduction.name(), EnvironmentType.PreProduction.name(), EnvironmentType.Production.name());

    TimeAndStatusDeployment statusAndTime = TimeAndStatusDeployment.builder().status(status).time(time).build();

    doReturn(statusAndTime).when(cdOverviewDashboardServiceImpl).queryCalculatorTimeAndStatus(anyString());
    doReturn(env_type).when(cdOverviewDashboardServiceImpl).queryCalculatorEnvType(anyString());

    HealthDeploymentDashboard healthDeploymentDashboard = cdOverviewDashboardServiceImpl.getHealthDeploymentDashboard(
        "acc", "oro", "pro", startInterval, endInterval, previousInterval);

    List<DeploymentDateAndCount> totalCountList = new ArrayList<>();
    totalCountList.add(
        DeploymentDateAndCount.builder().time("2021-04-28").deployments(Deployment.builder().count(2).build()).build());
    totalCountList.add(
        DeploymentDateAndCount.builder().time("2021-04-29").deployments(Deployment.builder().count(0).build()).build());
    totalCountList.add(
        DeploymentDateAndCount.builder().time("2021-04-30").deployments(Deployment.builder().count(3).build()).build());
    totalCountList.add(
        DeploymentDateAndCount.builder().time("2021-05-01").deployments(Deployment.builder().count(4).build()).build());
    totalCountList.add(
        DeploymentDateAndCount.builder().time("2021-05-02").deployments(Deployment.builder().count(1).build()).build());

    List<DeploymentDateAndCount> successCountList = new ArrayList<>();
    successCountList.add(
        DeploymentDateAndCount.builder().time("2021-04-28").deployments(Deployment.builder().count(1).build()).build());
    successCountList.add(
        DeploymentDateAndCount.builder().time("2021-04-29").deployments(Deployment.builder().count(0).build()).build());
    successCountList.add(
        DeploymentDateAndCount.builder().time("2021-04-30").deployments(Deployment.builder().count(1).build()).build());
    successCountList.add(
        DeploymentDateAndCount.builder().time("2021-05-01").deployments(Deployment.builder().count(2).build()).build());
    successCountList.add(
        DeploymentDateAndCount.builder().time("2021-05-02").deployments(Deployment.builder().count(0).build()).build());

    List<DeploymentDateAndCount> failureCountList = new ArrayList<>();
    failureCountList.add(
        DeploymentDateAndCount.builder().time("2021-04-28").deployments(Deployment.builder().count(0).build()).build());
    failureCountList.add(
        DeploymentDateAndCount.builder().time("2021-04-29").deployments(Deployment.builder().count(0).build()).build());
    failureCountList.add(
        DeploymentDateAndCount.builder().time("2021-04-30").deployments(Deployment.builder().count(2).build()).build());
    failureCountList.add(
        DeploymentDateAndCount.builder().time("2021-05-01").deployments(Deployment.builder().count(1).build()).build());
    failureCountList.add(
        DeploymentDateAndCount.builder().time("2021-05-02").deployments(Deployment.builder().count(1).build()).build());

    HealthDeploymentDashboard expectedHealthDeploymentDashboard =
        HealthDeploymentDashboard.builder()
            .healthDeploymentInfo(
                HealthDeploymentInfo.builder()
                    .total(TotalDeploymentInfo.builder()
                               .count(10)
                               .production(4L)
                               .nonProduction(6L)
                               .countList(totalCountList)

                               .build())
                    .success(DeploymentInfo.builder()
                                 .count(4)
                                 .rate((-1 / (double) 5) * 100)
                                 .countList(successCountList)
                                 .build())
                    .failure(DeploymentInfo.builder().count(4).rate(0.0).countList(failureCountList).build())
                    .build())
            .build();

    assertThat(expectedHealthDeploymentDashboard).isEqualTo(healthDeploymentDashboard);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGetExecutionDeploymentDashboard() {
    String startInterval = "2021-04-23";
    String endInterval = "2021-05-02";

    List<String> status = Arrays.asList(ExecutionStatus.SUCCESS.name(), ExecutionStatus.EXPIRED.name(),
        ExecutionStatus.RUNNING.name(), ExecutionStatus.ABORTED.name(), ExecutionStatus.SUCCESS.name(),
        ExecutionStatus.FAILED.name(), ExecutionStatus.FAILED.name(), ExecutionStatus.SUCCESS.name(),
        ExecutionStatus.SUCCESS.name(), ExecutionStatus.WAITING.name(), ExecutionStatus.SUCCESS.name(),
        ExecutionStatus.EXPIRED.name(), ExecutionStatus.RUNNING.name(), ExecutionStatus.ABORTED.name(),
        ExecutionStatus.SUCCESS.name(), ExecutionStatus.SUCCESS.name(), ExecutionStatus.FAILED.name(),
        ExecutionStatus.SUCCESS.name(), ExecutionStatus.SUCCESS.name(), ExecutionStatus.FAILED.name());

    List<String> time = Arrays.asList("2021-04-28 21:50:02.64", "2021-05-01 21:49:11.222", "2021-05-01 21:48:45.39",
        "2021-04-30 21:47:49.771", "2021-05-01 21:46:55.556", "2021-05-02 21:45:27.619", "2021-04-30 21:44:59.73",
        "2021-05-01 21:43:52.675", "2021-04-30 21:43:49.537", "2021-04-28 21:43:40.053", "2021-04-24 21:50:02.64",
        "2021-04-27 21:49:11.222", "2021-04-24 21:48:45.39", "2021-04-25 21:47:49.771", "2021-04-23 21:46:55.556",
        "2021-04-26 21:45:27.619", "2021-04-26 21:44:59.73", "2021-04-24 21:43:52.675", "2021-04-24 21:43:49.537",
        "2021-04-26 21:43:40.053");

    TimeAndStatusDeployment statusAndTime = TimeAndStatusDeployment.builder().status(status).time(time).build();

    doReturn(statusAndTime).when(cdOverviewDashboardServiceImpl).queryCalculatorTimeAndStatus(anyString());

    ExecutionDeploymentInfo executionDeploymentInfo =
        cdOverviewDashboardServiceImpl.getExecutionDeploymentDashboard("acc", "oro", "pro", startInterval, endInterval);

    List<ExecutionDeployment> executionDeploymentList = new ArrayList<>();
    executionDeploymentList.add(ExecutionDeployment.builder()
                                    .time("2021-04-23")
                                    .deployments(DeploymentCount.builder().total(1).success(1).failure(0).build())
                                    .build());
    executionDeploymentList.add(ExecutionDeployment.builder()
                                    .time("2021-04-24")
                                    .deployments(DeploymentCount.builder().total(4).success(3).failure(0).build())
                                    .build());
    executionDeploymentList.add(ExecutionDeployment.builder()
                                    .time("2021-04-25")
                                    .deployments(DeploymentCount.builder().total(1).success(0).failure(1).build())
                                    .build());
    executionDeploymentList.add(ExecutionDeployment.builder()
                                    .time("2021-04-26")
                                    .deployments(DeploymentCount.builder().total(3).success(1).failure(2).build())
                                    .build());
    executionDeploymentList.add(ExecutionDeployment.builder()
                                    .time("2021-04-27")
                                    .deployments(DeploymentCount.builder().total(1).success(0).failure(1).build())
                                    .build());
    executionDeploymentList.add(ExecutionDeployment.builder()
                                    .time("2021-04-28")
                                    .deployments(DeploymentCount.builder().total(2).success(1).failure(0).build())
                                    .build());
    executionDeploymentList.add(ExecutionDeployment.builder()
                                    .time("2021-04-29")
                                    .deployments(DeploymentCount.builder().total(0).success(0).failure(0).build())
                                    .build());
    executionDeploymentList.add(ExecutionDeployment.builder()
                                    .time("2021-04-30")
                                    .deployments(DeploymentCount.builder().total(3).success(1).failure(2).build())
                                    .build());
    executionDeploymentList.add(ExecutionDeployment.builder()
                                    .time("2021-05-01")
                                    .deployments(DeploymentCount.builder().total(4).success(2).failure(1).build())
                                    .build());
    executionDeploymentList.add(ExecutionDeployment.builder()
                                    .time("2021-05-02")
                                    .deployments(DeploymentCount.builder().total(1).success(0).failure(1).build())
                                    .build());

    ExecutionDeploymentInfo expectedExecutionDeploymentInfo =
        ExecutionDeploymentInfo.builder().executionDeploymentList(executionDeploymentList).build();

    assertThat(expectedExecutionDeploymentInfo).isEqualTo(executionDeploymentInfo);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGetWorkloadDeploymentInfoCalculation() {
    String startInterval = "2021-04-28";
    String endInterval = "2021-05-02";
    String previousStartInterval = "2021-04-23";

    List<String> workloads = Arrays.asList("Service1", "Service1", "Service2", "Service3", "Service3", "Service3",
        "Service1", "Service1", "Service3", "Service2", "Service1", "Service1", "Service2", "Service3", "Service3",
        "Service3", "Service1", "Service1", "Service3", "Service2");

    List<String> status = Arrays.asList(ExecutionStatus.SUCCESS.name(), ExecutionStatus.EXPIRED.name(),
        ExecutionStatus.RUNNING.name(), ExecutionStatus.ABORTED.name(), ExecutionStatus.SUCCESS.name(),
        ExecutionStatus.FAILED.name(), ExecutionStatus.FAILED.name(), ExecutionStatus.SUCCESS.name(),
        ExecutionStatus.SUCCESS.name(), ExecutionStatus.WAITING.name(), ExecutionStatus.SUCCESS.name(),
        ExecutionStatus.EXPIRED.name(), ExecutionStatus.RUNNING.name(), ExecutionStatus.ABORTED.name(),
        ExecutionStatus.SUCCESS.name(), ExecutionStatus.SUCCESS.name(), ExecutionStatus.FAILED.name(),
        ExecutionStatus.SUCCESS.name(), ExecutionStatus.SUCCESS.name(), ExecutionStatus.FAILED.name());

    List<String> time = Arrays.asList("2021-04-28 21:50:02.64", "2021-05-01 21:49:11.222", "2021-05-01 21:48:45.39",
        "2021-04-30 21:47:49.771", "2021-05-01 21:46:55.556", "2021-05-02 21:45:27.619", "2021-04-30 21:44:59.73",
        "2021-05-01 21:43:52.675", "2021-04-30 21:43:49.537", "2021-04-28 21:43:40.053", "2021-04-24 21:50:02.64",
        "2021-04-27 21:49:11.222", "2021-04-24 21:48:45.39", "2021-04-25 21:47:49.771", "2021-04-23 21:46:55.556",
        "2021-04-26 21:45:27.619", "2021-04-26 21:44:59.73", "2021-04-24 21:43:52.675", "2021-04-24 21:43:49.537",
        "2021-04-26 21:43:40.053");

    HashMap<String, Integer> hashMap = new HashMap<>();
    hashMap.put("Service1", 1);
    hashMap.put("Service2", 1);
    hashMap.put("Service3", 1);

    DashboardWorkloadDeployment dashboardWorkloadDeployment =
        cdOverviewDashboardServiceImpl.getWorkloadDeploymentInfoCalculation(workloads, status, time,
            Arrays.asList(
                "kuber1", "kuber2", "kuber1", "kuber3", "kuber3", "kuber1", "kuber4", "kuber2", "kuber2", "kuber1"),
            null, hashMap, LocalDate.parse(startInterval), LocalDate.parse(endInterval));

    List<WorkloadDeploymentInfo> workloadDeploymentInfos = new ArrayList<>();

    List<WorkloadDateCountInfo> service1WorkloadDateCount = new ArrayList<>();
    service1WorkloadDateCount.add(WorkloadDateCountInfo.builder()
                                      .date("2021-04-28")
                                      .execution(WorkloadCountInfo.builder().count(1).build())
                                      .build());
    service1WorkloadDateCount.add(WorkloadDateCountInfo.builder()
                                      .date("2021-04-29")
                                      .execution(WorkloadCountInfo.builder().count(0).build())
                                      .build());
    service1WorkloadDateCount.add(WorkloadDateCountInfo.builder()
                                      .date("2021-04-30")
                                      .execution(WorkloadCountInfo.builder().count(1).build())
                                      .build());
    service1WorkloadDateCount.add(WorkloadDateCountInfo.builder()
                                      .date("2021-05-01")
                                      .execution(WorkloadCountInfo.builder().count(2).build())
                                      .build());
    service1WorkloadDateCount.add(WorkloadDateCountInfo.builder()
                                      .date("2021-05-02")
                                      .execution(WorkloadCountInfo.builder().count(0).build())
                                      .build());

    List<WorkloadDateCountInfo> service2WorkloadDateCount = new ArrayList<>();
    service2WorkloadDateCount.add(WorkloadDateCountInfo.builder()
                                      .date("2021-04-28")
                                      .execution(WorkloadCountInfo.builder().count(1).build())
                                      .build());
    service2WorkloadDateCount.add(WorkloadDateCountInfo.builder()
                                      .date("2021-04-29")
                                      .execution(WorkloadCountInfo.builder().count(0).build())
                                      .build());
    service2WorkloadDateCount.add(WorkloadDateCountInfo.builder()
                                      .date("2021-04-30")
                                      .execution(WorkloadCountInfo.builder().count(0).build())
                                      .build());
    service2WorkloadDateCount.add(WorkloadDateCountInfo.builder()
                                      .date("2021-05-01")
                                      .execution(WorkloadCountInfo.builder().count(1).build())
                                      .build());
    service2WorkloadDateCount.add(WorkloadDateCountInfo.builder()
                                      .date("2021-05-02")
                                      .execution(WorkloadCountInfo.builder().count(0).build())
                                      .build());

    List<WorkloadDateCountInfo> service3WorkloadDateCount = new ArrayList<>();
    service3WorkloadDateCount.add(WorkloadDateCountInfo.builder()
                                      .date("2021-04-28")
                                      .execution(WorkloadCountInfo.builder().count(0).build())
                                      .build());
    service3WorkloadDateCount.add(WorkloadDateCountInfo.builder()
                                      .date("2021-04-29")
                                      .execution(WorkloadCountInfo.builder().count(0).build())
                                      .build());
    service3WorkloadDateCount.add(WorkloadDateCountInfo.builder()
                                      .date("2021-04-30")
                                      .execution(WorkloadCountInfo.builder().count(2).build())
                                      .build());
    service3WorkloadDateCount.add(WorkloadDateCountInfo.builder()
                                      .date("2021-05-01")
                                      .execution(WorkloadCountInfo.builder().count(1).build())
                                      .build());
    service3WorkloadDateCount.add(WorkloadDateCountInfo.builder()
                                      .date("2021-05-02")
                                      .execution(WorkloadCountInfo.builder().count(1).build())
                                      .build());

    workloadDeploymentInfos.add(WorkloadDeploymentInfo.builder()
                                    .serviceName("Service3")
                                    .lastStatus(ExecutionStatus.FAILED.name())
                                    .deploymentType("kuber1")
                                    .rateSuccess(((-1) / (double) 3) * 100)
                                    .percentSuccess((2 / (double) 4) * 100)
                                    .lastExecuted("2021-05-02 21:45:27.619")
                                    .totalDeployments(4)
                                    .workload(service3WorkloadDateCount)
                                    .build());

    workloadDeploymentInfos.add(WorkloadDeploymentInfo.builder()
                                    .serviceName("Service2")
                                    .lastStatus(ExecutionStatus.RUNNING.name())
                                    .deploymentType("kuber1")
                                    .rateSuccess(0.0)
                                    .percentSuccess(0.0)
                                    .lastExecuted("2021-05-01 21:48:45.39")
                                    .totalDeployments(2)
                                    .workload(service2WorkloadDateCount)
                                    .build());

    workloadDeploymentInfos.add(WorkloadDeploymentInfo.builder()
                                    .serviceName("Service1")
                                    .lastStatus(ExecutionStatus.EXPIRED.name())
                                    .deploymentType("kuber2")
                                    .rateSuccess(0.0)
                                    .percentSuccess((2 / (double) 4) * 100)
                                    .lastExecuted("2021-05-01 21:49:11.222")
                                    .totalDeployments(4)
                                    .workload(service1WorkloadDateCount)
                                    .build());

    DashboardWorkloadDeployment expectedWorkloadDeployment =
        DashboardWorkloadDeployment.builder().workloadDeploymentInfoList(workloadDeploymentInfos).build();

    assertThat(expectedWorkloadDeployment).isEqualTo(dashboardWorkloadDeployment);
  }

  @Test
  @Owner(developers = MEENAKSHI)
  @Category(UnitTests.class)
  public void testGetDeploymentsExecutionInfo() {
    String prevStartInterval = "2021-04-23";
    String prevEndInterval = "2021-04-27";
    String startInterval = "2021-04-28";
    String endInterval = "2021-05-02";

    List<ExecutionDeployment> executionDeploymentList = new ArrayList<>();
    List<ExecutionDeployment> prevExecutionDeploymentList = new ArrayList<>();
    prevExecutionDeploymentList.add(ExecutionDeployment.builder()
                                        .time("2021-04-23")
                                        .deployments(DeploymentCount.builder().total(1).success(1).failure(0).build())
                                        .build());
    prevExecutionDeploymentList.add(ExecutionDeployment.builder()
                                        .time("2021-04-24")
                                        .deployments(DeploymentCount.builder().total(4).success(3).failure(0).build())
                                        .build());
    prevExecutionDeploymentList.add(ExecutionDeployment.builder()
                                        .time("2021-04-25")
                                        .deployments(DeploymentCount.builder().total(1).success(0).failure(1).build())
                                        .build());
    prevExecutionDeploymentList.add(ExecutionDeployment.builder()
                                        .time("2021-04-26")
                                        .deployments(DeploymentCount.builder().total(3).success(1).failure(2).build())
                                        .build());
    prevExecutionDeploymentList.add(ExecutionDeployment.builder()
                                        .time("2021-04-27")
                                        .deployments(DeploymentCount.builder().total(1).success(0).failure(1).build())
                                        .build());

    executionDeploymentList.add(ExecutionDeployment.builder()
                                    .time("2021-04-28")
                                    .deployments(DeploymentCount.builder().total(2).success(1).failure(0).build())
                                    .build());
    executionDeploymentList.add(ExecutionDeployment.builder()
                                    .time("2021-04-29")
                                    .deployments(DeploymentCount.builder().total(0).success(0).failure(0).build())
                                    .build());
    executionDeploymentList.add(ExecutionDeployment.builder()
                                    .time("2021-04-30")
                                    .deployments(DeploymentCount.builder().total(3).success(1).failure(2).build())
                                    .build());
    executionDeploymentList.add(ExecutionDeployment.builder()
                                    .time("2021-05-01")
                                    .deployments(DeploymentCount.builder().total(4).success(2).failure(1).build())
                                    .build());
    executionDeploymentList.add(ExecutionDeployment.builder()
                                    .time("2021-05-02")
                                    .deployments(DeploymentCount.builder().total(1).success(0).failure(1).build())
                                    .build());

    ExecutionDeploymentInfo executionDeploymentInfo =
        ExecutionDeploymentInfo.builder().executionDeploymentList(executionDeploymentList).build();
    ExecutionDeploymentInfo prevExecutionDeploymentInfo =
        ExecutionDeploymentInfo.builder().executionDeploymentList(prevExecutionDeploymentList).build();

    doReturn(executionDeploymentInfo)
        .when(cdOverviewDashboardServiceImpl)
        .getExecutionDeploymentDashboard("acc", "org", "pro", startInterval, endInterval);
    doReturn(prevExecutionDeploymentInfo)
        .when(cdOverviewDashboardServiceImpl)
        .getExecutionDeploymentDashboard("acc", "org", "pro", prevStartInterval, prevEndInterval);

    ExecutionDeploymentDetailInfo deploymentsExecutionInfo =
        cdOverviewDashboardServiceImpl.getDeploymentsExecutionInfo("acc", "org", "pro", startInterval, endInterval);

    assertThat(deploymentsExecutionInfo.getExecutionDeploymentList()).isEqualTo(executionDeploymentList);
    assertThat(deploymentsExecutionInfo.getTotalDeployments()).isEqualTo(10);
    assertThat(deploymentsExecutionInfo.getFrequency()).isEqualTo(2.0);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGetDeploymentActiveFailedRunningInfo() {
    List<String> planExecutionIdListFailure = Arrays.asList("11", "12", "13", "14", "15", "16", "17", "18");
    List<String> namePipelineListFailure =
        Arrays.asList("name1", "name2", "name3", "name4", "name5", "name1", "name2", "name3");
    List<String> startTsFailure = Arrays.asList("2021-04-28 21:50:02.64", "2021-05-01 21:49:11.222",
        "2021-05-01 21:48:45.39", "2021-04-30 21:47:49.771", "2021-05-01 21:46:55.556", "2021-05-02 21:45:27.619",
        "2021-04-30 21:44:59.73", "2021-05-01 21:43:52.675");
    List<String> endTsFailure = Arrays.asList("2021-05-28 21:50:02.64", "2021-06-01 21:49:11.222",
        "2021-06-01 21:48:45.39", "2021-05-30 21:47:49.771", "2021-06-01 21:46:55.556", "2021-06-02 21:45:27.619",
        "2021-05-30 21:44:59.73", "2021-06-01 21:43:52.675");
    List<String> deploymentStatusFailure = Arrays.asList(failedStatusList.get(0), failedStatusList.get(1),
        failedStatusList.get(0), failedStatusList.get(1), failedStatusList.get(0), failedStatusList.get(2),
        failedStatusList.get(2), failedStatusList.get(0));

    DeploymentStatusInfoList deploymentStatusInfoListFailure = DeploymentStatusInfoList.builder()
                                                                   .planExecutionIdList(planExecutionIdListFailure)
                                                                   .startTs(startTsFailure)
                                                                   .namePipelineList(namePipelineListFailure)
                                                                   .endTs(endTsFailure)
                                                                   .deploymentStatus(deploymentStatusFailure)
                                                                   .build();

    // active list
    List<String> planExecutionIdListActive = Arrays.asList("21", "22", "23", "24", "25", "26", "27", "28");
    List<String> namePipelineListActive =
        Arrays.asList("name1", "name2", "name3", "name4", "name5", "name1", "name2", "name3");
    List<String> startTsActive = Arrays.asList("2021-04-28 21:50:02.64", "2021-05-01 21:49:11.222",
        "2021-05-01 21:48:45.39", "2021-04-30 21:47:49.771", "2021-05-01 21:46:55.556", "2021-05-02 21:45:27.619",
        "2021-04-30 21:44:59.73", "2021-05-01 21:43:52.675");
    List<String> endTsActive = Arrays.asList("2021-05-28 21:50:02.64", "2021-06-01 21:49:11.222",
        "2021-06-01 21:48:45.39", "2021-05-30 21:47:49.771", "2021-06-01 21:46:55.556", "2021-06-02 21:45:27.619",
        "2021-05-30 21:44:59.73", "2021-06-01 21:43:52.675");
    List<String> deploymentStatusActive = Arrays.asList(activeStatusList.get(0), activeStatusList.get(1),
        activeStatusList.get(0), activeStatusList.get(1), activeStatusList.get(0), activeStatusList.get(0),
        activeStatusList.get(1), activeStatusList.get(0));

    DeploymentStatusInfoList deploymentStatusInfoListActive = DeploymentStatusInfoList.builder()
                                                                  .planExecutionIdList(planExecutionIdListActive)
                                                                  .startTs(startTsActive)
                                                                  .namePipelineList(namePipelineListActive)
                                                                  .endTs(endTsActive)
                                                                  .deploymentStatus(deploymentStatusActive)
                                                                  .build();

    // pending list
    List<String> planExecutionIdListPending = Arrays.asList("31", "32", "33", "34", "35", "36", "37", "38");
    List<String> namePipelineListPending =
        Arrays.asList("name1", "name2", "name3", "name4", "name5", "name1", "name2", "name3");
    List<String> startTsPending = Arrays.asList("2021-04-28 21:50:02.64", "2021-05-01 21:49:11.222",
        "2021-05-01 21:48:45.39", "2021-04-30 21:47:49.771", "2021-05-01 21:46:55.556", "2021-05-02 21:45:27.619",
        "2021-04-30 21:44:59.73", "2021-05-01 21:43:52.675");
    List<String> endTsPending = Arrays.asList("2021-05-28 21:50:02.64", "2021-06-01 21:49:11.222",
        "2021-06-01 21:48:45.39", "2021-05-30 21:47:49.771", "2021-06-01 21:46:55.556", "2021-06-02 21:45:27.619",
        "2021-05-30 21:44:59.73", "2021-06-01 21:43:52.675");
    List<String> deploymentStatusPending = Arrays.asList(pendingStatusList.get(0), pendingStatusList.get(1),
        pendingStatusList.get(0), pendingStatusList.get(1), pendingStatusList.get(0), pendingStatusList.get(0),
        pendingStatusList.get(1), pendingStatusList.get(0));
    DeploymentStatusInfoList deploymentStatusInfoListPending = DeploymentStatusInfoList.builder()
                                                                   .planExecutionIdList(planExecutionIdListPending)
                                                                   .startTs(startTsPending)
                                                                   .namePipelineList(namePipelineListPending)
                                                                   .endTs(endTsPending)
                                                                   .deploymentStatus(deploymentStatusPending)
                                                                   .build();

    String queryFailed = cdOverviewDashboardServiceImpl.queryBuilderStatus("acc", "orgId", "pro", 10, failedStatusList);

    String queryActive = cdOverviewDashboardServiceImpl.queryBuilderStatus("acc", "orgId", "pro", 10, activeStatusList);

    String queryPending =
        cdOverviewDashboardServiceImpl.queryBuilderStatus("acc", "orgId", "pro", 10, pendingStatusList);

    // failure
    doReturn(deploymentStatusInfoListFailure)
        .when(cdOverviewDashboardServiceImpl)
        .queryCalculatorDeploymentInfo(queryFailed);

    String serviveTagQueryFailure = cdOverviewDashboardServiceImpl.queryBuilderServiceTag(
        "acc", "orgId", "pro", planExecutionIdListFailure, failedStatusList);

    HashMap<String, List<ServiceDeploymentInfo>> serviceTagMapFailure = new HashMap<>();
    serviceTagMapFailure.put("11",
        Arrays.asList(ServiceDeploymentInfo.builder().serviceName("serviceF1").serviceTag("tagF1").build(),
            ServiceDeploymentInfo.builder().serviceName("serviceF2").serviceTag(null).build()));

    serviceTagMapFailure.put(
        "13", Arrays.asList(ServiceDeploymentInfo.builder().serviceName("serviceF3").serviceTag("tagF3").build()));

    serviceTagMapFailure.put("15",
        Arrays.asList(ServiceDeploymentInfo.builder().serviceName("serviceF1").serviceTag("tagF1").build(),
            ServiceDeploymentInfo.builder().serviceName("serviceF2").serviceTag("tagF2").build()));

    doReturn(serviceTagMapFailure)
        .when(cdOverviewDashboardServiceImpl)
        .queryCalculatorServiceTagMag(serviveTagQueryFailure);

    // Active

    doReturn(deploymentStatusInfoListActive)
        .when(cdOverviewDashboardServiceImpl)
        .queryCalculatorDeploymentInfo(queryActive);

    String serviveTagQueryActive = cdOverviewDashboardServiceImpl.queryBuilderServiceTag(
        "acc", "orgId", "pro", planExecutionIdListActive, activeStatusList);

    HashMap<String, List<ServiceDeploymentInfo>> serviceTagMapActive = new HashMap<>();

    doReturn(serviceTagMapActive)
        .when(cdOverviewDashboardServiceImpl)
        .queryCalculatorServiceTagMag(serviveTagQueryActive);

    // Pending
    doReturn(deploymentStatusInfoListPending)
        .when(cdOverviewDashboardServiceImpl)
        .queryCalculatorDeploymentInfo(queryPending);

    String serviveTagQueryPending = cdOverviewDashboardServiceImpl.queryBuilderServiceTag(
        "acc", "orgId", "pro", planExecutionIdListPending, pendingStatusList);

    HashMap<String, List<ServiceDeploymentInfo>> serviceTagMapPending = new HashMap<>();

    doReturn(serviceTagMapPending)
        .when(cdOverviewDashboardServiceImpl)
        .queryCalculatorServiceTagMag(serviveTagQueryPending);

    DashboardDeploymentActiveFailedRunningInfo dashboardDeploymentActiveFailedRunningInfo =
        cdOverviewDashboardServiceImpl.getDeploymentActiveFailedRunningInfo("acc", "orgId", "pro", 10);

    // failure

    List<DeploymentStatusInfo> failureStatusInfo = new ArrayList<>();
    failureStatusInfo.add(DeploymentStatusInfo.builder()
                              .name("name1")
                              .startTs("2021-04-28 21:50:02.64")
                              .endTs("2021-05-28 21:50:02.64")
                              .status(failedStatusList.get(0))
                              .serviceInfoList(serviceTagMapFailure.get("11"))
                              .build());
    failureStatusInfo.add(DeploymentStatusInfo.builder()
                              .name("name2")
                              .startTs("2021-05-01 21:49:11.222")
                              .endTs("2021-06-01 21:49:11.222")
                              .status(failedStatusList.get(1))
                              .build());
    failureStatusInfo.add(DeploymentStatusInfo.builder()
                              .name("name3")
                              .startTs("2021-05-01 21:48:45.39")
                              .endTs("2021-06-01 21:48:45.39")
                              .status(failedStatusList.get(0))
                              .serviceInfoList(serviceTagMapFailure.get("13"))
                              .build());
    failureStatusInfo.add(DeploymentStatusInfo.builder()
                              .name("name4")
                              .startTs("2021-04-30 21:47:49.771")
                              .endTs("2021-05-30 21:47:49.771")
                              .status(failedStatusList.get(1))
                              .build());
    failureStatusInfo.add(DeploymentStatusInfo.builder()
                              .name("name5")
                              .startTs("2021-05-01 21:46:55.556")
                              .endTs("2021-06-01 21:46:55.556")
                              .status(failedStatusList.get(0))
                              .serviceInfoList(serviceTagMapFailure.get("15"))
                              .build());
    failureStatusInfo.add(DeploymentStatusInfo.builder()
                              .name("name1")
                              .startTs("2021-05-02 21:45:27.619")
                              .endTs("2021-06-02 21:45:27.619")
                              .status(failedStatusList.get(2))
                              .build());
    failureStatusInfo.add(DeploymentStatusInfo.builder()
                              .name("name2")
                              .startTs("2021-04-30 21:44:59.73")
                              .endTs("2021-05-30 21:44:59.73")
                              .status(failedStatusList.get(2))
                              .build());
    failureStatusInfo.add(DeploymentStatusInfo.builder()
                              .name("name3")
                              .startTs("2021-05-01 21:43:52.675")
                              .endTs("2021-06-01 21:43:52.675")
                              .status(failedStatusList.get(0))
                              .build());

    // active
    List<DeploymentStatusInfo> activeStatusInfo = new ArrayList<>();
    activeStatusInfo.add(DeploymentStatusInfo.builder()
                             .name("name1")
                             .startTs("2021-04-28 21:50:02.64")
                             .endTs("2021-05-28 21:50:02.64")
                             .status(activeStatusList.get(0))
                             .build());
    activeStatusInfo.add(DeploymentStatusInfo.builder()
                             .name("name2")
                             .startTs("2021-05-01 21:49:11.222")
                             .endTs("2021-06-01 21:49:11.222")
                             .status(activeStatusList.get(1))
                             .build());
    activeStatusInfo.add(DeploymentStatusInfo.builder()
                             .name("name3")
                             .startTs("2021-05-01 21:48:45.39")
                             .endTs("2021-06-01 21:48:45.39")
                             .status(activeStatusList.get(0))
                             .build());
    activeStatusInfo.add(DeploymentStatusInfo.builder()
                             .name("name4")
                             .startTs("2021-04-30 21:47:49.771")
                             .endTs("2021-05-30 21:47:49.771")
                             .status(activeStatusList.get(1))
                             .build());
    activeStatusInfo.add(DeploymentStatusInfo.builder()
                             .name("name5")
                             .startTs("2021-05-01 21:46:55.556")
                             .endTs("2021-06-01 21:46:55.556")
                             .status(activeStatusList.get(0))
                             .build());
    activeStatusInfo.add(DeploymentStatusInfo.builder()
                             .name("name1")
                             .startTs("2021-05-02 21:45:27.619")
                             .endTs("2021-06-02 21:45:27.619")
                             .status(activeStatusList.get(0))
                             .build());
    activeStatusInfo.add(DeploymentStatusInfo.builder()
                             .name("name2")
                             .startTs("2021-04-30 21:44:59.73")
                             .endTs("2021-05-30 21:44:59.73")
                             .status(activeStatusList.get(1))
                             .build());
    activeStatusInfo.add(DeploymentStatusInfo.builder()
                             .name("name3")
                             .startTs("2021-05-01 21:43:52.675")
                             .endTs("2021-06-01 21:43:52.675")
                             .status(activeStatusList.get(0))
                             .build());

    // pending
    List<DeploymentStatusInfo> pendingStatusInfo = new ArrayList<>();
    pendingStatusInfo.add(DeploymentStatusInfo.builder()
                              .name("name1")
                              .startTs("2021-04-28 21:50:02.64")
                              .endTs("2021-05-28 21:50:02.64")
                              .status(pendingStatusList.get(0))
                              .build());
    pendingStatusInfo.add(DeploymentStatusInfo.builder()
                              .name("name2")
                              .startTs("2021-05-01 21:49:11.222")
                              .endTs("2021-06-01 21:49:11.222")
                              .status(pendingStatusList.get(1))
                              .build());
    pendingStatusInfo.add(DeploymentStatusInfo.builder()
                              .name("name3")
                              .startTs("2021-05-01 21:48:45.39")
                              .endTs("2021-06-01 21:48:45.39")
                              .status(pendingStatusList.get(0))
                              .build());
    pendingStatusInfo.add(DeploymentStatusInfo.builder()
                              .name("name4")
                              .startTs("2021-04-30 21:47:49.771")
                              .endTs("2021-05-30 21:47:49.771")
                              .status(pendingStatusList.get(1))
                              .build());
    pendingStatusInfo.add(DeploymentStatusInfo.builder()
                              .name("name5")
                              .startTs("2021-05-01 21:46:55.556")
                              .endTs("2021-06-01 21:46:55.556")
                              .status(pendingStatusList.get(0))
                              .build());
    pendingStatusInfo.add(DeploymentStatusInfo.builder()
                              .name("name1")
                              .startTs("2021-05-02 21:45:27.619")
                              .endTs("2021-06-02 21:45:27.619")
                              .status(pendingStatusList.get(0))
                              .build());
    pendingStatusInfo.add(DeploymentStatusInfo.builder()
                              .name("name2")
                              .startTs("2021-04-30 21:44:59.73")
                              .endTs("2021-05-30 21:44:59.73")
                              .status(pendingStatusList.get(1))
                              .build());
    pendingStatusInfo.add(DeploymentStatusInfo.builder()
                              .name("name3")
                              .startTs("2021-05-01 21:43:52.675")
                              .endTs("2021-06-01 21:43:52.675")
                              .status(pendingStatusList.get(0))
                              .build());

    DashboardDeploymentActiveFailedRunningInfo expectedResult = DashboardDeploymentActiveFailedRunningInfo.builder()
                                                                    .failure(failureStatusInfo)
                                                                    .active(activeStatusInfo)
                                                                    .pending(pendingStatusInfo)
                                                                    .build();

    assertThat(expectedResult).isEqualTo(dashboardDeploymentActiveFailedRunningInfo);
  }
}
