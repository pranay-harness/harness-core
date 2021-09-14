/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.services.impl;

import io.harness.cvng.beans.DataCollectionRequest;
import io.harness.cvng.beans.SplunkSavedSearch;
import io.harness.cvng.beans.splunk.SplunkLatestHistogramDataCollectionRequest;
import io.harness.cvng.beans.splunk.SplunkSampleDataCollectionRequest;
import io.harness.cvng.beans.splunk.SplunkSavedSearchRequest;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.client.RequestExecutor;
import io.harness.cvng.client.VerificationManagerClient;
import io.harness.cvng.core.beans.MonitoringSourceImportStatus;
import io.harness.cvng.core.beans.OnboardingRequestDTO;
import io.harness.cvng.core.beans.OnboardingResponseDTO;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.services.api.OnboardingService;
import io.harness.cvng.core.services.api.SplunkService;
import io.harness.serializer.JsonUtils;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.inject.Inject;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.List;

public class SplunkServiceImpl implements SplunkService {
  @Inject private VerificationManagerClient verificationManagerClient;
  @Inject private RequestExecutor requestExecutor;
  @Inject private NextGenService nextGenService;

  @Inject private OnboardingService onboardingService;

  @Override
  public List<SplunkSavedSearch> getSavedSearches(String accountId, String orgIdentifier, String projectIdentifier,
      String connectorIdentifier, String requestGuid) {
    DataCollectionRequest request = SplunkSavedSearchRequest.builder().build();

    OnboardingRequestDTO onboardingRequestDTO = OnboardingRequestDTO.builder()
                                                    .dataCollectionRequest(request)
                                                    .connectorIdentifier(connectorIdentifier)
                                                    .accountId(accountId)
                                                    .tracingId(requestGuid)
                                                    .orgIdentifier(orgIdentifier)
                                                    .projectIdentifier(projectIdentifier)
                                                    .build();

    OnboardingResponseDTO response = onboardingService.getOnboardingResponse(accountId, onboardingRequestDTO);
    final Gson gson = new Gson();
    Type type = new TypeToken<List<SplunkSavedSearch>>() {}.getType();
    return gson.fromJson(JsonUtils.asJson(response.getResult()), type);
  }

  @Override
  public List<LinkedHashMap> getSampleData(String accountId, String orgIdentifier, String projectIdentifier,
      String connectorIdentifier, String query, String requestGuid) {
    DataCollectionRequest request = SplunkSampleDataCollectionRequest.builder().query(query).build();

    OnboardingRequestDTO onboardingRequestDTO = OnboardingRequestDTO.builder()
                                                    .dataCollectionRequest(request)
                                                    .connectorIdentifier(connectorIdentifier)
                                                    .accountId(accountId)
                                                    .tracingId(requestGuid)
                                                    .orgIdentifier(orgIdentifier)
                                                    .projectIdentifier(projectIdentifier)
                                                    .build();

    OnboardingResponseDTO response = onboardingService.getOnboardingResponse(accountId, onboardingRequestDTO);
    final Gson gson = new Gson();
    Type type = new TypeToken<List<LinkedHashMap>>() {}.getType();
    return gson.fromJson(JsonUtils.asJson(response.getResult()), type);
  }

  @Override
  public List<LinkedHashMap> getLatestHistogram(String accountId, String orgIdentifier, String projectIdentifier,
      String connectorIdentifier, String query, String requestGuid) {
    DataCollectionRequest request = SplunkLatestHistogramDataCollectionRequest.builder().query(query).build();

    OnboardingRequestDTO onboardingRequestDTO = OnboardingRequestDTO.builder()
                                                    .dataCollectionRequest(request)
                                                    .connectorIdentifier(connectorIdentifier)
                                                    .accountId(accountId)
                                                    .tracingId(requestGuid)
                                                    .orgIdentifier(orgIdentifier)
                                                    .projectIdentifier(projectIdentifier)
                                                    .build();

    OnboardingResponseDTO response = onboardingService.getOnboardingResponse(accountId, onboardingRequestDTO);
    final Gson gson = new Gson();
    Type type = new TypeToken<List<LinkedHashMap>>() {}.getType();
    return gson.fromJson(JsonUtils.asJson(response.getResult()), type);
  }

  public MonitoringSourceImportStatus createMonitoringSourceImportStatus(
      List<CVConfig> cvConfigsGroupedByMonitoringSource, int totalNumberOfEnvironments) {
    throw new UnsupportedOperationException("Not Implemented yet");
  }

  @Override
  public void checkConnectivity(
      String accountId, String orgIdentifier, String projectIdentifier, String connectorIdentifier, String tracingId) {
    getSavedSearches(accountId, connectorIdentifier, orgIdentifier, projectIdentifier, tracingId);
  }
}
