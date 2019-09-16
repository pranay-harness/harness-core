package software.wings.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.PageResponse.PageResponseBuilder;
import io.harness.category.element.UnitTests;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.WingsBaseTest;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.User;
import software.wings.common.VerificationConstants;
import software.wings.dl.WingsPersistence;
import software.wings.security.AppPermissionSummary;
import software.wings.security.AppPermissionSummary.EnvInfo;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.UserPermissionInfo;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData;
import software.wings.service.impl.analysis.ContinuousVerificationServiceImpl;
import software.wings.service.impl.analysis.DataCollectionInfoV2;
import software.wings.service.impl.apm.APMMetricInfo;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.service.intfc.verification.CVTaskService;
import software.wings.service.intfc.verification.DataCollectionInfoService;
import software.wings.sm.StateType;
import software.wings.sm.states.DatadogState.Metric;
import software.wings.verification.CVConfiguration;
import software.wings.verification.CVTask;
import software.wings.verification.HeatMapResolution;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by Praveen on 5/31/2018
 */
public class ContinuousVerificationServiceImplTest extends WingsBaseTest {
  private String accountId;
  private String appId;
  private String stateExecutionId;
  private String workflowId;
  private String workflowExecutionId;
  private String serviceId;
  private String envId;
  private User user;

  @Mock private AuthService mockAuthService;
  @Mock private WingsPersistence mockWingsPersistence;
  @Mock private UserPermissionInfo mockUserPermissionInfo;
  @Mock private CVConfigurationService cvConfigurationService;
  @Mock private DataCollectionInfoService dataCollectionInfoService;
  @Mock private CVTaskService cvTaskService;
  @InjectMocks private ContinuousVerificationServiceImpl continuousVerificationService;

  @Before
  public void setupMocks() throws IllegalAccessException {
    accountId = UUID.randomUUID().toString();
    appId = UUID.randomUUID().toString();
    stateExecutionId = UUID.randomUUID().toString();
    workflowId = UUID.randomUUID().toString();
    workflowExecutionId = UUID.randomUUID().toString();
    serviceId = UUID.randomUUID().toString();
    envId = UUID.randomUUID().toString();
    user = new User();

    MockitoAnnotations.initMocks(this);

    PageResponse<ContinuousVerificationExecutionMetaData> r =
        PageResponseBuilder.aPageResponse().withResponse(Arrays.asList(getExecutionMetadata())).build();
    PageResponse<ContinuousVerificationExecutionMetaData> rEmpty = PageResponseBuilder.aPageResponse().build();
    when(mockWingsPersistence.query(any(), any(PageRequest.class))).thenReturn(r).thenReturn(rEmpty);

    when(mockUserPermissionInfo.getAppPermissionMapInternal()).thenReturn(new HashMap<String, AppPermissionSummary>() {
      { put(appId, buildAppPermissionSummary()); }
    });

    when(mockAuthService.getUserPermissionInfo(accountId, user, false)).thenReturn(mockUserPermissionInfo);
    FieldUtils.writeField(continuousVerificationService, "cvConfigurationService", cvConfigurationService, true);
    FieldUtils.writeField(continuousVerificationService, "dataCollectionInfoService", dataCollectionInfoService, true);
    FieldUtils.writeField(continuousVerificationService, "cvTaskService", cvTaskService, true);
  }

  private ContinuousVerificationExecutionMetaData getExecutionMetadata() {
    return ContinuousVerificationExecutionMetaData.builder()
        .accountId(accountId)
        .applicationId(appId)
        .appName("dummy")
        .artifactName("cv dummy artifact")
        .envName("cv dummy env")
        .envId(envId)
        .phaseName("dummy phase")
        .pipelineName("dummy pipeline")
        .workflowName("dummy workflow")
        .pipelineStartTs(1519200000000L)
        .workflowStartTs(1519200000000L)
        .serviceId(serviceId)
        .serviceName("dummy service")
        .stateType(StateType.APM_VERIFICATION)
        .workflowId(workflowId)
        .workflowExecutionId(workflowExecutionId)
        .build();
  }

