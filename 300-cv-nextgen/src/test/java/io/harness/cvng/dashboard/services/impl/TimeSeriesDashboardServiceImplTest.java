package io.harness.cvng.dashboard.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.NEMANJA;
import static io.harness.rule.OwnerRule.PRAVEEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.activity.entities.Activity;
import io.harness.cvng.activity.entities.DeploymentActivity;
import io.harness.cvng.activity.services.api.ActivityService;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.TimeSeriesRecord;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.TimeSeriesService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.dashboard.beans.TimeSeriesMetricDataDTO;
import io.harness.cvng.dashboard.services.api.TimeSeriesDashboardService;
import io.harness.ng.beans.PageResponse;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class TimeSeriesDashboardServiceImplTest extends CvNextGenTest {
  @Inject private TimeSeriesDashboardService timeSeriesDashboardService;
  @Inject private HPersistence hPersistence;

  private String projectIdentifier;
  private String orgIdentifier;
  private String serviceIdentifier;
  private String envIdentifier;
  private String accountId;

  @Mock private CVConfigService cvConfigService;
  @Mock private TimeSeriesService timeSeriesService;
  @Mock private VerificationTaskService verificationTaskService;
  @Mock private ActivityService activityService;

  @Before
  public void setUp() throws Exception {
    projectIdentifier = generateUuid();
    orgIdentifier = generateUuid();
    serviceIdentifier = generateUuid();
    envIdentifier = generateUuid();
    accountId = generateUuid();

    MockitoAnnotations.initMocks(this);
    FieldUtils.writeField(timeSeriesDashboardService, "cvConfigService", cvConfigService, true);
    FieldUtils.writeField(timeSeriesDashboardService, "timeSeriesService", timeSeriesService, true);
    FieldUtils.writeField(timeSeriesDashboardService, "verificationTaskService", verificationTaskService, true);
    FieldUtils.writeField(timeSeriesDashboardService, "activityService", activityService, true);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetSortedMetricData() throws Exception {
    Instant start = Instant.parse("2020-07-07T02:40:00.000Z");
    Instant end = start.plus(5, ChronoUnit.MINUTES);
    String cvConfigId = generateUuid();
    when(timeSeriesService.getTimeSeriesRecordsForConfigs(any(), any(), any(), anyBoolean()))
        .thenReturn(getTimeSeriesRecords(cvConfigId, false));
    List<String> cvConfigs = Arrays.asList(cvConfigId);
    AppDynamicsCVConfig cvConfig = new AppDynamicsCVConfig();
    cvConfig.setUuid(cvConfigId);
    when(cvConfigService.getConfigsOfProductionEnvironments(accountId, orgIdentifier, projectIdentifier, envIdentifier,
             serviceIdentifier, CVMonitoringCategory.PERFORMANCE))
        .thenReturn(Arrays.asList(cvConfig));

    PageResponse<TimeSeriesMetricDataDTO> response =
        timeSeriesDashboardService.getSortedMetricData(accountId, projectIdentifier, orgIdentifier, envIdentifier,
            serviceIdentifier, CVMonitoringCategory.PERFORMANCE, start.toEpochMilli(), end.toEpochMilli(), 0, 10);

    verify(cvConfigService)
        .getConfigsOfProductionEnvironments(accountId, orgIdentifier, projectIdentifier, envIdentifier,
            serviceIdentifier, CVMonitoringCategory.PERFORMANCE);
    assertThat(response).isNotNull();
    assertThat(response.getContent()).isNotEmpty();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetSortedAnomalousMetricData() throws Exception {
    Instant start = Instant.parse("2020-07-07T02:40:00.000Z");
    Instant end = start.plus(5, ChronoUnit.MINUTES);
    String cvConfigId = generateUuid();
    when(timeSeriesService.getTimeSeriesRecordsForConfigs(any(), any(), any(), anyBoolean()))
        .thenReturn(getTimeSeriesRecords(cvConfigId, true));
    List<String> cvConfigs = Arrays.asList(cvConfigId);
    AppDynamicsCVConfig cvConfig = new AppDynamicsCVConfig();
    cvConfig.setUuid(cvConfigId);
    when(cvConfigService.getConfigsOfProductionEnvironments(accountId, orgIdentifier, projectIdentifier, envIdentifier,
             serviceIdentifier, CVMonitoringCategory.PERFORMANCE))
        .thenReturn(Arrays.asList(cvConfig));

    PageResponse<TimeSeriesMetricDataDTO> response = timeSeriesDashboardService.getSortedAnomalousMetricData(accountId,
        projectIdentifier, orgIdentifier, envIdentifier, serviceIdentifier, CVMonitoringCategory.PERFORMANCE,
        start.toEpochMilli(), end.toEpochMilli(), 0, 10);
    verify(cvConfigService)
        .getConfigsOfProductionEnvironments(accountId, orgIdentifier, projectIdentifier, envIdentifier,
            serviceIdentifier, CVMonitoringCategory.PERFORMANCE);
    assertThat(response).isNotNull();
    assertThat(response.getContent()).isNotEmpty();
    assertThat(response.getContent().size()).isEqualTo(response.getPageSize());
    response.getContent().forEach(timeSeriesMetricDataDTO -> {
      assertThat(timeSeriesMetricDataDTO.getMetricDataList()).isNotEmpty();
      timeSeriesMetricDataDTO.getMetricDataList().forEach(metricData -> {
        assertThat(metricData.getRisk().name()).isNotEqualTo(TimeSeriesMetricDataDTO.TimeSeriesRisk.LOW_RISK.name());
      });
    });
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetSortedAnomalousMetricData_noAnomalies() throws Exception {
    Instant start = Instant.parse("2020-07-07T02:40:00.000Z");
    Instant end = start.plus(5, ChronoUnit.MINUTES);
    String cvConfigId = generateUuid();
    when(timeSeriesService.getTimeSeriesRecordsForConfigs(any(), any(), any(), anyBoolean()))
        .thenReturn(getTimeSeriesRecords(cvConfigId, false));
    List<String> cvConfigs = Arrays.asList(cvConfigId);
    AppDynamicsCVConfig cvConfig = new AppDynamicsCVConfig();
    cvConfig.setUuid(cvConfigId);
    when(cvConfigService.getConfigsOfProductionEnvironments(accountId, orgIdentifier, projectIdentifier, envIdentifier,
             serviceIdentifier, CVMonitoringCategory.PERFORMANCE))
        .thenReturn(Arrays.asList(cvConfig));

    PageResponse<TimeSeriesMetricDataDTO> response = timeSeriesDashboardService.getSortedAnomalousMetricData(accountId,
        projectIdentifier, orgIdentifier, envIdentifier, serviceIdentifier, CVMonitoringCategory.PERFORMANCE,
        start.toEpochMilli(), end.toEpochMilli(), 0, 10);
    verify(cvConfigService)
        .getConfigsOfProductionEnvironments(accountId, orgIdentifier, projectIdentifier, envIdentifier,
            serviceIdentifier, CVMonitoringCategory.PERFORMANCE);
    assertThat(response).isNotNull();
    assertThat(response.getContent()).isEmpty();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetSortedAnomalousMetricData_noCategory() throws Exception {
    Instant start = Instant.parse("2020-07-07T02:40:00.000Z");
    Instant end = start.plus(5, ChronoUnit.MINUTES);
    String cvConfigId = generateUuid();
    when(timeSeriesService.getTimeSeriesRecordsForConfigs(any(), any(), any(), anyBoolean()))
        .thenReturn(getTimeSeriesRecords(cvConfigId, true));
    List<String> cvConfigs = Arrays.asList(cvConfigId);
    AppDynamicsCVConfig cvConfig = new AppDynamicsCVConfig();
    cvConfig.setUuid(cvConfigId);
    when(cvConfigService.getConfigsOfProductionEnvironments(
             accountId, orgIdentifier, projectIdentifier, envIdentifier, serviceIdentifier, null))
        .thenReturn(Arrays.asList(cvConfig));

    PageResponse<TimeSeriesMetricDataDTO> response =
        timeSeriesDashboardService.getSortedAnomalousMetricData(accountId, projectIdentifier, orgIdentifier,
            envIdentifier, serviceIdentifier, null, start.toEpochMilli(), end.toEpochMilli(), 0, 10);

    assertThat(response).isNotNull();
    assertThat(response.getContent()).isNotEmpty();
    assertThat(response.getContent().size()).isEqualTo(response.getPageSize());
    verify(cvConfigService)
        .getConfigsOfProductionEnvironments(
            accountId, orgIdentifier, projectIdentifier, envIdentifier, serviceIdentifier, null);
    response.getContent().forEach(timeSeriesMetricDataDTO -> {
      assertThat(timeSeriesMetricDataDTO.getMetricDataList()).isNotEmpty();
      timeSeriesMetricDataDTO.getMetricDataList().forEach(metricData -> {
        assertThat(metricData.getRisk().name()).isNotEqualTo(TimeSeriesMetricDataDTO.TimeSeriesRisk.LOW_RISK.name());
      });
    });
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetSortedAnomalousMetricData_validatePageResponse() throws Exception {
    Instant start = Instant.parse("2020-07-07T02:40:00.000Z");
    Instant end = start.plus(5, ChronoUnit.MINUTES);
    String cvConfigId = generateUuid();
    when(timeSeriesService.getTimeSeriesRecordsForConfigs(any(), any(), any(), anyBoolean()))
        .thenReturn(getTimeSeriesRecords(cvConfigId, true));
    AppDynamicsCVConfig cvConfig = new AppDynamicsCVConfig();
    cvConfig.setUuid(cvConfigId);
    when(cvConfigService.getConfigsOfProductionEnvironments(accountId, orgIdentifier, projectIdentifier, envIdentifier,
             serviceIdentifier, CVMonitoringCategory.PERFORMANCE))
        .thenReturn(Arrays.asList(cvConfig));

    PageResponse<TimeSeriesMetricDataDTO> response = timeSeriesDashboardService.getSortedAnomalousMetricData(accountId,
        projectIdentifier, orgIdentifier, envIdentifier, serviceIdentifier, CVMonitoringCategory.PERFORMANCE,
        start.toEpochMilli(), end.toEpochMilli(), 0, 3);

    assertThat(response).isNotNull();
    assertThat(response.getContent()).isNotEmpty();
    assertThat(response.getTotalPages()).isEqualTo(41);
    assertThat(response.getContent().size()).isEqualTo(3);
    response.getContent().forEach(timeSeriesMetricDataDTO -> {
      assertThat(timeSeriesMetricDataDTO.getMetricDataList()).isNotEmpty();
      timeSeriesMetricDataDTO.getMetricDataList().forEach(metricData -> {
        assertThat(metricData.getRisk().name()).isNotEqualTo(TimeSeriesMetricDataDTO.TimeSeriesRisk.LOW_RISK.name());
      });
    });
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetActivityMetrics() throws Exception {
    Instant start = Instant.parse("2020-07-07T02:40:00.000Z");
    Instant end = start.plus(5, ChronoUnit.MINUTES);
    String activityId = generateUuid();
    String cvConfigId = generateUuid();
    String verificationJobInstanceId = generateUuid();
    String taskId = generateUuid();
    Activity activity = DeploymentActivity.builder().deploymentTag("Build23").build();
    activity.setVerificationJobInstanceIds(Arrays.asList(verificationJobInstanceId));
    when(activityService.get(activityId)).thenReturn(activity);

    Set<String> verificationTaskIds = new HashSet<>();
    verificationTaskIds.add(taskId);
    when(verificationTaskService.getVerificationTaskIds(accountId, verificationJobInstanceId))
        .thenReturn(verificationTaskIds);
    when(verificationTaskService.getCVConfigId(taskId)).thenReturn(cvConfigId);
    when(timeSeriesService.getTimeSeriesRecordsForConfigs(any(), any(), any(), anyBoolean()))
        .thenReturn(getTimeSeriesRecords(cvConfigId, true));

    PageResponse<TimeSeriesMetricDataDTO> response =
        timeSeriesDashboardService.getActivityMetrics(activityId, accountId, projectIdentifier, orgIdentifier,
            envIdentifier, serviceIdentifier, start.toEpochMilli(), end.toEpochMilli(), false, 0, 10);
    assertThat(response).isNotNull();
    assertThat(response.getContent()).isNotEmpty();
    assertThat(response.getTotalPages()).isEqualTo(13);
    assertThat(response.getContent().size()).isEqualTo(10);
    response.getContent().forEach(timeSeriesMetricDataDTO -> {
      assertThat(timeSeriesMetricDataDTO.getMetricDataList()).isNotEmpty();
      timeSeriesMetricDataDTO.getMetricDataList().forEach(metricData -> {
        assertThat(metricData.getRisk().name()).isNotEqualTo(TimeSeriesMetricDataDTO.TimeSeriesRisk.LOW_RISK.name());
      });
    });
  }

  private List<TimeSeriesRecord> getTimeSeriesRecords(String cvConfigId, boolean anomalousOnly) throws Exception {
    File file = new File(getClass().getClassLoader().getResource("timeseries/timeseriesRecords.json").getFile());
    final Gson gson = new Gson();
    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      Type type = new TypeToken<List<TimeSeriesRecord>>() {}.getType();
      List<TimeSeriesRecord> timeSeriesMLAnalysisRecords = gson.fromJson(br, type);
      timeSeriesMLAnalysisRecords.forEach(timeSeriesMLAnalysisRecord -> {
        timeSeriesMLAnalysisRecord.setCvConfigId(cvConfigId);
        timeSeriesMLAnalysisRecord.setBucketStartTime(Instant.parse("2020-07-07T02:40:00.000Z"));
        timeSeriesMLAnalysisRecord.getTimeSeriesGroupValues().forEach(groupVal -> {
          Instant baseTime = Instant.parse("2020-07-07T02:40:00.000Z");
          Random random = new Random();
          groupVal.setTimeStamp(baseTime.plus(random.nextInt(4), ChronoUnit.MINUTES));
          if (anomalousOnly) {
            groupVal.setRiskScore(2);
          }
        });
      });
      return timeSeriesMLAnalysisRecords;
    }
  }
}
