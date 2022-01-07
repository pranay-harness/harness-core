/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.dashboard.services.impl;

import static io.harness.cvng.beans.DataSourceType.APP_DYNAMICS;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;
import static io.harness.rule.OwnerRule.KANHAIYA;
import static io.harness.rule.OwnerRule.NEMANJA;
import static io.harness.rule.OwnerRule.PRAVEEN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.activity.entities.Activity;
import io.harness.cvng.activity.entities.DeploymentActivity;
import io.harness.cvng.activity.services.api.ActivityService;
import io.harness.cvng.analysis.beans.Risk;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.TimeSeriesMetricType;
import io.harness.cvng.core.beans.params.PageParams;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.beans.params.TimeRangeParams;
import io.harness.cvng.core.beans.params.filterParams.TimeSeriesAnalysisFilter;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.TimeSeriesRecord;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.TimeSeriesRecordService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.dashboard.beans.TimeSeriesMetricDataDTO;
import io.harness.cvng.dashboard.services.api.TimeSeriesDashboardService;
import io.harness.ng.beans.PageResponse;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class TimeSeriesDashboardServiceImplTest extends CvNextGenTestBase {
  @Inject private TimeSeriesDashboardService timeSeriesDashboardService;
  @Inject private HPersistence hPersistence;

  private String projectIdentifier;
  private String orgIdentifier;
  private String serviceIdentifier;
  private String envIdentifier;
  private String accountId;
  private ServiceEnvironmentParams serviceEnvironmentParams;

  @Mock private CVConfigService cvConfigService;
  @Mock private TimeSeriesRecordService timeSeriesRecordService;
  @Mock private VerificationTaskService verificationTaskService;
  @Mock private ActivityService activityService;

  @Before
  public void setUp() throws Exception {
    projectIdentifier = generateUuid();
    orgIdentifier = generateUuid();
    serviceIdentifier = generateUuid();
    envIdentifier = generateUuid();
    accountId = generateUuid();
    serviceEnvironmentParams = ServiceEnvironmentParams.builder()
                                   .accountIdentifier(accountId)
                                   .orgIdentifier(orgIdentifier)
                                   .projectIdentifier(projectIdentifier)
                                   .serviceIdentifier(serviceIdentifier)
                                   .environmentIdentifier(envIdentifier)
                                   .build();

    MockitoAnnotations.initMocks(this);
    FieldUtils.writeField(timeSeriesDashboardService, "cvConfigService", cvConfigService, true);
    FieldUtils.writeField(timeSeriesDashboardService, "timeSeriesRecordService", timeSeriesRecordService, true);
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
    when(timeSeriesRecordService.getTimeSeriesRecordsForConfigs(any(), any(), any(), anyBoolean()))
        .thenReturn(getTimeSeriesRecords(cvConfigId, false));
    List<String> cvConfigs = Arrays.asList(cvConfigId);
    AppDynamicsCVConfig cvConfig = new AppDynamicsCVConfig();
    cvConfig.setUuid(cvConfigId);
    when(cvConfigService.getConfigsOfProductionEnvironments(accountId, orgIdentifier, projectIdentifier, envIdentifier,
             serviceIdentifier, CVMonitoringCategory.PERFORMANCE))
        .thenReturn(Arrays.asList(cvConfig));

    PageResponse<TimeSeriesMetricDataDTO> response = timeSeriesDashboardService.getSortedMetricData(accountId,
        projectIdentifier, orgIdentifier, envIdentifier, serviceIdentifier, CVMonitoringCategory.PERFORMANCE,
        start.toEpochMilli(), end.toEpochMilli(), start.toEpochMilli(), false, 0, 10, null, null);

    verify(cvConfigService)
        .getConfigsOfProductionEnvironments(accountId, orgIdentifier, projectIdentifier, envIdentifier,
            serviceIdentifier, CVMonitoringCategory.PERFORMANCE);
    assertThat(response).isNotNull();
    assertThat(response.getContent()).isNotEmpty();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetSortedMetricErrorData() throws Exception {
    Instant start = Instant.parse("2020-07-07T02:40:00.000Z");
    Instant end = start.plus(5, ChronoUnit.MINUTES);
    String cvConfigId = generateUuid();
    List<TimeSeriesRecord> timeSeriesRecords = new ArrayList<>();
    timeSeriesRecords.add(TimeSeriesRecord.builder()
                              .verificationTaskId(cvConfigId)
                              .bucketStartTime(start)
                              .metricName("m1")
                              .metricType(TimeSeriesMetricType.THROUGHPUT)
                              .timeSeriesGroupValues(Sets.newHashSet(TimeSeriesRecord.TimeSeriesGroupValue.builder()
                                                                         .groupName("g1")
                                                                         .metricValue(1.0)
                                                                         .timeStamp(start)
                                                                         .build(),
                                  TimeSeriesRecord.TimeSeriesGroupValue.builder()
                                      .groupName("g2")
                                      .metricValue(2.0)
                                      .timeStamp(start)
                                      .build()))
                              .build());

    timeSeriesRecords.add(TimeSeriesRecord.builder()
                              .verificationTaskId(cvConfigId)
                              .bucketStartTime(start)
                              .metricName("m2")
                              .metricType(TimeSeriesMetricType.ERROR)
                              .timeSeriesGroupValues(Sets.newHashSet(TimeSeriesRecord.TimeSeriesGroupValue.builder()
                                                                         .groupName("g1")
                                                                         .metricValue(1.0)
                                                                         .percentValue(2.0)
                                                                         .timeStamp(start)
                                                                         .build(),
                                  TimeSeriesRecord.TimeSeriesGroupValue.builder()
                                      .groupName("g2")
                                      .metricValue(2.0)
                                      .timeStamp(start)
                                      .build()))
                              .build());
    when(timeSeriesRecordService.getTimeSeriesRecordsForConfigs(any(), any(), any(), anyBoolean()))
        .thenReturn(timeSeriesRecords);
    AppDynamicsCVConfig cvConfig = new AppDynamicsCVConfig();
    cvConfig.setUuid(cvConfigId);
    when(cvConfigService.getConfigsOfProductionEnvironments(accountId, orgIdentifier, projectIdentifier, envIdentifier,
             serviceIdentifier, CVMonitoringCategory.PERFORMANCE))
        .thenReturn(Arrays.asList(cvConfig));

    PageResponse<TimeSeriesMetricDataDTO> response = timeSeriesDashboardService.getSortedMetricData(accountId,
        projectIdentifier, orgIdentifier, envIdentifier, serviceIdentifier, CVMonitoringCategory.PERFORMANCE,
        start.toEpochMilli(), end.toEpochMilli(), start.toEpochMilli(), false, 0, 10, null, null);
    List<TimeSeriesMetricDataDTO> timeSeriesMetricDTOs = response.getContent();
    assertThat(timeSeriesMetricDTOs.size()).isEqualTo(4);
    TimeSeriesMetricDataDTO timeSeriesMetricDataDTO = timeSeriesMetricDTOs.get(0);
    assertThat(timeSeriesMetricDataDTO.getMetricType()).isEqualTo(TimeSeriesMetricType.THROUGHPUT);
    assertThat(timeSeriesMetricDataDTO.getMetricName()).isEqualTo("m1");
    assertThat(timeSeriesMetricDataDTO.getGroupName()).isEqualTo("g1");
    assertThat(timeSeriesMetricDataDTO.getMetricDataList().size()).isEqualTo(1);
    assertThat(timeSeriesMetricDataDTO.getMetricDataList().first().getValue()).isEqualTo(1.0, offset(0.00001));

    timeSeriesMetricDataDTO = timeSeriesMetricDTOs.get(1);
    assertThat(timeSeriesMetricDataDTO.getMetricType()).isEqualTo(TimeSeriesMetricType.ERROR);
    assertThat(timeSeriesMetricDataDTO.getMetricName()).isEqualTo("m2");
    assertThat(timeSeriesMetricDataDTO.getGroupName()).isEqualTo("g1");
    assertThat(timeSeriesMetricDataDTO.getMetricDataList().size()).isEqualTo(1);
    assertThat(timeSeriesMetricDataDTO.getMetricDataList().first().getValue()).isEqualTo(2.0, offset(0.00001));

    timeSeriesMetricDataDTO = timeSeriesMetricDTOs.get(2);
    assertThat(timeSeriesMetricDataDTO.getMetricType()).isEqualTo(TimeSeriesMetricType.THROUGHPUT);
    assertThat(timeSeriesMetricDataDTO.getMetricName()).isEqualTo("m1");
    assertThat(timeSeriesMetricDataDTO.getGroupName()).isEqualTo("g2");
    assertThat(timeSeriesMetricDataDTO.getMetricDataList().size()).isEqualTo(1);
    assertThat(timeSeriesMetricDataDTO.getMetricDataList().first().getValue()).isEqualTo(2.0, offset(0.00001));

    timeSeriesMetricDataDTO = timeSeriesMetricDTOs.get(3);
    assertThat(timeSeriesMetricDataDTO.getMetricType()).isEqualTo(TimeSeriesMetricType.ERROR);
    assertThat(timeSeriesMetricDataDTO.getMetricName()).isEqualTo("m2");
    assertThat(timeSeriesMetricDataDTO.getGroupName()).isEqualTo("g2");
    assertThat(timeSeriesMetricDataDTO.getMetricDataList().size()).isEqualTo(1);
    assertThat(timeSeriesMetricDataDTO.getMetricDataList().first().getValue()).isEqualTo(0.0, offset(0.00001));
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetSortedMetricErrorData_withFilter() throws Exception {
    Instant start = Instant.parse("2020-07-07T02:40:00.000Z");
    Instant end = start.plus(5, ChronoUnit.MINUTES);
    String cvConfigId = generateUuid();
    List<TimeSeriesRecord> timeSeriesRecords = new ArrayList<>();
    timeSeriesRecords.add(TimeSeriesRecord.builder()
                              .verificationTaskId(cvConfigId)
                              .bucketStartTime(start)
                              .metricName("m1")
                              .metricType(TimeSeriesMetricType.THROUGHPUT)
                              .timeSeriesGroupValues(Sets.newHashSet(TimeSeriesRecord.TimeSeriesGroupValue.builder()
                                                                         .groupName("g1")
                                                                         .metricValue(1.0)
                                                                         .timeStamp(start)
                                                                         .build(),
                                  TimeSeriesRecord.TimeSeriesGroupValue.builder()
                                      .groupName("g2")
                                      .metricValue(2.0)
                                      .timeStamp(start)
                                      .build()))
                              .build());

    timeSeriesRecords.add(TimeSeriesRecord.builder()
                              .verificationTaskId(cvConfigId)
                              .bucketStartTime(start)
                              .metricName("m2")
                              .metricType(TimeSeriesMetricType.ERROR)
                              .timeSeriesGroupValues(Sets.newHashSet(TimeSeriesRecord.TimeSeriesGroupValue.builder()
                                                                         .groupName("g1")
                                                                         .metricValue(1.0)
                                                                         .percentValue(2.0)
                                                                         .timeStamp(start)
                                                                         .build(),
                                  TimeSeriesRecord.TimeSeriesGroupValue.builder()
                                      .groupName("g2")
                                      .metricValue(2.0)
                                      .timeStamp(start)
                                      .build()))
                              .build());
    when(timeSeriesRecordService.getTimeSeriesRecordsForConfigs(any(), any(), any(), anyBoolean()))
        .thenReturn(timeSeriesRecords);
    AppDynamicsCVConfig cvConfig = new AppDynamicsCVConfig();
    cvConfig.setUuid(cvConfigId);
    when(cvConfigService.getConfigsOfProductionEnvironments(accountId, orgIdentifier, projectIdentifier, envIdentifier,
             serviceIdentifier, CVMonitoringCategory.PERFORMANCE))
        .thenReturn(Arrays.asList(cvConfig));

    PageResponse<TimeSeriesMetricDataDTO> response = timeSeriesDashboardService.getSortedMetricData(accountId,
        projectIdentifier, orgIdentifier, envIdentifier, serviceIdentifier, CVMonitoringCategory.PERFORMANCE,
        start.toEpochMilli(), end.toEpochMilli(), start.toEpochMilli(), false, 0, 10, "m1", null);
    List<TimeSeriesMetricDataDTO> timeSeriesMetricDTOs = response.getContent();
    assertThat(timeSeriesMetricDTOs.size()).isEqualTo(2);
    TimeSeriesMetricDataDTO timeSeriesMetricDataDTO = timeSeriesMetricDTOs.get(0);
    assertThat(timeSeriesMetricDataDTO.getMetricType()).isEqualTo(TimeSeriesMetricType.THROUGHPUT);
    assertThat(timeSeriesMetricDataDTO.getMetricName()).isEqualTo("m1");
    assertThat(timeSeriesMetricDataDTO.getGroupName()).isEqualTo("g1");
    assertThat(timeSeriesMetricDataDTO.getMetricDataList().size()).isEqualTo(1);
    assertThat(timeSeriesMetricDataDTO.getMetricDataList().first().getValue()).isEqualTo(1.0, offset(0.00001));

    timeSeriesMetricDataDTO = timeSeriesMetricDTOs.get(1);
    assertThat(timeSeriesMetricDataDTO.getMetricType()).isEqualTo(TimeSeriesMetricType.THROUGHPUT);
    assertThat(timeSeriesMetricDataDTO.getMetricName()).isEqualTo("m1");
    assertThat(timeSeriesMetricDataDTO.getGroupName()).isEqualTo("g2");
    assertThat(timeSeriesMetricDataDTO.getMetricDataList().size()).isEqualTo(1);
    assertThat(timeSeriesMetricDataDTO.getMetricDataList().first().getValue()).isEqualTo(2.0, offset(0.00001));
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testGetSortedMetricData_withNegativeRiskScoreTotalRiskDoesNotChange() {
    Instant start = Instant.parse("2020-07-07T02:40:00.000Z");
    Instant end = start.plus(5, ChronoUnit.MINUTES);
    String cvConfigId = generateUuid();
    List<TimeSeriesRecord> timeSeriesRecords = new ArrayList<>();
    timeSeriesRecords.add(TimeSeriesRecord.builder()
                              .verificationTaskId(cvConfigId)
                              .bucketStartTime(start)
                              .metricName("m1")
                              .metricType(TimeSeriesMetricType.THROUGHPUT)
                              .timeSeriesGroupValues(Sets.newHashSet(TimeSeriesRecord.TimeSeriesGroupValue.builder()
                                                                         .groupName("g1")
                                                                         .metricValue(1.0)
                                                                         .timeStamp(start)
                                                                         .riskScore(-1)
                                                                         .build()))
                              .build());

    when(timeSeriesRecordService.getTimeSeriesRecordsForConfigs(any(), any(), any(), anyBoolean()))
        .thenReturn(timeSeriesRecords);
    AppDynamicsCVConfig cvConfig = new AppDynamicsCVConfig();
    cvConfig.setUuid(cvConfigId);
    when(cvConfigService.getConfigsOfProductionEnvironments(accountId, orgIdentifier, projectIdentifier, envIdentifier,
             serviceIdentifier, CVMonitoringCategory.PERFORMANCE))
        .thenReturn(Arrays.asList(cvConfig));

    PageResponse<TimeSeriesMetricDataDTO> response = timeSeriesDashboardService.getSortedMetricData(accountId,
        projectIdentifier, orgIdentifier, envIdentifier, serviceIdentifier, CVMonitoringCategory.PERFORMANCE,
        start.toEpochMilli(), end.toEpochMilli(), start.toEpochMilli(), false, 0, 10, "m1", null);
    List<TimeSeriesMetricDataDTO> timeSeriesMetricDTOs = response.getContent();
    assertThat(timeSeriesMetricDTOs.size()).isEqualTo(1);
    assertThat(timeSeriesMetricDTOs.get(0).getTotalRisk()).isEqualTo(0);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetSortedMetricErrorData_withTxnFilter() throws Exception {
    Instant start = Instant.parse("2020-07-07T02:40:00.000Z");
    Instant end = start.plus(5, ChronoUnit.MINUTES);
    String cvConfigId = generateUuid();
    List<TimeSeriesRecord> timeSeriesRecords = new ArrayList<>();
    timeSeriesRecords.add(TimeSeriesRecord.builder()
                              .verificationTaskId(cvConfigId)
                              .bucketStartTime(start)
                              .metricName("m1")
                              .metricType(TimeSeriesMetricType.THROUGHPUT)
                              .timeSeriesGroupValues(Sets.newHashSet(TimeSeriesRecord.TimeSeriesGroupValue.builder()
                                                                         .groupName("g1")
                                                                         .metricValue(1.0)
                                                                         .timeStamp(start)
                                                                         .build(),
                                  TimeSeriesRecord.TimeSeriesGroupValue.builder()
                                      .groupName("g2")
                                      .metricValue(2.0)
                                      .timeStamp(start)
                                      .build()))
                              .build());

    timeSeriesRecords.add(TimeSeriesRecord.builder()
                              .verificationTaskId(cvConfigId)
                              .bucketStartTime(start)
                              .metricName("m2")
                              .metricType(TimeSeriesMetricType.ERROR)
                              .timeSeriesGroupValues(Sets.newHashSet(TimeSeriesRecord.TimeSeriesGroupValue.builder()
                                                                         .groupName("g1")
                                                                         .metricValue(1.0)
                                                                         .percentValue(2.0)
                                                                         .timeStamp(start)
                                                                         .build(),
                                  TimeSeriesRecord.TimeSeriesGroupValue.builder()
                                      .groupName("g2")
                                      .metricValue(2.0)
                                      .timeStamp(start)
                                      .build()))
                              .build());
    when(timeSeriesRecordService.getTimeSeriesRecordsForConfigs(any(), any(), any(), anyBoolean()))
        .thenReturn(timeSeriesRecords);
    AppDynamicsCVConfig cvConfig = new AppDynamicsCVConfig();
    cvConfig.setUuid(cvConfigId);
    when(cvConfigService.getConfigsOfProductionEnvironments(accountId, orgIdentifier, projectIdentifier, envIdentifier,
             serviceIdentifier, CVMonitoringCategory.PERFORMANCE))
        .thenReturn(Arrays.asList(cvConfig));

    PageResponse<TimeSeriesMetricDataDTO> response = timeSeriesDashboardService.getSortedMetricData(accountId,
        projectIdentifier, orgIdentifier, envIdentifier, serviceIdentifier, CVMonitoringCategory.PERFORMANCE,
        start.toEpochMilli(), end.toEpochMilli(), start.toEpochMilli(), false, 0, 10, "g1", null);
    List<TimeSeriesMetricDataDTO> timeSeriesMetricDTOs = response.getContent();
    assertThat(timeSeriesMetricDTOs.size()).isEqualTo(2);
    TimeSeriesMetricDataDTO timeSeriesMetricDataDTO = timeSeriesMetricDTOs.get(0);
    assertThat(timeSeriesMetricDataDTO.getMetricType()).isEqualTo(TimeSeriesMetricType.THROUGHPUT);
    assertThat(timeSeriesMetricDataDTO.getMetricName()).isEqualTo("m1");
    assertThat(timeSeriesMetricDataDTO.getGroupName()).isEqualTo("g1");
    assertThat(timeSeriesMetricDataDTO.getMetricDataList().size()).isEqualTo(1);
    assertThat(timeSeriesMetricDataDTO.getMetricDataList().first().getValue()).isEqualTo(1.0, offset(0.00001));

    timeSeriesMetricDataDTO = timeSeriesMetricDTOs.get(1);
    assertThat(timeSeriesMetricDataDTO.getMetricType()).isEqualTo(TimeSeriesMetricType.ERROR);
    assertThat(timeSeriesMetricDataDTO.getMetricName()).isEqualTo("m2");
    assertThat(timeSeriesMetricDataDTO.getGroupName()).isEqualTo("g1");
    assertThat(timeSeriesMetricDataDTO.getMetricDataList().size()).isEqualTo(1);
    assertThat(timeSeriesMetricDataDTO.getMetricDataList().first().getValue()).isEqualTo(2.0, offset(0.00001));
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetSortedAnomalousMetricData() throws Exception {
    Instant start = Instant.parse("2020-07-07T02:40:00.000Z");
    Instant end = start.plus(5, ChronoUnit.MINUTES);
    String cvConfigId = generateUuid();
    when(timeSeriesRecordService.getTimeSeriesRecordsForConfigs(any(), any(), any(), anyBoolean()))
        .thenReturn(getTimeSeriesRecords(cvConfigId, true));
    List<String> cvConfigs = Arrays.asList(cvConfigId);
    AppDynamicsCVConfig cvConfig = new AppDynamicsCVConfig();
    cvConfig.setUuid(cvConfigId);
    when(cvConfigService.getConfigsOfProductionEnvironments(accountId, orgIdentifier, projectIdentifier, envIdentifier,
             serviceIdentifier, CVMonitoringCategory.PERFORMANCE))
        .thenReturn(Arrays.asList(cvConfig));

    PageResponse<TimeSeriesMetricDataDTO> response = timeSeriesDashboardService.getSortedMetricData(accountId,
        projectIdentifier, orgIdentifier, envIdentifier, serviceIdentifier, CVMonitoringCategory.PERFORMANCE,
        start.toEpochMilli(), end.toEpochMilli(), start.toEpochMilli(), true, 0, 10, null, null);
    verify(cvConfigService)
        .getConfigsOfProductionEnvironments(accountId, orgIdentifier, projectIdentifier, envIdentifier,
            serviceIdentifier, CVMonitoringCategory.PERFORMANCE);
    assertThat(response).isNotNull();
    assertThat(response.getContent()).isNotEmpty();
    assertThat(response.getContent().size()).isEqualTo(response.getPageSize());
    response.getContent().forEach(timeSeriesMetricDataDTO -> {
      assertThat(timeSeriesMetricDataDTO.getMetricDataList()).isNotEmpty();
      timeSeriesMetricDataDTO.getMetricDataList().forEach(
          metricData -> { assertThat(metricData.getRisk()).isNotEqualTo(Risk.HEALTHY); });
    });
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testGetTimeSeriesMetricData_filterByHealthSourceIdentifiers() throws Exception {
    Instant start = Instant.parse("2020-07-07T02:40:00.000Z");
    Instant end = start.plus(5, ChronoUnit.MINUTES);
    TimeRangeParams timeRangeParams = TimeRangeParams.builder().startTime(start).endTime(end).build();
    PageParams pageParams = PageParams.builder().page(0).size(10).build();

    String cvConfigId = generateUuid();
    when(timeSeriesRecordService.getTimeSeriesRecordsForConfigs(any(), any(), any(), anyBoolean()))
        .thenReturn(getTimeSeriesRecords(cvConfigId, true));
    AppDynamicsCVConfig cvConfig = new AppDynamicsCVConfig();
    cvConfig.setUuid(cvConfigId);
    List<String> healthSourceIdentifiers = Arrays.asList(cvConfig.getIdentifier());
    Map<String, DataSourceType> cvConfigToDataSourceTypeMap = new HashMap<>();
    cvConfigToDataSourceTypeMap.put(cvConfigId, APP_DYNAMICS);
    when(cvConfigService.list(serviceEnvironmentParams, healthSourceIdentifiers)).thenReturn(Arrays.asList(cvConfig));
    when(cvConfigService.getDataSourceTypeForCVConfigs(serviceEnvironmentParams, Arrays.asList(cvConfigId)))
        .thenReturn(cvConfigToDataSourceTypeMap);

    TimeSeriesAnalysisFilter timeSeriesAnalysisFilter = TimeSeriesAnalysisFilter.builder()
                                                            .filter(null)
                                                            .anomalous(true)
                                                            .healthSourceIdentifiers(healthSourceIdentifiers)
                                                            .build();

    PageResponse<TimeSeriesMetricDataDTO> response = timeSeriesDashboardService.getTimeSeriesMetricData(
        serviceEnvironmentParams, timeRangeParams, timeSeriesAnalysisFilter, pageParams);

    verify(cvConfigService).list(serviceEnvironmentParams, healthSourceIdentifiers);
    verify(cvConfigService).getDataSourceTypeForCVConfigs(serviceEnvironmentParams, Arrays.asList(cvConfigId));
    assertThat(response).isNotNull();
    assertThat(response.getContent()).isNotEmpty();
    assertThat(response.getContent().size()).isEqualTo(10);

    response.getContent().forEach(timeSeriesMetricDataDTO -> {
      assertThat(timeSeriesMetricDataDTO.getDataSourceType()).isEqualTo(APP_DYNAMICS);
    });

    timeSeriesAnalysisFilter = TimeSeriesAnalysisFilter.builder()
                                   .filter(null)
                                   .anomalous(true)
                                   .healthSourceIdentifiers(Arrays.asList("some-identifier"))
                                   .build();

    response = timeSeriesDashboardService.getTimeSeriesMetricData(
        serviceEnvironmentParams, timeRangeParams, timeSeriesAnalysisFilter, pageParams);
    assertThat(response).isNotNull();
    assertThat(response.getContent()).isEmpty();
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testGetTimeSeriesMetricData_fillersForNoDataPresent() throws Exception {
    Instant start = Instant.parse("2020-07-07T02:40:00.000Z");
    Instant end = start.plus(15, ChronoUnit.MINUTES);
    TimeRangeParams timeRangeParams = TimeRangeParams.builder().startTime(start).endTime(end).build();
    PageParams pageParams = PageParams.builder().page(0).size(10).build();

    String cvConfigId = generateUuid();
    when(timeSeriesRecordService.getTimeSeriesRecordsForConfigs(any(), any(), any(), anyBoolean()))
        .thenReturn(getTimeSeriesRecords(cvConfigId, true));
    AppDynamicsCVConfig cvConfig = new AppDynamicsCVConfig();
    cvConfig.setUuid(cvConfigId);
    List<String> healthSourceIdentifiers = Arrays.asList(cvConfig.getIdentifier());
    Map<String, DataSourceType> cvConfigToDataSourceTypeMap = new HashMap<>();
    cvConfigToDataSourceTypeMap.put(cvConfigId, APP_DYNAMICS);
    when(cvConfigService.list(serviceEnvironmentParams, healthSourceIdentifiers)).thenReturn(Arrays.asList(cvConfig));
    when(cvConfigService.getDataSourceTypeForCVConfigs(serviceEnvironmentParams, Arrays.asList(cvConfigId)))
        .thenReturn(cvConfigToDataSourceTypeMap);

    TimeSeriesAnalysisFilter timeSeriesAnalysisFilter = TimeSeriesAnalysisFilter.builder()
                                                            .filter(null)
                                                            .anomalous(true)
                                                            .healthSourceIdentifiers(healthSourceIdentifiers)
                                                            .build();

    PageResponse<TimeSeriesMetricDataDTO> response = timeSeriesDashboardService.getTimeSeriesMetricData(
        serviceEnvironmentParams, timeRangeParams, timeSeriesAnalysisFilter, pageParams);

    verify(cvConfigService).list(serviceEnvironmentParams, healthSourceIdentifiers);
    verify(cvConfigService).getDataSourceTypeForCVConfigs(serviceEnvironmentParams, Arrays.asList(cvConfigId));
    assertThat(response).isNotNull();
    assertThat(response.getContent()).isNotEmpty();
    assertThat(response.getContent().size()).isEqualTo(10);
    assertThat(response.getTotalPages()).isEqualTo(19);

    response.getContent().forEach(timeSeriesMetricDataDTO -> {
      assertThat(timeSeriesMetricDataDTO.getDataSourceType()).isEqualTo(APP_DYNAMICS);
      assertThat(timeSeriesMetricDataDTO.getMetricDataList()).size().isEqualTo(16);
    });
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void
  testGetTimeSeriesMetricData_fillersForNoDataPresentAlongWithUpdatingTimeStampValuesWithRespectiveValues() {
    Instant start = Instant.parse("2020-07-07T02:40:00.000Z");
    Instant end = start.plus(10, ChronoUnit.MINUTES);
    TimeRangeParams timeRangeParams = TimeRangeParams.builder().startTime(start).endTime(end).build();
    PageParams pageParams = PageParams.builder().page(0).size(10).build();

    String cvConfigId = generateUuid();
    List<TimeSeriesRecord> timeSeriesRecords = new ArrayList<>();
    timeSeriesRecords.add(TimeSeriesRecord.builder()
                              .verificationTaskId(cvConfigId)
                              .bucketStartTime(start)
                              .metricName("m1")
                              .metricType(TimeSeriesMetricType.THROUGHPUT)
                              .timeSeriesGroupValues(Sets.newHashSet(TimeSeriesRecord.TimeSeriesGroupValue.builder()
                                                                         .groupName("g1")
                                                                         .metricValue(1.0)
                                                                         .timeStamp(start.plus(1, ChronoUnit.MINUTES))
                                                                         .build(),
                                  TimeSeriesRecord.TimeSeriesGroupValue.builder()
                                      .groupName("g2")
                                      .metricValue(2.0)
                                      .timeStamp(start.plus(2, ChronoUnit.MINUTES))
                                      .build()))
                              .build());

    timeSeriesRecords.add(TimeSeriesRecord.builder()
                              .verificationTaskId(cvConfigId)
                              .bucketStartTime(start)
                              .metricName("m2")
                              .metricType(TimeSeriesMetricType.ERROR)
                              .timeSeriesGroupValues(Sets.newHashSet(TimeSeriesRecord.TimeSeriesGroupValue.builder()
                                                                         .groupName("g1")
                                                                         .metricValue(1.0)
                                                                         .percentValue(2.0)
                                                                         .timeStamp(start.plus(3, ChronoUnit.MINUTES))
                                                                         .build(),
                                  TimeSeriesRecord.TimeSeriesGroupValue.builder()
                                      .groupName("g2")
                                      .metricValue(2.0)
                                      .timeStamp(start.plus(4, ChronoUnit.MINUTES))
                                      .build()))
                              .build());
    when(timeSeriesRecordService.getTimeSeriesRecordsForConfigs(any(), any(), any(), anyBoolean()))
        .thenReturn(timeSeriesRecords);
    AppDynamicsCVConfig cvConfig = new AppDynamicsCVConfig();
    cvConfig.setUuid(cvConfigId);
    when(cvConfigService.list(any())).thenReturn(Arrays.asList(cvConfig));

    PageResponse<TimeSeriesMetricDataDTO> response = timeSeriesDashboardService.getTimeSeriesMetricData(
        serviceEnvironmentParams, timeRangeParams, TimeSeriesAnalysisFilter.builder().build(), pageParams);

    List<TimeSeriesMetricDataDTO> timeSeriesMetricDTOs = response.getContent();
    assertThat(timeSeriesMetricDTOs.size()).isEqualTo(4);
    TimeSeriesMetricDataDTO timeSeriesMetricDataDTO = timeSeriesMetricDTOs.get(0);
    assertThat(timeSeriesMetricDataDTO.getMetricType()).isEqualTo(TimeSeriesMetricType.THROUGHPUT);
    assertThat(timeSeriesMetricDataDTO.getMetricName()).isEqualTo("m1");
    assertThat(timeSeriesMetricDataDTO.getGroupName()).isEqualTo("g1");
    assertThat(timeSeriesMetricDataDTO.getMetricDataList().size()).isEqualTo(11);
    int noDataRecordCount = 0;
    int index = 0;
    for (TimeSeriesMetricDataDTO.MetricData metricData : timeSeriesMetricDataDTO.getMetricDataList()) {
      if (metricData.getRisk().compareTo(Risk.NO_DATA) == 0) {
        noDataRecordCount++;
      } else {
        assertThat(metricData.getValue()).isEqualTo(1.0, offset(0.00001));
        assertThat(index).isEqualTo(1);
      }
      index++;
    }
    assertThat(noDataRecordCount).isEqualTo(10);

    timeSeriesMetricDataDTO = timeSeriesMetricDTOs.get(1);
    assertThat(timeSeriesMetricDataDTO.getMetricType()).isEqualTo(TimeSeriesMetricType.ERROR);
    assertThat(timeSeriesMetricDataDTO.getMetricName()).isEqualTo("m2");
    assertThat(timeSeriesMetricDataDTO.getGroupName()).isEqualTo("g1");
    assertThat(timeSeriesMetricDataDTO.getMetricDataList().size()).isEqualTo(11);
    noDataRecordCount = 0;
    index = 0;
    for (TimeSeriesMetricDataDTO.MetricData metricData : timeSeriesMetricDataDTO.getMetricDataList()) {
      if (metricData.getRisk().compareTo(Risk.NO_DATA) == 0) {
        noDataRecordCount++;
      } else {
        assertThat(metricData.getValue()).isEqualTo(2.0, offset(0.00001));
        assertThat(index).isEqualTo(3);
      }
      index++;
    }
    assertThat(noDataRecordCount).isEqualTo(10);

    timeSeriesMetricDataDTO = timeSeriesMetricDTOs.get(2);
    assertThat(timeSeriesMetricDataDTO.getMetricType()).isEqualTo(TimeSeriesMetricType.THROUGHPUT);
    assertThat(timeSeriesMetricDataDTO.getMetricName()).isEqualTo("m1");
    assertThat(timeSeriesMetricDataDTO.getGroupName()).isEqualTo("g2");
    assertThat(timeSeriesMetricDataDTO.getMetricDataList().size()).isEqualTo(11);
    noDataRecordCount = 0;
    index = 0;
    for (TimeSeriesMetricDataDTO.MetricData metricData : timeSeriesMetricDataDTO.getMetricDataList()) {
      if (metricData.getRisk().compareTo(Risk.NO_DATA) == 0) {
        noDataRecordCount++;
      } else {
        assertThat(metricData.getValue()).isEqualTo(2.0, offset(0.00001));
        assertThat(index).isEqualTo(2);
      }
      index++;
    }
    assertThat(noDataRecordCount).isEqualTo(10);

    timeSeriesMetricDataDTO = timeSeriesMetricDTOs.get(3);
    assertThat(timeSeriesMetricDataDTO.getMetricType()).isEqualTo(TimeSeriesMetricType.ERROR);
    assertThat(timeSeriesMetricDataDTO.getMetricName()).isEqualTo("m2");
    assertThat(timeSeriesMetricDataDTO.getGroupName()).isEqualTo("g2");
    assertThat(timeSeriesMetricDataDTO.getMetricDataList().size()).isEqualTo(11);
    noDataRecordCount = 0;
    index = 0;
    for (TimeSeriesMetricDataDTO.MetricData metricData : timeSeriesMetricDataDTO.getMetricDataList()) {
      if (metricData.getRisk().compareTo(Risk.NO_DATA) == 0) {
        noDataRecordCount++;
      } else {
        assertThat(metricData.getValue()).isEqualTo(0.0, offset(0.00001));
        assertThat(index).isEqualTo(4);
      }
      index++;
    }
    assertThat(noDataRecordCount).isEqualTo(10);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetSortedAnomalousMetricData_noAnomalies() throws Exception {
    Instant start = Instant.parse("2020-07-07T02:40:00.000Z");
    Instant end = start.plus(5, ChronoUnit.MINUTES);
    String cvConfigId = generateUuid();
    when(timeSeriesRecordService.getTimeSeriesRecordsForConfigs(any(), any(), any(), anyBoolean()))
        .thenReturn(getTimeSeriesRecords(cvConfigId, false));
    List<String> cvConfigs = Arrays.asList(cvConfigId);
    AppDynamicsCVConfig cvConfig = new AppDynamicsCVConfig();
    cvConfig.setUuid(cvConfigId);
    when(cvConfigService.getConfigsOfProductionEnvironments(accountId, orgIdentifier, projectIdentifier, envIdentifier,
             serviceIdentifier, CVMonitoringCategory.PERFORMANCE))
        .thenReturn(Arrays.asList(cvConfig));

    PageResponse<TimeSeriesMetricDataDTO> response = timeSeriesDashboardService.getSortedMetricData(accountId,
        projectIdentifier, orgIdentifier, envIdentifier, serviceIdentifier, CVMonitoringCategory.PERFORMANCE,
        start.toEpochMilli(), end.toEpochMilli(), start.toEpochMilli(), true, 0, 10, null, null);
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
    when(timeSeriesRecordService.getTimeSeriesRecordsForConfigs(any(), any(), any(), anyBoolean()))
        .thenReturn(getTimeSeriesRecords(cvConfigId, true));
    List<String> cvConfigs = Arrays.asList(cvConfigId);
    AppDynamicsCVConfig cvConfig = new AppDynamicsCVConfig();
    cvConfig.setUuid(cvConfigId);
    when(cvConfigService.getConfigsOfProductionEnvironments(
             accountId, orgIdentifier, projectIdentifier, envIdentifier, serviceIdentifier, null))
        .thenReturn(Arrays.asList(cvConfig));

    PageResponse<TimeSeriesMetricDataDTO> response = timeSeriesDashboardService.getSortedMetricData(accountId,
        projectIdentifier, orgIdentifier, envIdentifier, serviceIdentifier, null, start.toEpochMilli(),
        end.toEpochMilli(), start.toEpochMilli(), true, 0, 10, null, null);

    assertThat(response).isNotNull();
    assertThat(response.getContent()).isNotEmpty();
    assertThat(response.getContent().size()).isEqualTo(response.getPageSize());
    verify(cvConfigService)
        .getConfigsOfProductionEnvironments(
            accountId, orgIdentifier, projectIdentifier, envIdentifier, serviceIdentifier, null);
    response.getContent().forEach(timeSeriesMetricDataDTO -> {
      assertThat(timeSeriesMetricDataDTO.getMetricDataList()).isNotEmpty();
      timeSeriesMetricDataDTO.getMetricDataList().forEach(
          metricData -> { assertThat(metricData.getRisk()).isNotEqualTo(Risk.HEALTHY); });
    });
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetSortedAnomalousMetricData_validatePageResponse() throws Exception {
    Instant start = Instant.parse("2020-07-07T02:40:00.000Z");
    Instant end = start.plus(5, ChronoUnit.MINUTES);
    String cvConfigId = generateUuid();
    when(timeSeriesRecordService.getTimeSeriesRecordsForConfigs(any(), any(), any(), anyBoolean()))
        .thenReturn(getTimeSeriesRecords(cvConfigId, true));
    AppDynamicsCVConfig cvConfig = new AppDynamicsCVConfig();
    cvConfig.setUuid(cvConfigId);
    when(cvConfigService.getConfigsOfProductionEnvironments(accountId, orgIdentifier, projectIdentifier, envIdentifier,
             serviceIdentifier, CVMonitoringCategory.PERFORMANCE))
        .thenReturn(Arrays.asList(cvConfig));

    PageResponse<TimeSeriesMetricDataDTO> response = timeSeriesDashboardService.getSortedMetricData(accountId,
        projectIdentifier, orgIdentifier, envIdentifier, serviceIdentifier, CVMonitoringCategory.PERFORMANCE,
        start.toEpochMilli(), end.toEpochMilli(), start.toEpochMilli(), true, 0, 3, null, null);

    assertThat(response).isNotNull();
    assertThat(response.getContent()).isNotEmpty();
    assertThat(response.getTotalPages()).isEqualTo(61);
    assertThat(response.getContent().size()).isEqualTo(3);
    response.getContent().forEach(timeSeriesMetricDataDTO -> {
      assertThat(timeSeriesMetricDataDTO.getMetricDataList()).isNotEmpty();
      timeSeriesMetricDataDTO.getMetricDataList().forEach(
          metricData -> { assertThat(metricData.getRisk()).isNotEqualTo(Risk.HEALTHY); });
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
    when(verificationTaskService.maybeGetVerificationTaskIds(accountId, verificationJobInstanceId))
        .thenReturn(verificationTaskIds);
    when(verificationTaskService.getCVConfigId(taskId)).thenReturn(cvConfigId);
    when(timeSeriesRecordService.getTimeSeriesRecordsForConfigs(any(), any(), any(), anyBoolean()))
        .thenReturn(getTimeSeriesRecords(cvConfigId, true));

    PageResponse<TimeSeriesMetricDataDTO> response =
        timeSeriesDashboardService.getActivityMetrics(activityId, accountId, projectIdentifier, orgIdentifier,
            envIdentifier, serviceIdentifier, start.toEpochMilli(), end.toEpochMilli(), false, 0, 10);
    assertThat(response).isNotNull();
    assertThat(response.getContent()).isNotEmpty();
    assertThat(response.getTotalPages()).isEqualTo(19);
    assertThat(response.getContent().size()).isEqualTo(10);
    response.getContent().forEach(timeSeriesMetricDataDTO -> {
      assertThat(timeSeriesMetricDataDTO.getMetricDataList()).isNotEmpty();
      assertThat(timeSeriesMetricDataDTO.getMetricType()).isNotNull();
      timeSeriesMetricDataDTO.getMetricDataList().forEach(metricData -> {
        assertThat(metricData.getRisk()).isNotEqualTo(Risk.HEALTHY);
        if (TimeSeriesMetricType.ERROR.equals(timeSeriesMetricDataDTO.getMetricType())) {
          assertThat(metricData.getValue()).isGreaterThan(0.0);
        }
      });
    });
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetActivityMetrics_withNoVerificationTaskMapping() throws Exception {
    Instant start = Instant.parse("2020-07-07T02:40:00.000Z");
    Instant end = start.plus(5, ChronoUnit.MINUTES);
    String activityId = generateUuid();
    String cvConfigId = generateUuid();
    String verificationJobInstanceId = generateUuid();
    String taskId = generateUuid();
    Activity activity = DeploymentActivity.builder().deploymentTag("Build23").build();
    activity.setVerificationJobInstanceIds(Arrays.asList(verificationJobInstanceId));
    when(activityService.get(activityId)).thenReturn(activity);
    when(verificationTaskService.maybeGetVerificationTaskIds(accountId, verificationJobInstanceId))
        .thenReturn(Collections.emptySet());
    when(verificationTaskService.getCVConfigId(taskId)).thenReturn(cvConfigId);
    when(timeSeriesRecordService.getTimeSeriesRecordsForConfigs(any(), any(), any(), anyBoolean()))
        .thenReturn(getTimeSeriesRecords(cvConfigId, true));

    PageResponse<TimeSeriesMetricDataDTO> response =
        timeSeriesDashboardService.getActivityMetrics(activityId, accountId, projectIdentifier, orgIdentifier,
            envIdentifier, serviceIdentifier, start.toEpochMilli(), end.toEpochMilli(), false, 0, 10);
    assertThat(response).isNotNull();
    assertThat(response.getContent()).isEmpty();
  }

  private List<TimeSeriesRecord> getTimeSeriesRecords(String cvConfigId, boolean anomalousOnly) throws Exception {
    File file = new File(getResourceFilePath("timeseries/timeseriesRecords.json"));
    final Gson gson = new Gson();
    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      Type type = new TypeToken<List<TimeSeriesRecord>>() {}.getType();
      List<TimeSeriesRecord> timeSeriesMLAnalysisRecords = gson.fromJson(br, type);
      timeSeriesMLAnalysisRecords.forEach(timeSeriesMLAnalysisRecord -> {
        // timeSeriesMLAnalysisRecord.setCvConfigId(cvConfigId);
        timeSeriesMLAnalysisRecord.setVerificationTaskId(cvConfigId);
        timeSeriesMLAnalysisRecord.setBucketStartTime(Instant.parse("2020-07-07T02:40:00.000Z"));
        if (timeSeriesMLAnalysisRecord.getMetricName().equals("Calls per Minute")) {
          Random r = new Random();
          timeSeriesMLAnalysisRecord.getTimeSeriesGroupValues().forEach(
              timeSeriesGroupValue -> timeSeriesGroupValue.setPercentValue(100 * Math.abs(r.nextDouble())));
        }
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