  private AppPermissionSummary buildAppPermissionSummary() {
    Map<Action, Set<String>> servicePermissions = new HashMap<Action, Set<String>>() {
      { put(Action.READ, Sets.newHashSet(serviceId)); }
    };
    Map<Action, Set<EnvInfo>> envPermissions = new HashMap<Action, Set<EnvInfo>>() {
      {
        put(Action.READ, Sets.newHashSet(EnvInfo.builder().envId(envId).envType(EnvironmentType.PROD.name()).build()));
      }
    };
    Map<Action, Set<String>> pipelinePermissions = new HashMap<Action, Set<String>>() {
      { put(Action.READ, Sets.newHashSet()); }
    };
    Map<Action, Set<String>> workflowPermissions = new HashMap<Action, Set<String>>() {
      { put(Action.READ, Sets.newHashSet(workflowId)); }
    };

    return AppPermissionSummary.builder()
        .servicePermissions(servicePermissions)
        .envPermissions(envPermissions)
        .workflowPermissions(workflowPermissions)
        .pipelinePermissions(pipelinePermissions)
        .build();
  }
  @Test
  @Category(UnitTests.class)
  public void testNullUser() throws ParseException, IllegalAccessException {
    setupMocks();
    LinkedHashMap<Long,
        LinkedHashMap<String,
            LinkedHashMap<String,
                LinkedHashMap<String, LinkedHashMap<String, List<ContinuousVerificationExecutionMetaData>>>>>>
        execData =
            continuousVerificationService.getCVExecutionMetaData(accountId, 1519200000000L, 1519200000001L, null);

    assertThat(execData).isNotNull();
    assertThat(execData).hasSize(0);
  }

  @Test
  @Category(UnitTests.class)
  public void testAllValidPermissions() throws ParseException {
    LinkedHashMap<Long,
        LinkedHashMap<String,
            LinkedHashMap<String,
                LinkedHashMap<String, LinkedHashMap<String, List<ContinuousVerificationExecutionMetaData>>>>>>
        execData =
            continuousVerificationService.getCVExecutionMetaData(accountId, 1519200000000L, 1519200000001L, user);

    assertThat(execData).isNotNull();
    assertThat(execData).hasSize(1);
  }

  @Test
  @Category(UnitTests.class)
  public void testNoPermissionsForEnvironment() throws ParseException {
    AppPermissionSummary permissionSummary = buildAppPermissionSummary();
    permissionSummary.setEnvPermissions(new HashMap<>());
    when(mockUserPermissionInfo.getAppPermissionMapInternal()).thenReturn(new HashMap<String, AppPermissionSummary>() {
      { put(appId, permissionSummary); }
    });

    LinkedHashMap<Long,
        LinkedHashMap<String,
            LinkedHashMap<String,
                LinkedHashMap<String, LinkedHashMap<String, List<ContinuousVerificationExecutionMetaData>>>>>>
        execData =
            continuousVerificationService.getCVExecutionMetaData(accountId, 1519200000000L, 1519200000001L, user);

    assertThat(execData).isNotNull();
    assertThat(execData).isEmpty();
  }

  @Test
  @Category(UnitTests.class)
  public void testNoPermissionsForService() throws ParseException {
    AppPermissionSummary permissionSummary = buildAppPermissionSummary();
    permissionSummary.setServicePermissions(new HashMap<>());
    when(mockUserPermissionInfo.getAppPermissionMapInternal()).thenReturn(new HashMap<String, AppPermissionSummary>() {
      { put(appId, permissionSummary); }
    });
    LinkedHashMap<Long,
        LinkedHashMap<String,
            LinkedHashMap<String,
                LinkedHashMap<String, LinkedHashMap<String, List<ContinuousVerificationExecutionMetaData>>>>>>
        execData =
            continuousVerificationService.getCVExecutionMetaData(accountId, 1519200000000L, 1519200000001L, user);
    assertThat(execData).isNotNull();
    assertThat(execData).hasSize(0);
  }

