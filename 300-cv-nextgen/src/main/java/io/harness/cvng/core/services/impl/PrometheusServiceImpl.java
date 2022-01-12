/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.services.impl;

import io.harness.cvng.beans.DataCollectionRequest;
import io.harness.cvng.beans.DataCollectionRequestType;
import io.harness.cvng.beans.prometheus.PrometheusFetchSampleDataRequest;
import io.harness.cvng.beans.prometheus.PrometheusLabelNamesFetchRequest;
import io.harness.cvng.beans.prometheus.PrometheusLabelValuesFetchRequest;
import io.harness.cvng.beans.prometheus.PrometheusMetricListFetchRequest;
import io.harness.cvng.core.beans.OnboardingRequestDTO;
import io.harness.cvng.core.beans.OnboardingResponseDTO;
import io.harness.cvng.core.beans.PrometheusSampleData;
import io.harness.cvng.core.services.api.OnboardingService;
import io.harness.cvng.core.services.api.PrometheusService;
import io.harness.cvng.core.utils.DateTimeUtils;
import io.harness.serializer.JsonUtils;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.inject.Inject;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class PrometheusServiceImpl implements PrometheusService {
  @Inject OnboardingService onboardingService;
  @Override
  public List<String> getMetricNames(
      String accountId, String connectorIdentifier, String orgIdentifier, String projectIdentifier, String tracingId) {
    DataCollectionRequest request =
        PrometheusMetricListFetchRequest.builder().type(DataCollectionRequestType.PROMETHEUS_METRIC_LIST_GET).build();
    return callOnboardingServiceForStringList(
        request, accountId, connectorIdentifier, orgIdentifier, projectIdentifier, tracingId);
  }

  @Override
  public List<String> getLabelNames(
      String accountId, String connectorIdentifier, String orgIdentifier, String projectIdentifier, String tracingId) {
    DataCollectionRequest request =
        PrometheusLabelNamesFetchRequest.builder().type(DataCollectionRequestType.PROMETHEUS_LABEL_NAMES_GET).build();
    return callOnboardingServiceForStringList(
        request, accountId, connectorIdentifier, orgIdentifier, projectIdentifier, tracingId);
  }

  @Override
  public List<String> getLabelValues(String accountId, String connectorIdentifier, String orgIdentifier,
      String projectIdentifier, String labelName, String tracingId) {
    DataCollectionRequest request = PrometheusLabelValuesFetchRequest.builder()
                                        .type(DataCollectionRequestType.PROMETHEUS_LABEL_VALUES_GET)
                                        .labelName(labelName)
                                        .build();
    return callOnboardingServiceForStringList(
        request, accountId, connectorIdentifier, orgIdentifier, projectIdentifier, tracingId);
  }

  private List<String> callOnboardingServiceForStringList(DataCollectionRequest request, String accountId,
      String connectorIdentifier, String orgIdentifier, String projectIdentifier, String tracingId) {
    OnboardingRequestDTO onboardingRequestDTO = OnboardingRequestDTO.builder()
                                                    .dataCollectionRequest(request)
                                                    .connectorIdentifier(connectorIdentifier)
                                                    .accountId(accountId)
                                                    .orgIdentifier(orgIdentifier)
                                                    .tracingId(tracingId)
                                                    .projectIdentifier(projectIdentifier)
                                                    .build();

    OnboardingResponseDTO response = onboardingService.getOnboardingResponse(accountId, onboardingRequestDTO);
    final Gson gson = new Gson();
    Type type = new TypeToken<List<String>>() {}.getType();
    return gson.fromJson(JsonUtils.asJson(response.getResult()), type);
  }

  @Override
  public List<PrometheusSampleData> getSampleData(String accountId, String connectorIdentifier, String orgIdentifier,
      String projectIdentifier, String query, String tracingId) {
    Instant time = DateTimeUtils.roundDownTo1MinBoundary(Instant.now());
    DataCollectionRequest request = PrometheusFetchSampleDataRequest.builder()
                                        .type(DataCollectionRequestType.PROMETHEUS_SAMPLE_DATA)
                                        .query(query)
                                        .startTime(time.minus(Duration.of(2, ChronoUnit.HOURS)))
                                        .endTime(time)
                                        .build();
    OnboardingRequestDTO onboardingRequestDTO = OnboardingRequestDTO.builder()
                                                    .dataCollectionRequest(request)
                                                    .connectorIdentifier(connectorIdentifier)
                                                    .accountId(accountId)
                                                    .orgIdentifier(orgIdentifier)
                                                    .tracingId(tracingId)
                                                    .projectIdentifier(projectIdentifier)
                                                    .build();

    OnboardingResponseDTO response = onboardingService.getOnboardingResponse(accountId, onboardingRequestDTO);
    final Gson gson = new Gson();
    Type type = new TypeToken<List<PrometheusSampleData>>() {}.getType();
    return gson.fromJson(JsonUtils.asJson(response.getResult()), type);
  }
}
