package io.harness.ng.cdoverview;

import static io.harness.rule.OwnerRule.MEENAKSHI;
import static io.harness.rule.OwnerRule.PRASHANTSHARMA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ng.cdOverview.dto.DashboardDeploymentActiveFailedRunningInfo;
import io.harness.ng.cdOverview.dto.DashboardWorkloadDeployment;
import io.harness.ng.cdOverview.dto.Deployment;
import io.harness.ng.cdOverview.dto.DeploymentCount;
import io.harness.ng.cdOverview.dto.DeploymentDateAndCount;
import io.harness.ng.cdOverview.dto.DeploymentInfo;
import io.harness.ng.cdOverview.dto.DeploymentStatusInfo;
import io.harness.ng.cdOverview.dto.DeploymentStatusInfoList;
import io.harness.ng.cdOverview.dto.ExecutionDeployment;
import io.harness.ng.cdOverview.dto.ExecutionDeploymentInfo;
import io.harness.ng.cdOverview.dto.HealthDeploymentDashboard;
import io.harness.ng.cdOverview.dto.HealthDeploymentInfo;
import io.harness.ng.cdOverview.dto.LastWorkloadInfo;
import io.harness.ng.cdOverview.dto.ServiceDeployment;
import io.harness.ng.cdOverview.dto.ServiceDeploymentInfo;
import io.harness.ng.cdOverview.dto.ServiceDeploymentInfoDTO;
import io.harness.ng.cdOverview.dto.ServiceDeploymentListInfo;
import io.harness.ng.cdOverview.dto.TimeAndStatusDeployment;
import io.harness.ng.cdOverview.dto.TotalDeploymentInfo;
import io.harness.ng.cdOverview.dto.WorkloadCountInfo;
import io.harness.ng.cdOverview.dto.WorkloadDateCountInfo;
import io.harness.ng.cdOverview.dto.WorkloadDeploymentInfo;
import io.harness.ng.cdOverview.service.CDOverviewDashboardServiceImpl;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.rule.Owner;
import io.harness.timescaledb.TimeScaleDBService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
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
  private List<String> pendingStatusList = Arrays.asList(ExecutionStatus.INTERVENTIONWAITING.name(),
      ExecutionStatus.APPROVALWAITING.name(), ExecutionStatus.WAITING.name());

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGetHealthDeploymentDashboard() {
    long startInterval = 1619568000000L;
    long endInterval = 1619913600000L;
    long previousInterval = 1619136000000L;

    List<String> status = Arrays.asList(ExecutionStatus.SUCCESS.name(), ExecutionStatus.EXPIRED.name(),
        ExecutionStatus.RUNNING.name(), ExecutionStatus.ABORTED.name(), ExecutionStatus.SUCCESS.name(),
        ExecutionStatus.FAILED.name(), ExecutionStatus.FAILED.name(), ExecutionStatus.SUCCESS.name(),
        ExecutionStatus.SUCCESS.name(), ExecutionStatus.WAITING.name(), ExecutionStatus.SUCCESS.name(),
        ExecutionStatus.EXPIRED.name(), ExecutionStatus.RUNNING.name(), ExecutionStatus.ABORTED.name(),
        ExecutionStatus.SUCCESS.name(), ExecutionStatus.SUCCESS.name(), ExecutionStatus.FAILED.name(),
        ExecutionStatus.SUCCESS.name(), ExecutionStatus.SUCCESS.name(), ExecutionStatus.FAILED.name());

    List<Long> time = Arrays.asList(1619626802000L, 1619885951000L, 1619885925000L, 1619799469000L, 1619885815000L,
        1619972127000L, 1619799299000L, 1619885632000L, 1619799229000L, 1619626420000L, 1619281202000L, 1619540351000L,
        1619281125000L, 1619367469000L, 1619194615000L, 1619453727000L, 1619453699000L, 1619280832000L, 1619280829000L,
        1619453620000L);

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
    totalCountList.add(DeploymentDateAndCount.builder()
                           .time(1619568000000L)
                           .deployments(Deployment.builder().count(2).build())
                           .build());
    totalCountList.add(DeploymentDateAndCount.builder()
                           .time(1619654400000L)
                           .deployments(Deployment.builder().count(0).build())
                           .build());
    totalCountList.add(DeploymentDateAndCount.builder()
                           .time(1619740800000L)
                           .deployments(Deployment.builder().count(3).build())
                           .build());
    totalCountList.add(DeploymentDateAndCount.builder()
                           .time(1619827200000L)
                           .deployments(Deployment.builder().count(4).build())
                           .build());
    totalCountList.add(DeploymentDateAndCount.builder()
                           .time(1619913600000L)
                           .deployments(Deployment.builder().count(1).build())
                           .build());

    List<DeploymentDateAndCount> successCountList = new ArrayList<>();
    successCountList.add(DeploymentDateAndCount.builder()
                             .time(1619568000000L)
                             .deployments(Deployment.builder().count(1).build())
                             .build());
    successCountList.add(DeploymentDateAndCount.builder()
                             .time(1619654400000L)
                             .deployments(Deployment.builder().count(0).build())
                             .build());
    successCountList.add(DeploymentDateAndCount.builder()
                             .time(1619740800000L)
                             .deployments(Deployment.builder().count(1).build())
                             .build());
    successCountList.add(DeploymentDateAndCount.builder()
                             .time(1619827200000L)
                             .deployments(Deployment.builder().count(2).build())
                             .build());
    successCountList.add(DeploymentDateAndCount.builder()
                             .time(1619913600000L)
                             .deployments(Deployment.builder().count(0).build())
                             .build());

    List<DeploymentDateAndCount> failureCountList = new ArrayList<>();
    failureCountList.add(DeploymentDateAndCount.builder()
                             .time(1619568000000L)
                             .deployments(Deployment.builder().count(0).build())
                             .build());
    failureCountList.add(DeploymentDateAndCount.builder()
                             .time(1619654400000L)
                             .deployments(Deployment.builder().count(0).build())
                             .build());
    failureCountList.add(DeploymentDateAndCount.builder()
                             .time(1619740800000L)
                             .deployments(Deployment.builder().count(2).build())
                             .build());
    failureCountList.add(DeploymentDateAndCount.builder()
                             .time(1619827200000L)
                             .deployments(Deployment.builder().count(1).build())
                             .build());
    failureCountList.add(DeploymentDateAndCount.builder()
                             .time(1619913600000L)
                             .deployments(Deployment.builder().count(1).build())
                             .build());

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
    long startInterval = 1619136000000L;
    long endInterval = 1619913600000L;

    List<String> status = Arrays.asList(ExecutionStatus.SUCCESS.name(), ExecutionStatus.EXPIRED.name(),
        ExecutionStatus.RUNNING.name(), ExecutionStatus.ABORTED.name(), ExecutionStatus.SUCCESS.name(),
        ExecutionStatus.FAILED.name(), ExecutionStatus.FAILED.name(), ExecutionStatus.SUCCESS.name(),
        ExecutionStatus.SUCCESS.name(), ExecutionStatus.WAITING.name(), ExecutionStatus.SUCCESS.name(),
        ExecutionStatus.EXPIRED.name(), ExecutionStatus.RUNNING.name(), ExecutionStatus.ABORTED.name(),
        ExecutionStatus.SUCCESS.name(), ExecutionStatus.SUCCESS.name(), ExecutionStatus.FAILED.name(),
        ExecutionStatus.SUCCESS.name(), ExecutionStatus.SUCCESS.name(), ExecutionStatus.FAILED.name());

    List<Long> time = Arrays.asList(1619626802000L, 1619885951000L, 1619885925000L, 1619799469000L, 1619885815000L,
        1619972127000L, 1619799299000L, 1619885632000L, 1619799229000L, 1619626420000L, 1619281202000L, 1619540351000L,
        1619281125000L, 1619367469000L, 1619194615000L, 1619453727000L, 1619453699000L, 1619280832000L, 1619280829000L,
        1619453620000L);

    TimeAndStatusDeployment statusAndTime = TimeAndStatusDeployment.builder().status(status).time(time).build();

    doReturn(statusAndTime).when(cdOverviewDashboardServiceImpl).queryCalculatorTimeAndStatus(anyString());

    ExecutionDeploymentInfo executionDeploymentInfo =
        cdOverviewDashboardServiceImpl.getExecutionDeploymentDashboard("acc", "oro", "pro", startInterval, endInterval);

    List<ExecutionDeployment> executionDeploymentList = new ArrayList<>();
    executionDeploymentList.add(ExecutionDeployment.builder()
                                    .time(1619136000000L)
                                    .deployments(DeploymentCount.builder().total(1).success(1).failure(0).build())
                                    .build());
    executionDeploymentList.add(ExecutionDeployment.builder()
                                    .time(1619222400000L)
                                    .deployments(DeploymentCount.builder().total(4).success(3).failure(0).build())
                                    .build());
    executionDeploymentList.add(ExecutionDeployment.builder()
                                    .time(1619308800000L)
                                    .deployments(DeploymentCount.builder().total(1).success(0).failure(1).build())
                                    .build());
    executionDeploymentList.add(ExecutionDeployment.builder()
                                    .time(1619395200000L)
                                    .deployments(DeploymentCount.builder().total(3).success(1).failure(2).build())
                                    .build());
    executionDeploymentList.add(ExecutionDeployment.builder()
                                    .time(1619481600000L)
                                    .deployments(DeploymentCount.builder().total(1).success(0).failure(1).build())
                                    .build());
    executionDeploymentList.add(ExecutionDeployment.builder()
                                    .time(1619568000000L)
                                    .deployments(DeploymentCount.builder().total(2).success(1).failure(0).build())
                                    .build());
    executionDeploymentList.add(ExecutionDeployment.builder()
                                    .time(1619654400000L)
                                    .deployments(DeploymentCount.builder().total(0).success(0).failure(0).build())
                                    .build());
    executionDeploymentList.add(ExecutionDeployment.builder()
                                    .time(1619740800000L)
                                    .deployments(DeploymentCount.builder().total(3).success(1).failure(2).build())
                                    .build());
    executionDeploymentList.add(ExecutionDeployment.builder()
                                    .time(1619827200000L)
                                    .deployments(DeploymentCount.builder().total(4).success(2).failure(1).build())
                                    .build());
    executionDeploymentList.add(ExecutionDeployment.builder()
                                    .time(1619913600000L)
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
    long startInterval = 1619568000000L;
    long endInterval = 1620000000000L;
    long previousStartInterval = 1619136000000L;

    List<String> workloadsId = Arrays.asList("ServiceId1", "ServiceId1", "ServiceId2", "ServiceId3", "ServiceId3",
        "ServiceId3", "ServiceId1", "ServiceId1", "ServiceId3", "ServiceId2", "ServiceId1", "ServiceId1", "ServiceId2",
        "ServiceId3", "ServiceId3", "ServiceId3", "ServiceId1", "ServiceId1", "ServiceId3", "ServiceId2");
    List<String> deploymentTypeList = Arrays.asList(
        "kuber1", "kuber2", "kuber1", "kuber3", "kuber3", "kuber1", "kuber4", "kuber2", "kuber2", "kuber1");

    List<String> status = Arrays.asList(ExecutionStatus.SUCCESS.name(), ExecutionStatus.EXPIRED.name(),
        ExecutionStatus.RUNNING.name(), ExecutionStatus.ABORTED.name(), ExecutionStatus.SUCCESS.name(),
        ExecutionStatus.FAILED.name(), ExecutionStatus.FAILED.name(), ExecutionStatus.SUCCESS.name(),
        ExecutionStatus.SUCCESS.name(), ExecutionStatus.WAITING.name(), ExecutionStatus.SUCCESS.name(),
        ExecutionStatus.EXPIRED.name(), ExecutionStatus.RUNNING.name(), ExecutionStatus.ABORTED.name(),
        ExecutionStatus.SUCCESS.name(), ExecutionStatus.SUCCESS.name(), ExecutionStatus.FAILED.name(),
        ExecutionStatus.SUCCESS.name(), ExecutionStatus.SUCCESS.name(), ExecutionStatus.FAILED.name());
    List<Pair<Long, Long>> timeInterval = new ArrayList<Pair<Long, Long>>() {
      {
        add(Pair.of(1619626802000L, 1619626802000L));
        add(Pair.of(1619885951000L, 1619885951000L));
        add(Pair.of(1619885925000L, 1619885925000L));
        add(Pair.of(1619799469000L, 1619799469000L));
        add(Pair.of(1619885815000L, 1619885815000L));
        add(Pair.of(1619972127000L, 1619972127000L));

        add(Pair.of(1619799299000L, 1619799299000L));
        add(Pair.of(1619885632000L, 1619885632000L));
        add(Pair.of(1619799229000L, 1619799229000L));
        add(Pair.of(1619626420000L, 1619626420000L));

        add(Pair.of(1619540351000L, 1619540351000L));
        add(Pair.of(1619540351000L, 1619540351000L));
        add(Pair.of(1619281125000L, 1619281125000L));
        add(Pair.of(1619367469000L, 1619367469000L));

        add(Pair.of(1619194615000L, 1619194615000L));
        add(Pair.of(1619453727000L, 1619453727000L));
        add(Pair.of(1619453699000L, 1619453699000L));
        add(Pair.of(1619280832000L, 1619280832000L));
        add(Pair.of(1619280829000L, 1619280829000L));
        add(Pair.of(1619453620000L, 1619453620000L));
      }
    };

    HashMap<String, String> hashMap = new HashMap<>();
    hashMap.put("ServiceId1", "Service1");
    hashMap.put("ServiceId2", "Service2");
    hashMap.put("ServiceId3", "Service3");

    DashboardWorkloadDeployment dashboardWorkloadDeployment =
        cdOverviewDashboardServiceImpl.getWorkloadDeploymentInfoCalculation(workloadsId, status, timeInterval,
            Arrays.asList(
                "kuber1", "kuber2", "kuber1", "kuber3", "kuber3", "kuber1", "kuber4", "kuber2", "kuber2", "kuber1"),
            hashMap, startInterval, endInterval, workloadsId);

    List<WorkloadDeploymentInfo> workloadDeploymentInfos = new ArrayList<>();

    List<WorkloadDateCountInfo> service1WorkloadDateCount = new ArrayList<>();
    service1WorkloadDateCount.add(WorkloadDateCountInfo.builder()
                                      .date(1619568000000L)
                                      .execution(WorkloadCountInfo.builder().count(1).build())
                                      .build());
    service1WorkloadDateCount.add(WorkloadDateCountInfo.builder()
                                      .date(1619654400000L)
                                      .execution(WorkloadCountInfo.builder().count(0).build())
                                      .build());
    service1WorkloadDateCount.add(WorkloadDateCountInfo.builder()
                                      .date(1619740800000L)
                                      .execution(WorkloadCountInfo.builder().count(1).build())
                                      .build());
    service1WorkloadDateCount.add(WorkloadDateCountInfo.builder()
                                      .date(1619827200000L)
                                      .execution(WorkloadCountInfo.builder().count(2).build())
                                      .build());
    service1WorkloadDateCount.add(WorkloadDateCountInfo.builder()
                                      .date(1619913600000L)
                                      .execution(WorkloadCountInfo.builder().count(0).build())
                                      .build());

    List<WorkloadDateCountInfo> service2WorkloadDateCount = new ArrayList<>();
    service2WorkloadDateCount.add(WorkloadDateCountInfo.builder()
                                      .date(1619568000000L)
                                      .execution(WorkloadCountInfo.builder().count(1).build())
                                      .build());
    service2WorkloadDateCount.add(WorkloadDateCountInfo.builder()
                                      .date(1619654400000L)
                                      .execution(WorkloadCountInfo.builder().count(0).build())
                                      .build());
    service2WorkloadDateCount.add(WorkloadDateCountInfo.builder()
                                      .date(1619740800000L)
                                      .execution(WorkloadCountInfo.builder().count(0).build())
                                      .build());
    service2WorkloadDateCount.add(WorkloadDateCountInfo.builder()
                                      .date(1619827200000L)
                                      .execution(WorkloadCountInfo.builder().count(1).build())
                                      .build());
    service2WorkloadDateCount.add(WorkloadDateCountInfo.builder()
                                      .date(1619913600000L)
                                      .execution(WorkloadCountInfo.builder().count(0).build())
                                      .build());

    List<WorkloadDateCountInfo> service3WorkloadDateCount = new ArrayList<>();
    service3WorkloadDateCount.add(WorkloadDateCountInfo.builder()
                                      .date(1619568000000L)
                                      .execution(WorkloadCountInfo.builder().count(0).build())
                                      .build());
    service3WorkloadDateCount.add(WorkloadDateCountInfo.builder()
                                      .date(1619654400000L)
                                      .execution(WorkloadCountInfo.builder().count(0).build())
                                      .build());
    service3WorkloadDateCount.add(WorkloadDateCountInfo.builder()
                                      .date(1619740800000L)
                                      .execution(WorkloadCountInfo.builder().count(2).build())
                                      .build());
    service3WorkloadDateCount.add(WorkloadDateCountInfo.builder()
                                      .date(1619827200000L)
                                      .execution(WorkloadCountInfo.builder().count(1).build())
                                      .build());
    service3WorkloadDateCount.add(WorkloadDateCountInfo.builder()
                                      .date(1619913600000L)
                                      .execution(WorkloadCountInfo.builder().count(1).build())
                                      .build());

    workloadDeploymentInfos.add(WorkloadDeploymentInfo.builder()
                                    .serviceName("Service3")
                                    .serviceId("ServiceId3")
                                    .totalDeploymentChangeRate(400.00)
                                    .failureRate(50.0)
                                    .failureRateChangeRate(200.00)
                                    .frequency(0.0)
                                    .frequencyChangeRate(0.0)
                                    .lastExecuted(LastWorkloadInfo.builder()
                                                      .startTime(1619972127000L)
                                                      .endTime(1619972127000L)
                                                      .status(ExecutionStatus.FAILED.name())
                                                      .deploymentType("kuber1")
                                                      .build())
                                    .deploymentTypeList(deploymentTypeList)
                                    .rateSuccess(((-1) / (double) 3) * 100)
                                    .percentSuccess((2 / (double) 4) * 100)
                                    .totalDeployments(4)
                                    .lastPipelineExecutionId("ServiceId3")
                                    .workload(service3WorkloadDateCount)
                                    .build());

    workloadDeploymentInfos.add(WorkloadDeploymentInfo.builder()
                                    .serviceName("Service2")
                                    .serviceId("ServiceId2")
                                    .totalDeploymentChangeRate(200.00)
                                    .failureRate(0.0)
                                    .failureRateChangeRate(-100.00)
                                    .frequency(0.0)
                                    .frequencyChangeRate(0.0)
                                    .rateSuccess(0.0)
                                    .percentSuccess(0.0)
                                    .lastExecuted(LastWorkloadInfo.builder()
                                                      .startTime(1619885925000L)
                                                      .endTime(1619885925000L)
                                                      .status(ExecutionStatus.RUNNING.name())
                                                      .deploymentType("kuber1")
                                                      .build())
                                    .deploymentTypeList(deploymentTypeList)
                                    .lastPipelineExecutionId("ServiceId2")
                                    .totalDeployments(2)
                                    .workload(service2WorkloadDateCount)
                                    .build());

    workloadDeploymentInfos.add(WorkloadDeploymentInfo.builder()
                                    .serviceName("Service1")
                                    .serviceId("ServiceId1")
                                    .totalDeploymentChangeRate(400.00)
                                    .failureRate(50.0)
                                    .failureRateChangeRate(100)
                                    .frequencyChangeRate(0.00)
                                    .rateSuccess(0.0)
                                    .percentSuccess((2 / (double) 4) * 100)
                                    .lastExecuted(LastWorkloadInfo.builder()
                                                      .startTime(1619885951000L)
                                                      .endTime(1619885951000L)
                                                      .status(ExecutionStatus.EXPIRED.name())
                                                      .deploymentType("kuber2")
                                                      .build())
                                    .deploymentTypeList(deploymentTypeList)
                                    .lastPipelineExecutionId("ServiceId1")

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
  public void testGetDeploymentsExecutionInfo() throws Exception {
    long prevStartInterval = 1619136000000L;
    long prevEndInterval = 1619481600000L;
    long startInterval = 1619568000000L;
    long endInterval = 1619913600000L;

    List<ServiceDeployment> executionDeploymentList = new ArrayList<>();
    List<ServiceDeployment> prevExecutionDeploymentList = new ArrayList<>();
    prevExecutionDeploymentList.add(ServiceDeployment.builder()
                                        .time(1619136000000L)
                                        .deployments(DeploymentCount.builder().total(1).success(1).failure(0).build())
                                        .build());
    prevExecutionDeploymentList.add(ServiceDeployment.builder()
                                        .time(1619222400000L)
                                        .deployments(DeploymentCount.builder().total(4).success(3).failure(0).build())
                                        .build());
    prevExecutionDeploymentList.add(ServiceDeployment.builder()
                                        .time(1619308800000L)
                                        .deployments(DeploymentCount.builder().total(1).success(0).failure(1).build())
                                        .build());
    prevExecutionDeploymentList.add(ServiceDeployment.builder()
                                        .time(1619395200000L)
                                        .deployments(DeploymentCount.builder().total(3).success(1).failure(2).build())
                                        .build());
    prevExecutionDeploymentList.add(ServiceDeployment.builder()
                                        .time(1619481600000L)
                                        .deployments(DeploymentCount.builder().total(1).success(0).failure(1).build())
                                        .build());

    executionDeploymentList.add(ServiceDeployment.builder()
                                    .time(1619568000000L)
                                    .deployments(DeploymentCount.builder().total(2).success(1).failure(0).build())
                                    .build());
    executionDeploymentList.add(ServiceDeployment.builder()
                                    .time(1619654400000L)
                                    .deployments(DeploymentCount.builder().total(0).success(0).failure(0).build())
                                    .build());
    executionDeploymentList.add(ServiceDeployment.builder()
                                    .time(1619740800000L)
                                    .deployments(DeploymentCount.builder().total(3).success(1).failure(2).build())
                                    .build());
    executionDeploymentList.add(ServiceDeployment.builder()
                                    .time(1619827200000L)
                                    .deployments(DeploymentCount.builder().total(4).success(2).failure(1).build())
                                    .build());
    executionDeploymentList.add(ServiceDeployment.builder()
                                    .time(1619913600000L)
                                    .deployments(DeploymentCount.builder().total(1).success(0).failure(1).build())
                                    .build());

    ServiceDeploymentInfoDTO serviceDeploymentListWrap =
        ServiceDeploymentInfoDTO.builder().serviceDeploymentList(executionDeploymentList).build();
    ServiceDeploymentInfoDTO prevExecutionDeploymentWrap =
        ServiceDeploymentInfoDTO.builder().serviceDeploymentList(prevExecutionDeploymentList).build();

    doReturn(serviceDeploymentListWrap)
        .when(cdOverviewDashboardServiceImpl)
        .getServiceDeployments("acc", "org", "pro", startInterval, endInterval, null, 1);
    doReturn(prevExecutionDeploymentWrap)
        .when(cdOverviewDashboardServiceImpl)
        .getServiceDeployments("acc", "org", "pro", prevStartInterval, prevEndInterval, null, 1);

    ServiceDeploymentListInfo deploymentsExecutionInfo = cdOverviewDashboardServiceImpl.getServiceDeploymentsInfo(
        "acc", "org", "pro", startInterval, endInterval, null, 1);

    assertThat(deploymentsExecutionInfo.getServiceDeploymentList()).isEqualTo(executionDeploymentList);
    assertThat(deploymentsExecutionInfo.getTotalDeployments()).isEqualTo(10);
    assertThat(deploymentsExecutionInfo.getFrequency()).isEqualTo(2.0);
    assertThat(deploymentsExecutionInfo.getFailureRate()).isEqualTo(40.0);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGetDeploymentActiveFailedRunningInfo() {
    List<String> planExecutionIdListFailure = Arrays.asList("11", "12", "13", "14", "15", "16", "17", "18");
    List<String> namePipelineListFailure =
        Arrays.asList("name1", "name2", "name3", "name4", "name5", "name1", "name2", "name3");
    List<Long> startTsFailure = Arrays.asList(1619626802000L, 1619885951000L, 1619885925000L, 1619799469000L,
        1619885815000L, 1619972127000L, 1619799299000L, 1619885632000L);
    List<Long> endTsFailure = Arrays.asList(1622218802000L, 1622564351000L, 1622564325000L, 1622391469000L,
        1622564215000L, 1622650527000L, 1622391299000L, 1622564032000L);
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
    List<Long> startTsActive = Arrays.asList(1619626802000L, 1619885951000L, 1619885925000L, 1619799469000L,
        1619885815000L, 1619972127000L, 1619799299000L, 1619885632000L);
    List<Long> endTsActive = Arrays.asList(1622218802000L, 1622564351000L, 1622564325000L, 1622391469000L,
        1622564215000L, 1622650527000L, 1622391299000L, 1622564032000L);
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
    List<Long> startTsPending = Arrays.asList(1619626802000L, 1619885951000L, 1619885925000L, 1619799469000L,
        1619885815000L, 1619972127000L, 1619799299000L, 1619885632000L);
    List<Long> endTsPending = Arrays.asList(1622218802000L, 1622564351000L, 1622564325000L, 1622391469000L,
        1622564215000L, 1622650527000L, 1622391299000L, 1622564032000L);
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
    String queryIdFailed = cdOverviewDashboardServiceImpl.queryBuilderSelectIdLimitTimeCdTable(
        "acc", "orgId", "pro", 10, failedStatusList);

    String queryActive = cdOverviewDashboardServiceImpl.queryBuilderStatus("acc", "orgId", "pro", 10, activeStatusList);
    String queryIdActive = cdOverviewDashboardServiceImpl.queryBuilderSelectIdLimitTimeCdTable(
        "acc", "orgId", "pro", 10, activeStatusList);

    String queryPending =
        cdOverviewDashboardServiceImpl.queryBuilderStatus("acc", "orgId", "pro", 10, pendingStatusList);
    String queryIdPending = cdOverviewDashboardServiceImpl.queryBuilderSelectIdLimitTimeCdTable(
        "acc", "orgId", "pro", 10, pendingStatusList);

    // failure
    doReturn(deploymentStatusInfoListFailure)
        .when(cdOverviewDashboardServiceImpl)
        .queryCalculatorDeploymentInfo(queryFailed);

    String serviveTagQueryFailure = cdOverviewDashboardServiceImpl.queryBuilderServiceTag(queryIdFailed);

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

    String serviveTagQueryActive = cdOverviewDashboardServiceImpl.queryBuilderServiceTag(queryIdActive);

    HashMap<String, List<ServiceDeploymentInfo>> serviceTagMapActive = new HashMap<>();

    doReturn(serviceTagMapActive)
        .when(cdOverviewDashboardServiceImpl)
        .queryCalculatorServiceTagMag(serviveTagQueryActive);

    // Pending
    doReturn(deploymentStatusInfoListPending)
        .when(cdOverviewDashboardServiceImpl)
        .queryCalculatorDeploymentInfo(queryPending);

    String serviveTagQueryPending = cdOverviewDashboardServiceImpl.queryBuilderServiceTag(queryIdPending);

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
                              .startTs(1619626802000L)
                              .endTs(1622218802000L)
                              .status(failedStatusList.get(0))
                              .serviceInfoList(serviceTagMapFailure.get("11"))
                              .build());
    failureStatusInfo.add(DeploymentStatusInfo.builder()
                              .name("name2")
                              .startTs(1619885951000L)
                              .endTs(1622564351000L)
                              .status(failedStatusList.get(1))
                              .build());
    failureStatusInfo.add(DeploymentStatusInfo.builder()
                              .name("name3")
                              .startTs(1619885925000L)
                              .endTs(1622564325000L)
                              .status(failedStatusList.get(0))
                              .serviceInfoList(serviceTagMapFailure.get("13"))
                              .build());
    failureStatusInfo.add(DeploymentStatusInfo.builder()
                              .name("name4")
                              .startTs(1619799469000l)
                              .endTs(1622391469000L)
                              .status(failedStatusList.get(1))
                              .build());
    failureStatusInfo.add(DeploymentStatusInfo.builder()
                              .name("name5")
                              .startTs(1619885815000L)
                              .endTs(1622564215000L)
                              .status(failedStatusList.get(0))
                              .serviceInfoList(serviceTagMapFailure.get("15"))
                              .build());
    failureStatusInfo.add(DeploymentStatusInfo.builder()
                              .name("name1")
                              .startTs(1619972127000L)
                              .endTs(1622650527000L)
                              .status(failedStatusList.get(2))
                              .build());
    failureStatusInfo.add(DeploymentStatusInfo.builder()
                              .name("name2")
                              .startTs(1619799299000L)
                              .endTs(1622391299000L)
                              .status(failedStatusList.get(2))
                              .build());
    failureStatusInfo.add(DeploymentStatusInfo.builder()
                              .name("name3")
                              .startTs(1619885632000L)
                              .endTs(1622564032000L)
                              .status(failedStatusList.get(0))
                              .build());

    // active
    List<DeploymentStatusInfo> activeStatusInfo = new ArrayList<>();
    activeStatusInfo.add(DeploymentStatusInfo.builder()
                             .name("name1")
                             .startTs(1619626802000L)
                             .endTs(1622218802000L)
                             .status(activeStatusList.get(0))
                             .build());
    activeStatusInfo.add(DeploymentStatusInfo.builder()
                             .name("name2")
                             .startTs(1619885951000L)
                             .endTs(1622564351000L)
                             .status(activeStatusList.get(1))
                             .build());
    activeStatusInfo.add(DeploymentStatusInfo.builder()
                             .name("name3")
                             .startTs(1619885925000L)
                             .endTs(1622564325000L)
                             .status(activeStatusList.get(0))
                             .build());
    activeStatusInfo.add(DeploymentStatusInfo.builder()
                             .name("name4")
                             .startTs(1619799469000L)
                             .endTs(1622391469000L)
                             .status(activeStatusList.get(1))
                             .build());
    activeStatusInfo.add(DeploymentStatusInfo.builder()
                             .name("name5")
                             .startTs(1619885815000L)
                             .endTs(1622564215000L)
                             .status(activeStatusList.get(0))
                             .build());
    activeStatusInfo.add(DeploymentStatusInfo.builder()
                             .name("name1")
                             .startTs(1619972127000L)
                             .endTs(1622650527000L)
                             .status(activeStatusList.get(0))
                             .build());
    activeStatusInfo.add(DeploymentStatusInfo.builder()
                             .name("name2")
                             .startTs(1619799299000L)
                             .endTs(1622391299000L)
                             .status(activeStatusList.get(1))
                             .build());
    activeStatusInfo.add(DeploymentStatusInfo.builder()
                             .name("name3")
                             .startTs(1619885632000L)
                             .endTs(1622564032000L)
                             .status(activeStatusList.get(0))
                             .build());

    // pending
    List<DeploymentStatusInfo> pendingStatusInfo = new ArrayList<>();
    pendingStatusInfo.add(DeploymentStatusInfo.builder()
                              .name("name1")
                              .startTs(1619626802000L)
                              .endTs(1622218802000L)
                              .status(pendingStatusList.get(0))
                              .build());
    pendingStatusInfo.add(DeploymentStatusInfo.builder()
                              .name("name2")
                              .startTs(1619885951000L)
                              .endTs(1622564351000L)
                              .status(pendingStatusList.get(1))
                              .build());
    pendingStatusInfo.add(DeploymentStatusInfo.builder()
                              .name("name3")
                              .startTs(1619885925000L)
                              .endTs(1622564325000L)
                              .status(pendingStatusList.get(0))
                              .build());
    pendingStatusInfo.add(DeploymentStatusInfo.builder()
                              .name("name4")
                              .startTs(1619799469000L)
                              .endTs(1622391469000L)
                              .status(pendingStatusList.get(1))
                              .build());
    pendingStatusInfo.add(DeploymentStatusInfo.builder()
                              .name("name5")
                              .startTs(1619885815000L)
                              .endTs(1622564215000L)
                              .status(pendingStatusList.get(0))
                              .build());
    pendingStatusInfo.add(DeploymentStatusInfo.builder()
                              .name("name1")
                              .startTs(1619972127000L)
                              .endTs(1622650527000L)
                              .status(pendingStatusList.get(0))
                              .build());
    pendingStatusInfo.add(DeploymentStatusInfo.builder()
                              .name("name2")
                              .startTs(1619799299000L)
                              .endTs(1622391299000L)
                              .status(pendingStatusList.get(1))
                              .build());
    pendingStatusInfo.add(DeploymentStatusInfo.builder()
                              .name("name3")
                              .startTs(1619885632000L)
                              .endTs(1622564032000L)
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