  @Test
  @Category(UnitTests.class)
  public void testDataDogMetricEndPointCreation() {
    String expectedDockerCPUMetricURL =
        "query?api_key=${apiKey}&application_key=${applicationKey}&from=${start_time_seconds}&to=${end_time_seconds}&query=docker.cpu.usage{cluster-name:harness-test}.rollup(avg,60)";
    String expectedDockerMEMMetricURL =
        "query?api_key=${apiKey}&application_key=${applicationKey}&from=${start_time_seconds}&to=${end_time_seconds}&query=docker.mem.rss{cluster-name:harness-test}.rollup(avg,60)/docker.mem.limit{cluster-name:harness-test}.rollup(avg,60)*100";
    Map<String, String> dockerMetrics = new HashMap<>();
    dockerMetrics.put("cluster-name:harness-test", "docker.cpu.usage,docker.mem.rss");

    String expectedECSMetricURL =
        "query?api_key=${apiKey}&application_key=${applicationKey}&from=${start_time_seconds}&to=${end_time_seconds}&query=ecs.fargate.cpu.user{cluster-name:sdktest}.rollup(avg,60)";
    Map<String, String> ecsMetrics = new HashMap<>();
    ecsMetrics.put("cluster-name:sdktest", "ecs.fargate.cpu.user");

    String expectedCustomMetricURL =
        "query?api_key=${apiKey}&application_key=${applicationKey}&from=${start_time_seconds}&to=${end_time_seconds}&query=ec2.cpu{service_name:harness}.rollup(avg,60)";
    Map<String, Set<Metric>> customMetricsMap = new HashMap<>();
    Set<Metric> metrics = new HashSet<>();
    metrics.add(Metric.builder()
                    .metricName("ec2.cpu")
                    .displayName("ec2 cpu")
                    .mlMetricType("VALUE")
                    .datadogMetricType("Custom")
                    .build());
    customMetricsMap.put("service_name:harness", metrics);
    Map<String, List<APMMetricInfo>> metricEndPoints =
        continuousVerificationService.createDatadogMetricEndPointMap(dockerMetrics, ecsMetrics, null, customMetricsMap);

    assertThat(4).isEqualTo(metricEndPoints.size());
    assertThat(metricEndPoints.keySet().contains(expectedDockerCPUMetricURL)).isTrue();
    assertThat(metricEndPoints.keySet().contains(expectedDockerMEMMetricURL)).isTrue();
    assertThat(metricEndPoints.keySet().contains(expectedECSMetricURL)).isTrue();
    assertThat(metricEndPoints.keySet().contains(expectedCustomMetricURL)).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testHeatMapResolutionEnum() {
    long endTime = System.currentTimeMillis();

    long twelveHours = endTime - TimeUnit.HOURS.toMillis(12);
    HeatMapResolution heatMapResolution = HeatMapResolution.getResolution(twelveHours, endTime);
    assertThat(heatMapResolution).isEqualTo(HeatMapResolution.TWELVE_HOURS);

    int twelveHoursResolutionDurationInMinutes = VerificationConstants.CRON_POLL_INTERVAL_IN_MINUTES;
    assertThat(heatMapResolution.getDurationOfHeatMapUnit(heatMapResolution))
        .isEqualTo(twelveHoursResolutionDurationInMinutes);

    assertThat(heatMapResolution.getEventsPerHeatMapUnit(heatMapResolution))
        .isEqualTo(twelveHoursResolutionDurationInMinutes / VerificationConstants.CRON_POLL_INTERVAL_IN_MINUTES);

    long oneDay = endTime - TimeUnit.DAYS.toMillis(1);
    heatMapResolution = HeatMapResolution.getResolution(oneDay, endTime);
    assertThat(heatMapResolution).isEqualTo(HeatMapResolution.ONE_DAY);

    int oneDayResolutionDurationInMinutes = 2 * VerificationConstants.CRON_POLL_INTERVAL_IN_MINUTES;
    assertThat(heatMapResolution.getDurationOfHeatMapUnit(heatMapResolution))
        .isEqualTo(oneDayResolutionDurationInMinutes);

    assertThat(heatMapResolution.getEventsPerHeatMapUnit(heatMapResolution))
        .isEqualTo(oneDayResolutionDurationInMinutes / VerificationConstants.CRON_POLL_INTERVAL_IN_MINUTES);

    long sevenDays = endTime - TimeUnit.DAYS.toMillis(7);
    heatMapResolution = HeatMapResolution.getResolution(sevenDays, endTime);
    assertThat(heatMapResolution).isEqualTo(HeatMapResolution.SEVEN_DAYS);

    int sevenDayResolutionDurationInMinutes = 16 * VerificationConstants.CRON_POLL_INTERVAL_IN_MINUTES;
    assertThat(heatMapResolution.getDurationOfHeatMapUnit(heatMapResolution))
        .isEqualTo(sevenDayResolutionDurationInMinutes);

    assertThat(heatMapResolution.getEventsPerHeatMapUnit(heatMapResolution))
        .isEqualTo(sevenDayResolutionDurationInMinutes / VerificationConstants.CRON_POLL_INTERVAL_IN_MINUTES);

    long thirtyDays = endTime - TimeUnit.DAYS.toMillis(30);
    heatMapResolution = HeatMapResolution.getResolution(thirtyDays, endTime);
    assertThat(heatMapResolution).isEqualTo(HeatMapResolution.THIRTY_DAYS);

    int thirtyDayResolutionDurationInMinutes = 48 * VerificationConstants.CRON_POLL_INTERVAL_IN_MINUTES;
    assertThat(heatMapResolution.getDurationOfHeatMapUnit(heatMapResolution))
        .isEqualTo(thirtyDayResolutionDurationInMinutes);

    assertThat(heatMapResolution.getEventsPerHeatMapUnit(heatMapResolution))
        .isEqualTo(thirtyDayResolutionDurationInMinutes / VerificationConstants.CRON_POLL_INTERVAL_IN_MINUTES);
  }

  @Test
  @Category(UnitTests.class)
  public void testCreateCVTask247() {
    CVConfiguration cvConfiguration = mock(CVConfiguration.class);
    Instant startTime = Instant.now().minus(5, ChronoUnit.MINUTES);
    Instant endTime = Instant.now();
    String cvConfigId = UUID.randomUUID().toString();
    when(cvConfigurationService.getConfiguration(eq(cvConfigId))).thenReturn(cvConfiguration);
    when(cvConfiguration.getUuid()).thenReturn(cvConfigId);
    when(cvConfiguration.getAccountId()).thenReturn(accountId);
    DataCollectionInfoV2 dataCollectionInfo = mock(DataCollectionInfoV2.class);
    when(dataCollectionInfoService.create(any(), any(), any())).thenReturn(dataCollectionInfo);
    continuousVerificationService.createCVTask247(cvConfigId, startTime, endTime);
    verify(dataCollectionInfoService).create(eq(cvConfiguration), eq(startTime), eq(endTime));
    ArgumentCaptor<CVTask> cvTaskArgumentCaptor = ArgumentCaptor.forClass(CVTask.class);
    verify(cvTaskService).saveCVTask(cvTaskArgumentCaptor.capture());
    assertEquals(ExecutionStatus.QUEUED, cvTaskArgumentCaptor.getValue().getStatus());
    assertEquals(accountId, cvTaskArgumentCaptor.getValue().getAccountId());
    assertEquals(dataCollectionInfo, cvTaskArgumentCaptor.getValue().getDataCollectionInfo());
  }
}
