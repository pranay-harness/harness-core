package software.wings.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.api.ServiceElement.Builder.aServiceElement;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.ElementExecutionSummary.ElementExecutionSummaryBuilder.anElementExecutionSummary;
import static software.wings.beans.Environment.EnvironmentType.NON_PROD;
import static software.wings.beans.Environment.EnvironmentType.PROD;
import static software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder.aWorkflowExecution;
import static software.wings.beans.WorkflowType.ORCHESTRATION;
import static software.wings.dl.PageResponse.PageResponseBuilder.aPageResponse;
import static software.wings.sm.ExecutionStatus.FAILED;
import static software.wings.sm.ExecutionStatus.SUCCESS;
import static software.wings.sm.InstanceStatusSummary.InstanceStatusSummaryBuilder.anInstanceStatusSummary;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;

import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.aggregation.AggregationPipeline;
import org.mongodb.morphia.aggregation.Group;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.stats.DeploymentStatistics;
import software.wings.beans.stats.DeploymentStatistics.AggregatedDayStats;
import software.wings.beans.stats.DeploymentStatistics.AggregatedDayStats.DayStat;
import software.wings.beans.stats.ServiceInstanceStatistics;
import software.wings.beans.stats.TopConsumer;
import software.wings.dl.HIterator;
import software.wings.dl.PageRequest;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.StatisticsService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ExecutionStatus;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class StatisticsServiceTest extends WingsBaseTest {
  @Mock private AppService appService;
  @Mock private WorkflowExecutionService workflowExecutionService;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS) private WingsPersistence wingsPersistence;

  @Inject @InjectMocks private StatisticsService statisticsService;

  @Mock private AggregationPipeline aggregationPipeline;
  @Mock private HIterator<WorkflowExecution> executionIterator;

  @Before
  public void setUp() throws Exception {
    when(appService.getAppIdsByAccountId(ACCOUNT_ID)).thenReturn(asList(APP_ID));
    when(appService.getAppsByAccountId(ACCOUNT_ID)).thenReturn(asList(anApplication().withUuid(APP_ID).build()));
    when(wingsPersistence.getDatastore().createAggregation(WorkflowExecution.class)).thenReturn(aggregationPipeline);
    when(aggregationPipeline.match(any(Query.class))).thenReturn(aggregationPipeline);
    when(aggregationPipeline.group(anyList(), any(Group.class))).thenReturn(aggregationPipeline);
    when(aggregationPipeline.group(anyString(), any(Group.class))).thenReturn(aggregationPipeline);
    when(workflowExecutionService.obtainWorkflowExecutionIterator(anyList(), anyLong())).thenReturn(executionIterator);
  }

  @Test
  public void shouldGetServiceInstanceStatistics() {
    long endEpoch = LocalDate.now(ZoneId.of("America/Los_Angeles"))
                        .minus(0, ChronoUnit.DAYS)
                        .atStartOfDay(ZoneId.of("America/Los_Angeles"))
                        .toInstant()
                        .toEpochMilli();
    long startEpoch = LocalDate.now(ZoneId.of("America/Los_Angeles"))
                          .minus(29, ChronoUnit.DAYS)
                          .atStartOfDay(ZoneId.of("America/Los_Angeles"))
                          .toInstant()
                          .toEpochMilli();

    when(appService.list(any(PageRequest.class)))
        .thenReturn(
            aPageResponse().withResponse(asList(anApplication().withUuid(APP_ID).withName(APP_NAME).build())).build());

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

    List<WorkflowExecution> executions = asList(aWorkflowExecution()
                                                    .withAppId(APP_ID)
                                                    .withAppName(APP_NAME)
                                                    .withEnvType(PROD)
                                                    .withStatus(SUCCESS)
                                                    .withServiceExecutionSummaries(serviceExecutionSummaries)
                                                    .withCreatedAt(endEpoch)
                                                    .build(),
        aWorkflowExecution()
            .withAppId(APP_ID)
            .withAppName(APP_NAME)
            .withEnvType(PROD)
            .withStatus(SUCCESS)
            .withServiceExecutionSummaries(serviceExecutionSummaries)
            .withCreatedAt(endEpoch)
            .build(),
        aWorkflowExecution()
            .withAppId(APP_ID)
            .withAppName(APP_NAME)
            .withEnvType(NON_PROD)
            .withStatus(ExecutionStatus.FAILED)
            .withServiceExecutionSummaries(serviceFailureExecutionSummaries)
            .withCreatedAt(startEpoch)
            .build(),
        aWorkflowExecution()
            .withAppId(APP_ID)
            .withAppName(APP_NAME)
            .withEnvType(NON_PROD)
            .withStatus(ExecutionStatus.FAILED)
            .withServiceExecutionSummaries(serviceFailureExecutionSummaries)
            .withCreatedAt(startEpoch)
            .build());

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
  public void shouldGetDeploymentStatistics() {
    long startEpoch = LocalDate.now(ZoneId.of("America/Los_Angeles"))
                          .minus(29, ChronoUnit.DAYS)
                          .atStartOfDay(ZoneId.of("America/Los_Angeles"))
                          .toInstant()
                          .toEpochMilli();

    List<ElementExecutionSummary> serviceExecutionSummaries =
        asList(anElementExecutionSummary()
                   .withInstanceStatusSummaries(
                       asList(anInstanceStatusSummary()
                                  .withInstanceElement(anInstanceElement().withUuid(generateUuid()).build())
                                  .build()))
                   .build());

    List<WorkflowExecution> executions = asList(aWorkflowExecution()
                                                    .withAppId(APP_ID)
                                                    .withEnvType(PROD)
                                                    .withStatus(SUCCESS)
                                                    .withWorkflowType(ORCHESTRATION)
                                                    .withServiceExecutionSummaries(serviceExecutionSummaries)
                                                    .withCreatedAt(startEpoch)
                                                    .build(),
        aWorkflowExecution()
            .withAppId(APP_ID)
            .withEnvType(PROD)
            .withStatus(SUCCESS)
            .withWorkflowType(ORCHESTRATION)
            .withServiceExecutionSummaries(serviceExecutionSummaries)
            .withCreatedAt(startEpoch)
            .build(),
        aWorkflowExecution()
            .withAppId(APP_ID)
            .withEnvType(NON_PROD)
            .withStatus(ExecutionStatus.FAILED)
            .withWorkflowType(ORCHESTRATION)
            .withServiceExecutionSummaries(serviceExecutionSummaries)
            .withCreatedAt(startEpoch)
            .build(),
        aWorkflowExecution()
            .withAppId(APP_ID)
            .withEnvType(NON_PROD)
            .withStatus(ExecutionStatus.FAILED)
            .withWorkflowType(ORCHESTRATION)
            .withServiceExecutionSummaries(serviceExecutionSummaries)
            .withCreatedAt(startEpoch)
            .build());

    when(workflowExecutionService.obtainWorkflowExecutions(anyList(), anyLong())).thenReturn(executions);

    DeploymentStatistics deploymentStatistics =
        statisticsService.getDeploymentStatistics(ACCOUNT_ID, asList(APP_ID), 30);

    assertThat(deploymentStatistics.getStatsMap()).hasSize(3).containsOnlyKeys(EnvironmentType.values());

    AggregatedDayStats aggregatedProdDayStats = deploymentStatistics.getStatsMap().get(PROD);
    AggregatedDayStats aggregatedNonProdDayStats = deploymentStatistics.getStatsMap().get(NON_PROD);

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
}
