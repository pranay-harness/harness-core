/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.services.api;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PAVIC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.DataCollectionRequest;
import io.harness.cvng.beans.DataCollectionRequestType;
import io.harness.cvng.core.beans.OnboardingRequestDTO;
import io.harness.cvng.core.beans.OnboardingResponseDTO;
import io.harness.cvng.core.beans.TimeSeriesSampleDTO;
import io.harness.cvng.core.beans.datadog.DatadogDashboardDTO;
import io.harness.cvng.core.beans.datadog.DatadogDashboardDetail;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.services.impl.DatadogServiceImpl;
import io.harness.delegate.beans.connector.datadog.DatadogConnectorDTO;
import io.harness.ng.beans.PageResponse;
import io.harness.rule.Owner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DatadogServiceImplTest extends CategoryTest {
  private static final int PAGE_SIZE = 5;
  private static final String FILTER = "Datadog";
  private static final String MOCKED_DASHBOARD_ID = "mocked_dashboard_id";
  private static final String MOCKED_QUERY = "mocked_query";
  private static final String MOCKED_METRIC_NAME = "mocked_metric_name";
  private static final List<Map<String, Object>> mockedDashboards =
      Arrays.asList(createDatadogDashboardResponse("DatadogDashboard1", "path1"),
          createDatadogDashboardResponse("DatadogDashboard2", "path2"),
          createDatadogDashboardResponse("DatadogDashboard3", "path3"),
          createDatadogDashboardResponse("Dashboard4", "path4"), createDatadogDashboardResponse("Dashboard5", "path5"),
          createDatadogDashboardResponse("Dashboard6", "path6"), createDatadogDashboardResponse("Dashboard7", "path7"));

  private static final List<Map<String, Object>> mockedFilteredDashboard =
      mockedDashboards
          .stream()

          .filter(datadogDashboardMap
              -> datadogDashboardMap.get("title").toString().toLowerCase().contains(FILTER.toLowerCase()))
          .collect(Collectors.toList());

  private static final List<String> mockedMetricTags = Arrays.asList("tag1", "tag2", "tag3");
  private static final List<String> mockedActiveMetrics =
      Arrays.asList("activeMetric1", "activeMetric2", "activeMetric3");

  @Mock private OnboardingService mockedOnboardingService;
  @InjectMocks private DatadogService classUnderTest = new DatadogServiceImpl();
  private ProjectParams mockedProjectParams;
  private String connectorIdentifier;
  @Captor private ArgumentCaptor<OnboardingRequestDTO> requestCaptor;

  @Before
  public void setup() {
    mockedProjectParams = ProjectParams.builder()
                              .accountIdentifier(generateUuid())
                              .orgIdentifier(generateUuid())
                              .projectIdentifier(generateUuid())
                              .build();
    connectorIdentifier = generateUuid();
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = PAVIC)
  @Category(UnitTests.class)
  public void testGetAllDashboards() {
    when(mockedOnboardingService.getOnboardingResponse(eq(mockedProjectParams.getAccountIdentifier()), any()))
        .thenReturn(OnboardingResponseDTO.builder().result(mockedDashboards).build());
    PageResponse<DatadogDashboardDTO> dashboardList =
        classUnderTest.getAllDashboards(mockedProjectParams, connectorIdentifier, PAGE_SIZE, 0, null, generateUuid());

    OnboardingRequestDTO onboardingRequestDTO = getAndVerifyOnBoardingRequest();

    DataCollectionRequest<DatadogConnectorDTO> request = onboardingRequestDTO.getDataCollectionRequest();
    assertThat(request.getType().name()).isEqualTo(DataCollectionRequestType.DATADOG_DASHBOARD_LIST.name());
    assertThat(dashboardList).isNotNull();
    assertThat(dashboardList.getContent().size()).isEqualTo(Math.min(mockedDashboards.size(), PAGE_SIZE));
  }

  @Test
  @Owner(developers = PAVIC)
  @Category(UnitTests.class)
  public void testGetAllDashboardsWithFilter() {
    when(mockedOnboardingService.getOnboardingResponse(eq(mockedProjectParams.getAccountIdentifier()), any()))
        .thenReturn(OnboardingResponseDTO.builder().result(mockedDashboards).build());
    PageResponse<DatadogDashboardDTO> dashboardList = classUnderTest.getAllDashboards(
        mockedProjectParams, connectorIdentifier, mockedDashboards.size(), 0, FILTER, generateUuid());

    assertThat(dashboardList.getContent().size()).isEqualTo(mockedFilteredDashboard.size());
  }

  @Test
  @Owner(developers = PAVIC)
  @Category(UnitTests.class)
  public void testGetDashboardDetails() throws Exception {
    when(mockedOnboardingService.getOnboardingResponse(eq(mockedProjectParams.getAccountIdentifier()), any()))
        .thenReturn(OnboardingResponseDTO.builder().result(createMockedDashboardDetailsResponse()).build());
    List<DatadogDashboardDetail> dashboardDetailList = classUnderTest.getDashboardDetails(
        mockedProjectParams, connectorIdentifier, MOCKED_DASHBOARD_ID, generateUuid());

    OnboardingRequestDTO onboardingRequestDTO = getAndVerifyOnBoardingRequest();

    DataCollectionRequest request = onboardingRequestDTO.getDataCollectionRequest();
    assertThat(request.getType().name()).isEqualTo(DataCollectionRequestType.DATADOG_DASHBOARD_DETAILS.name());
    assertThat(dashboardDetailList).isNotNull();
  }

  @Test
  @Owner(developers = PAVIC)
  @Category(UnitTests.class)
  public void testGetMetricTagsList() {
    when(mockedOnboardingService.getOnboardingResponse(eq(mockedProjectParams.getAccountIdentifier()), any()))
        .thenReturn(OnboardingResponseDTO.builder().result(mockedMetricTags).build());

    List<String> metricTags =
        classUnderTest.getMetricTagsList(mockedProjectParams, connectorIdentifier, MOCKED_METRIC_NAME, generateUuid());

    testMetricsListRequest(DataCollectionRequestType.DATADOG_METRIC_TAGS, metricTags, mockedMetricTags);
  }

  @Test
  @Owner(developers = PAVIC)
  @Category(UnitTests.class)
  public void testGetActiveMetrics() {
    when(mockedOnboardingService.getOnboardingResponse(eq(mockedProjectParams.getAccountIdentifier()), any()))
        .thenReturn(OnboardingResponseDTO.builder().result(mockedActiveMetrics).build());

    List<String> metricTags = classUnderTest.getActiveMetrics(mockedProjectParams, connectorIdentifier, generateUuid());

    testMetricsListRequest(DataCollectionRequestType.DATADOG_ACTIVE_METRICS, metricTags, mockedActiveMetrics);
  }

  @Test
  @Owner(developers = PAVIC)
  @Category(UnitTests.class)
  public void testGetTimeSeriesPoints() {
    when(mockedOnboardingService.getOnboardingResponse(eq(mockedProjectParams.getAccountIdentifier()), any()))
        .thenReturn(OnboardingResponseDTO.builder().result(new ArrayList<>()).build());

    List<TimeSeriesSampleDTO> timeSeriesPoints =
        classUnderTest.getTimeSeriesPoints(mockedProjectParams, connectorIdentifier, generateUuid(), MOCKED_QUERY);

    OnboardingRequestDTO onboardingRequestDTO = getAndVerifyOnBoardingRequest();

    DataCollectionRequest request = onboardingRequestDTO.getDataCollectionRequest();
    assertThat(request.getType().name()).isEqualTo(DataCollectionRequestType.DATADOG_TIME_SERIES_POINTS.name());
    assertThat(timeSeriesPoints).isNotNull();
  }

  @Test
  @Owner(developers = PAVIC)
  @Category(UnitTests.class)
  public void testGetSampleLogData() {}

  private void testMetricsListRequest(
      DataCollectionRequestType dataCollectionRequestType, List<String> result, List<String> expectedResult) {
    OnboardingRequestDTO onboardingRequestDTO = getAndVerifyOnBoardingRequest();

    DataCollectionRequest request = onboardingRequestDTO.getDataCollectionRequest();
    assertThat(request.getType().name()).isEqualTo(dataCollectionRequestType.name());
    assertThat(result).isNotNull();
    assertThat(result).containsAll(expectedResult);
  }

  private OnboardingRequestDTO getAndVerifyOnBoardingRequest() {
    verify(mockedOnboardingService)
        .getOnboardingResponse(eq(mockedProjectParams.getAccountIdentifier()), requestCaptor.capture());
    OnboardingRequestDTO onboardingRequestDTO = requestCaptor.getValue();
    assertThat(onboardingRequestDTO.getOrgIdentifier()).isEqualTo(mockedProjectParams.getOrgIdentifier());
    assertThat(onboardingRequestDTO.getConnectorIdentifier()).isEqualTo(connectorIdentifier);
    assertThat(onboardingRequestDTO.getAccountId()).isEqualTo(mockedProjectParams.getAccountIdentifier());
    assertThat(onboardingRequestDTO.getProjectIdentifier()).isEqualTo(mockedProjectParams.getProjectIdentifier());
    assertThat(onboardingRequestDTO.getDataCollectionRequest()).isNotNull();
    return onboardingRequestDTO;
  }

  private static Map<String, Object> createDatadogDashboardResponse(String name, String path) {
    Map<String, Object> mockedResponse = new LinkedHashMap<>();
    mockedResponse.put("id", generateUuid());
    mockedResponse.put("title", name);
    mockedResponse.put("url", path);
    return mockedResponse;
  }

  private static List<Map<String, Object>> createMockedDashboardDetailsResponse() {
    List<Map<String, Object>> widgets = new ArrayList<>();
    Map<String, Object> mockedWidget = new LinkedHashMap<>();
    mockedWidget.put("widgetName", "Test widget name");
    List<Map<String, Object>> mockedDataSets = new ArrayList<>();
    Map<String, Object> mockedDataSet = new LinkedHashMap<>();
    mockedDataSet.put("name", "Test query name");
    mockedDataSet.put("query", "Test query");
    mockedDataSets.add(mockedDataSet);
    mockedWidget.put("dataSets", mockedDataSets);
    widgets.add(mockedWidget);
    return widgets;
  }
}
