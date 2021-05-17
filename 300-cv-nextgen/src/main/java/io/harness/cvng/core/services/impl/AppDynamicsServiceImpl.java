package io.harness.cvng.core.services.impl;

import static io.harness.annotations.dev.HarnessTeam.CV;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.AppdynamicsValidationResponse;
import io.harness.cvng.beans.AppdynamicsValidationResponse.AppdynamicsMetricValueValidationResponse;
import io.harness.cvng.beans.AppdynamicsValidationResponse.AppdynamicsValidationResponseBuilder;
import io.harness.cvng.beans.DataCollectionRequest;
import io.harness.cvng.beans.DataCollectionRequestType;
import io.harness.cvng.beans.MetricPackDTO;
import io.harness.cvng.beans.ThirdPartyApiResponseStatus;
import io.harness.cvng.beans.appd.AppDynamicsApplication;
import io.harness.cvng.beans.appd.AppDynamicsFetchAppRequest;
import io.harness.cvng.beans.appd.AppDynamicsFetchTiersRequest;
import io.harness.cvng.beans.appd.AppDynamicsMetricDataValidationRequest;
import io.harness.cvng.beans.appd.Tier;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.client.RequestExecutor;
import io.harness.cvng.client.VerificationManagerClient;
import io.harness.cvng.core.beans.AppdynamicsImportStatus;
import io.harness.cvng.core.beans.MonitoringSourceImportStatus;
import io.harness.cvng.core.beans.OnboardingRequestDTO;
import io.harness.cvng.core.beans.OnboardingResponseDTO;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.services.api.AppDynamicsService;
import io.harness.cvng.core.services.api.MetricPackService;
import io.harness.cvng.core.services.api.OnboardingService;
import io.harness.datacollection.entity.TimeSeriesRecord;
import io.harness.ng.beans.PageResponse;
import io.harness.serializer.JsonUtils;
import io.harness.utils.PageUtils;

import com.google.common.base.Preconditions;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.inject.Inject;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CV)
public class AppDynamicsServiceImpl implements AppDynamicsService {
  @Inject private VerificationManagerClient verificationManagerClient;
  @Inject private RequestExecutor requestExecutor;
  @Inject private MetricPackService metricPackService;
  @Inject private NextGenService nextGenService;
  @Inject private OnboardingService onboardingService;

  @Override
  // TODO: We need to find a testing strategy for Retrofit interfaces. The current way of mocking Call is too cumbersome
  public Set<AppdynamicsValidationResponse> getMetricPackData(String accountId, String connectorIdentifier,
      String orgIdentifier, String projectIdentifier, String appName, String tierName, String requestGuid,
      List<MetricPackDTO> metricPacks) {
    // TODO: move this logic to one call when we have ability to iterate through a map in DSL
    Set<AppdynamicsValidationResponse> validationResponses = new HashSet<>();
    metricPacks.forEach(metricPack -> {
      DataCollectionRequest request = AppDynamicsMetricDataValidationRequest.builder()
                                          .applicationName(appName)
                                          .tierName(tierName)
                                          .metricPack(metricPack)
                                          .type(DataCollectionRequestType.APPDYNAMICS_GET_METRIC_DATA)
                                          .build();

      OnboardingRequestDTO onboardingRequestDTO = OnboardingRequestDTO.builder()
                                                      .dataCollectionRequest(request)
                                                      .connectorIdentifier(connectorIdentifier)
                                                      .accountId(accountId)
                                                      .orgIdentifier(orgIdentifier)
                                                      .projectIdentifier(projectIdentifier)
                                                      .tracingId(requestGuid)
                                                      .build();

      try {
        OnboardingResponseDTO response = onboardingService.getOnboardingResponse(accountId, onboardingRequestDTO);
        final Gson gson = new Gson();
        Type type = new TypeToken<List<TimeSeriesRecord>>() {}.getType();
        List<TimeSeriesRecord> timeSeriesRecords = gson.fromJson(JsonUtils.asJson(response.getResult()), type);
        AppdynamicsValidationResponseBuilder validationResponseBuilder =
            AppdynamicsValidationResponse.builder().metricPackName(metricPack.getIdentifier());
        AtomicReference<ThirdPartyApiResponseStatus> overAllStatus =
            new AtomicReference<>(ThirdPartyApiResponseStatus.SUCCESS);
        metricPack.getMetrics()
            .stream()
            .filter(metricDefinitionDTO -> metricDefinitionDTO.isIncluded())
            .forEach(metricDefinition -> {
              TimeSeriesRecord timeSeriesRecord =
                  timeSeriesRecords.stream()
                      .filter(record -> record.getMetricName().equals(metricDefinition.getName()))
                      .findFirst()
                      .orElse(null);

              if (timeSeriesRecord == null) {
                validationResponseBuilder.addValidationResponse(
                    AppdynamicsMetricValueValidationResponse.builder()
                        .metricName(metricDefinition.getName())
                        .apiResponseStatus(ThirdPartyApiResponseStatus.NO_DATA)
                        .build());
                overAllStatus.set(ThirdPartyApiResponseStatus.NO_DATA);
              } else {
                validationResponseBuilder.addValidationResponse(
                    AppdynamicsMetricValueValidationResponse.builder()
                        .metricName(metricDefinition.getName())
                        .apiResponseStatus(ThirdPartyApiResponseStatus.SUCCESS)
                        .value(timeSeriesRecord.getMetricValue())
                        .build());
              }
            });
        validationResponses.add(validationResponseBuilder.overallStatus(overAllStatus.get()).build());
      } catch (Exception e) {
        validationResponses.add(AppdynamicsValidationResponse.builder()
                                    .metricPackName(metricPack.getIdentifier())
                                    .overallStatus(ThirdPartyApiResponseStatus.FAILED)
                                    .build());
      }
    });

    return validationResponses;
  }

