package software.wings.service;

import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.beans.WorkflowType.ORCHESTRATION;
import static io.harness.beans.WorkflowType.PIPELINE;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.when;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.api.ServiceElement.Builder.aServiceElement;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.ElementExecutionSummary.ElementExecutionSummaryBuilder.anElementExecutionSummary;
import static software.wings.beans.Environment.EnvironmentType.NON_PROD;
import static software.wings.beans.Environment.EnvironmentType.PROD;
import static software.wings.beans.PipelineExecution.Builder.aPipelineExecution;
import static software.wings.sm.InstanceStatusSummary.InstanceStatusSummaryBuilder.anInstanceStatusSummary;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.PageRequest;
import io.harness.category.element.UnitTests;
import io.harness.persistence.HIterator;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.PipelineStageExecution;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.stats.DeploymentStatistics;
import software.wings.beans.stats.DeploymentStatistics.AggregatedDayStats;
import software.wings.beans.stats.DeploymentStatistics.AggregatedDayStats.DayStat;
import software.wings.beans.stats.ServiceInstanceStatistics;
import software.wings.beans.stats.TopConsumer;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.StatisticsService;
import software.wings.service.intfc.WorkflowExecutionService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class StatisticsServiceTest extends WingsBaseTest {
  @Mock private AppService appService;
  @Mock private WorkflowExecutionService workflowExecutionService;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS) private WingsPersistence wingsPersistence;

  @Inject @InjectMocks private StatisticsService statisticsService;

  @Mock private HIterator<WorkflowExecution> executionIterator;

  @Before
  public void setUp() throws Exception {
    when(appService.getAppIdsByAccountId(ACCOUNT_ID)).thenReturn(asList(APP_ID));
    when(appService.getAppsByAccountId(ACCOUNT_ID)).thenReturn(asList(anApplication().uuid(APP_ID).build()));
    when(workflowExecutionService.obtainWorkflowExecutionIterator(anyList(), anyLong())).thenReturn(executionIterator);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetServiceInstanceStatistics() {
    when(appService.list(any(PageRequest.class)))
        .thenReturn(aPageResponse().withResponse(asList(anApplication().uuid(APP_ID).name(APP_NAME).build())).build());

    List<ElementExecutionSummary> serviceExecutionSummaries =
        asList(anElementExecutionSummary()
                   .withStatus(SUCCESS)
                   .withContextElement(aServiceElement().withName(SERVICE_NAME).withUuid(SERVICE_ID).build())
                   .build());
    List<ElementExecutionSummary> serviceFailureExecutionSummaries =
        asList(anElementExecutionSummary()
                   .withStatus(FAILED)
                   .withContextElement(aServiceElement().withName(SERVICE_NAME).withUuid(SERVICE_ID).build())
                   .build());

    List<WorkflowExecution> executions =
        constructWorkflowExecutions(serviceExecutionSummaries, serviceFailureExecutionSummaries);

    when(workflowExecutionService.obtainWorkflowExecutions(anyList(), anyLong())).thenReturn(executions);

    ServiceInstanceStatistics statistics = statisticsService.getServiceInstanceStatistics(ACCOUNT_ID, null, 30);
    assertThat(statistics.getStatsMap()).isNotEmpty();
    assertThat(statistics.getStatsMap().get(PROD))
        .hasSize(1)
        .containsExactlyInAnyOrder(TopConsumer.builder()
                                       .appId(APP_ID)
                                       .appName(APP_NAME)
                                       .serviceId(SERVICE_ID)
                                       .serviceName(SERVICE_NAME)
                                       .successfulActivityCount(2)
                                       .failedActivityCount(0)
                                       .totalCount(2)
                                       .build());

    assertThat(statistics.getStatsMap().get(NON_PROD)).hasSize(1);
  }

  @Test
  @Category(UnitTests.class)
  public void setWingsPersistence() {
    when(appService.list(any(PageRequest.class)))
        .thenReturn(aPageResponse().withResponse(asList(anApplication().uuid(APP_ID).name(APP_NAME).build())).build());

    List<ElementExecutionSummary> serviceExecutionSummaries =
        asList(anElementExecutionSummary()
                   .withStatus(SUCCESS)
                   .withContextElement(aServiceElement().withName(SERVICE_NAME).withUuid(SERVICE_ID).build())
                   .build());
    List<ElementExecutionSummary> serviceFailureExecutionSummaries =
        asList(anElementExecutionSummary()
                   .withStatus(FAILED)
                   .withContextElement(aServiceElement().withName(SERVICE_NAME).withUuid(SERVICE_ID).build())
                   .build());

    List<WorkflowExecution> executions =
        constructPipelineExecutions(serviceExecutionSummaries, serviceFailureExecutionSummaries);

    when(workflowExecutionService.obtainWorkflowExecutions(anyList(), anyLong())).thenReturn(executions);

    ServiceInstanceStatistics statistics = statisticsService.getServiceInstanceStatistics(ACCOUNT_ID, null, 30);
    assertThat(statistics.getStatsMap()).isNotEmpty();
    assertThat(statistics.getStatsMap().get(PROD))
        .hasSize(1)
        .containsExactlyInAnyOrder(TopConsumer.builder()
                                       .appId(APP_ID)
                                       .appName(APP_NAME)
                                       .serviceId(SERVICE_ID)
                                       .serviceName(SERVICE_NAME)
                                       .successfulActivityCount(2)
                                       .failedActivityCount(0)
                                       .totalCount(2)
                                       .build());

    assertThat(statistics.getStatsMap().get(NON_PROD)).hasSize(1);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetDeploymentStatistics() {
    List<ElementExecutionSummary> serviceExecutionSummaries =
        asList(anElementExecutionSummary()
                   .withInstanceStatusSummaries(
                       asList(anInstanceStatusSummary()
                                  .withInstanceElement(anInstanceElement().uuid(generateUuid()).build())
                                  .build()))
                   .build());

    List<WorkflowExecution> executions = constructWorkflowServiceExecutions(serviceExecutionSummaries);

    when(workflowExecutionService.obtainWorkflowExecutions(anyList(), anyLong())).thenReturn(executions);

    DeploymentStatistics deploymentStatistics =
        statisticsService.getDeploymentStatistics(ACCOUNT_ID, asList(APP_ID), 30);

    assertThat(deploymentStatistics.getStatsMap()).hasSize(3).containsOnlyKeys(EnvironmentType.values());

    AggregatedDayStats aggregatedProdDayStats = deploymentStatistics.getStatsMap().get(PROD);
    AggregatedDayStats aggregatedNonProdDayStats = deploymentStatistics.getStatsMap().get(NON_PROD);

    assertAggregatedDatyStats(getStartEpoch(), aggregatedProdDayStats, aggregatedNonProdDayStats);
  }

  private long getStartEpoch() {
    return getEndEpoch(29);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetPipelineDeploymentStatistics() {
    long startEpoch = getStartEpoch();

    List<ElementExecutionSummary> serviceExecutionSummaries =
        asList(anElementExecutionSummary()
                   .withInstanceStatusSummaries(
                       asList(anInstanceStatusSummary()
                                  .withInstanceElement(anInstanceElement().uuid(generateUuid()).build())
                                  .build()))
                   .build());

    List<WorkflowExecution> executions = constructPipelineExecutions(serviceExecutionSummaries);

    when(workflowExecutionService.obtainWorkflowExecutions(anyList(), anyLong())).thenReturn(executions);

    DeploymentStatistics deploymentStatistics =
        statisticsService.getDeploymentStatistics(ACCOUNT_ID, asList(APP_ID), 30);

    assertThat(deploymentStatistics.getStatsMap()).hasSize(3).containsOnlyKeys(EnvironmentType.values());

    AggregatedDayStats aggregatedProdDayStats = deploymentStatistics.getStatsMap().get(PROD);
    AggregatedDayStats aggregatedNonProdDayStats = deploymentStatistics.getStatsMap().get(NON_PROD);

    assertAggregatedDatyStats(startEpoch, aggregatedProdDayStats, aggregatedNonProdDayStats);
  }

  private void assertAggregatedDatyStats(
      long startEpoch, AggregatedDayStats aggregatedProdDayStats, AggregatedDayStats aggregatedNonProdDayStats) {
    assertThat(aggregatedNonProdDayStats.getFailedCount()).isEqualTo(2);
    assertThat(aggregatedNonProdDayStats.getInstancesCount()).isEqualTo(2);
    assertThat(aggregatedNonProdDayStats.getTotalCount()).isEqualTo(2);
    assertThat(aggregatedNonProdDayStats.getDaysStats().size()).isEqualTo(30);
    assertThat(aggregatedNonProdDayStats.getDaysStats().get(0)).isEqualTo(new DayStat(2, 2, 2, startEpoch));

    assertThat(aggregatedProdDayStats.getFailedCount()).isEqualTo(0);
    assertThat(aggregatedProdDayStats.getInstancesCount()).isEqualTo(2);
    assertThat(aggregatedProdDayStats.getTotalCount()).isEqualTo(2);
    assertThat(aggregatedProdDayStats.getDaysStats().size()).isEqualTo(30);
    assertThat(aggregatedProdDayStats.getDaysStats().get(0)).isEqualTo(new DayStat(2, 0, 2, startEpoch));
  }

  private List<WorkflowExecution> constructWorkflowServiceExecutions(
      List<ElementExecutionSummary> serviceExecutionSummaries) {
    long startEpoch = getStartEpoch();
    return asList(aSuccessfulServiceWfExecution(serviceExecutionSummaries, startEpoch),
        aSuccessfulServiceWfExecution(serviceExecutionSummaries, startEpoch),
        aFailedServiceWfExecution(serviceExecutionSummaries, startEpoch),
        aFailedServiceWfExecution(serviceExecutionSummaries, startEpoch));
  }

  private WorkflowExecution aFailedServiceWfExecution(
      List<ElementExecutionSummary> serviceExecutionSummaries, long startEpoch) {
    return WorkflowExecution.builder()
        .appId(APP_ID)
        .envType(NON_PROD)
        .status(ExecutionStatus.FAILED)
        .workflowType(ORCHESTRATION)
        .serviceExecutionSummaries(serviceExecutionSummaries)
        .createdAt(startEpoch)
        .build();
  }

  private WorkflowExecution aSuccessfulServiceWfExecution(
      List<ElementExecutionSummary> serviceExecutionSummaries, long startEpoch) {
    return WorkflowExecution.builder()
        .appId(APP_ID)
        .envType(PROD)
        .status(SUCCESS)
        .workflowType(ORCHESTRATION)
        .serviceExecutionSummaries(serviceExecutionSummaries)
        .createdAt(startEpoch)
        .build();
  }

  private List<WorkflowExecution> constructWorkflowExecutions(List<ElementExecutionSummary> serviceExecutionSummaries,
      List<ElementExecutionSummary> serviceFailureExecutionSummaries) {
    long endEpoch = getEndEpoch(0);
    long startEpoch = getStartEpoch();
    return asList(aSuccessWorkflowExecution(serviceExecutionSummaries, endEpoch),
        aSuccessWorkflowExecution(serviceExecutionSummaries, endEpoch),
        aFailedWorkflowExecution(serviceFailureExecutionSummaries, startEpoch),
        aFailedWorkflowExecution(serviceFailureExecutionSummaries, startEpoch));
  }

  private WorkflowExecution aFailedWorkflowExecution(
      List<ElementExecutionSummary> serviceFailureExecutionSummaries, long startEpoch) {
    return WorkflowExecution.builder()
        .appId(APP_ID)
        .appName(APP_NAME)
        .envType(NON_PROD)
        .status(ExecutionStatus.FAILED)
        .serviceExecutionSummaries(serviceFailureExecutionSummaries)
        .createdAt(startEpoch)
        .build();
  }

  private WorkflowExecution aSuccessWorkflowExecution(
      List<ElementExecutionSummary> serviceExecutionSummaries, long endEpoch) {
    return WorkflowExecution.builder()
        .appId(APP_ID)
        .appName(APP_NAME)
        .envType(PROD)
        .status(SUCCESS)
        .serviceExecutionSummaries(serviceExecutionSummaries)
        .createdAt(endEpoch)
        .build();
  }

  private List<WorkflowExecution> constructPipelineExecutions(List<ElementExecutionSummary> serviceExecutionSummaries) {
    return asList(aPipelineServiceWfExecution(PROD, SUCCESS, serviceExecutionSummaries),

        aPipelineServiceWfExecution(PROD, SUCCESS, serviceExecutionSummaries),
        aPipelineServiceWfExecution(NON_PROD, FAILED, serviceExecutionSummaries),
        aPipelineServiceWfExecution(NON_PROD, FAILED, serviceExecutionSummaries));
  }

  private List<WorkflowExecution> constructPipelineExecutions(List<ElementExecutionSummary> serviceExecutionSummaries,
      List<ElementExecutionSummary> failureExecutionSummaries) {
    return asList(aPipelineServiceWfExecution(PROD, SUCCESS, serviceExecutionSummaries),

        aPipelineServiceWfExecution(PROD, SUCCESS, serviceExecutionSummaries),
        aPipelineServiceWfExecution(NON_PROD, FAILED, failureExecutionSummaries),
        aPipelineServiceWfExecution(NON_PROD, FAILED, failureExecutionSummaries));
  }

  private WorkflowExecution aPipelineServiceWfExecution(EnvironmentType environmentType,
      ExecutionStatus executionStatus, List<ElementExecutionSummary> serviceExecutionSummaries) {
    return WorkflowExecution.builder()
        .appId(APP_ID)
        .appName(APP_NAME)
        .envType(environmentType)
        .status(executionStatus)
        .workflowType(PIPELINE)
        .pipelineExecution(
            aPipelineExecution()
                .withPipelineStageExecutions(
                    asList(PipelineStageExecution.builder()
                               .workflowExecutions(asList(WorkflowExecution.builder()
                                                              .appId(APP_ID)
                                                              .envType(NON_PROD)
                                                              .appName(APP_NAME)
                                                              .status(executionStatus)
                                                              .workflowType(PIPELINE)
                                                              .serviceExecutionSummaries(serviceExecutionSummaries)
                                                              .createdAt(getStartEpoch())
                                                              .build()))
                               .build()))
                .build())
        .createdAt(getStartEpoch())
        .build();
  }

  private long getEndEpoch(int i) {
    return LocalDate.now(ZoneId.of("America/Los_Angeles"))
        .minus(i, ChronoUnit.DAYS)
        .atStartOfDay(ZoneId.of("America/Los_Angeles"))
        .toInstant()
        .toEpochMilli();
  }
}