  @Override
  public PageResponse<AppDynamicsApplication> getApplications(String accountId, String connectorIdentifier,
      String orgIdentifier, String projectIdentifier, int offset, int pageSize, String filter) {
    DataCollectionRequest request =
        AppDynamicsFetchAppRequest.builder().type(DataCollectionRequestType.APPDYNAMICS_FETCH_APPS).build();

    OnboardingRequestDTO onboardingRequestDTO = OnboardingRequestDTO.builder()
                                                    .dataCollectionRequest(request)
                                                    .connectorIdentifier(connectorIdentifier)
                                                    .accountId(accountId)
                                                    .orgIdentifier(orgIdentifier)
                                                    .projectIdentifier(projectIdentifier)
                                                    .build();

    OnboardingResponseDTO response = onboardingService.getOnboardingResponse(accountId, onboardingRequestDTO);

    final Gson gson = new Gson();
    Type type = new TypeToken<List<AppDynamicsApplication>>() {}.getType();
    List<AppDynamicsApplication> applications = gson.fromJson(JsonUtils.asJson(response.getResult()), type);
    if (isNotEmpty(filter)) {
      applications = applications.stream()
                         .filter(appDynamicsApplication
                             -> appDynamicsApplication.getName().toLowerCase().contains(filter.trim().toLowerCase()))
                         .collect(Collectors.toList());
    }
    Collections.sort(applications);

    return PageUtils.offsetAndLimit(applications, offset, pageSize);
  }

  @Override
  public PageResponse<Tier> getTiers(String accountId, String connectorIdentifier, String orgIdentifier,
      String projectIdentifier, String appName, int offset, int pageSize, String filter) {
    DataCollectionRequest request = AppDynamicsFetchTiersRequest.builder()
                                        .appName(appName)
                                        .type(DataCollectionRequestType.APPDYNAMICS_FETCH_TIERS)
                                        .build();

    OnboardingRequestDTO onboardingRequestDTO = OnboardingRequestDTO.builder()
                                                    .dataCollectionRequest(request)
                                                    .connectorIdentifier(connectorIdentifier)
                                                    .accountId(accountId)
                                                    .orgIdentifier(orgIdentifier)
                                                    .projectIdentifier(projectIdentifier)
                                                    .build();

    OnboardingResponseDTO response = onboardingService.getOnboardingResponse(accountId, onboardingRequestDTO);

    final Gson gson = new Gson();
    Type type = new TypeToken<List<Tier>>() {}.getType();
    List<Tier> tiers = gson.fromJson(JsonUtils.asJson(response.getResult()), type);
    List<Tier> appDynamicsTiers = new ArrayList<>();
    tiers.forEach(appDynamicsTier -> {
      if (isEmpty(filter) || appDynamicsTier.getName().toLowerCase().contains(filter.trim().toLowerCase())) {
        appDynamicsTiers.add(appDynamicsTier);
      }
    });
    Collections.sort(appDynamicsTiers);
    return PageUtils.offsetAndLimit(appDynamicsTiers, offset, pageSize);
  }

  @Override
  public void checkConnectivity(
      String accountId, String orgIdentifier, String projectIdentifier, String connectorIdentifier, String tracingId) {
    getApplications(accountId, connectorIdentifier, orgIdentifier, projectIdentifier, 0, 1, null);
  }

  @Override
  public MonitoringSourceImportStatus createMonitoringSourceImportStatus(
      List<CVConfig> cvConfigsGroupedByMonitoringSource, int totalNumberOfEnvironments) {
    Preconditions.checkState(
        isNotEmpty(cvConfigsGroupedByMonitoringSource), "The cv configs belonging to a monitoring source is empty");
    Set<String> applicationSet = cvConfigsGroupedByMonitoringSource.stream()
                                     .map(config -> ((AppDynamicsCVConfig) config).getApplicationName())
                                     .collect(Collectors.toSet());
    Set<String> envIdentifiersList =
        cvConfigsGroupedByMonitoringSource.stream().map(CVConfig::getEnvIdentifier).collect(Collectors.toSet());
    CVConfig firstCVConfigForReference = cvConfigsGroupedByMonitoringSource.get(0);

    List<AppDynamicsApplication> appDynamicsApplications = getApplications(firstCVConfigForReference.getAccountId(),
        firstCVConfigForReference.getConnectorIdentifier(), firstCVConfigForReference.getOrgIdentifier(),
        firstCVConfigForReference.getProjectIdentifier(), 0, Integer.MAX_VALUE, null)
                                                               .getContent();

    return AppdynamicsImportStatus.builder()
        .numberOfApplications(isNotEmpty(applicationSet) ? applicationSet.size() : 0)
        .numberOfEnvironments(isNotEmpty(envIdentifiersList) ? envIdentifiersList.size() : 0)
        .totalNumberOfApplications(isNotEmpty(appDynamicsApplications) ? appDynamicsApplications.size() : 0)
        .totalNumberOfEnvironments(totalNumberOfEnvironments)
        .build();
  }
}
