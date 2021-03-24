package io.harness.cvng.core.services.impl;

import io.harness.cvng.beans.DataCollectionRequest;
import io.harness.cvng.beans.DataCollectionRequestType;
import io.harness.cvng.beans.MetricPackDTO;
import io.harness.cvng.beans.ThirdPartyApiResponseStatus;
import io.harness.cvng.beans.newrelic.NewRelicApplication;
import io.harness.cvng.beans.newrelic.NewRelicApplicationFetchRequest;
import io.harness.cvng.beans.newrelic.NewRelicMetricPackValidationRequest;
import io.harness.cvng.core.beans.MetricPackValidationResponse;
import io.harness.cvng.core.beans.MetricPackValidationResponse.MetricValidationResponse;
import io.harness.cvng.core.beans.MonitoringSourceImportStatus;
import io.harness.cvng.core.beans.OnboardingRequestDTO;
import io.harness.cvng.core.beans.OnboardingResponseDTO;
import io.harness.cvng.core.beans.stackdriver.StackdriverDashboardDetail;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.services.api.NewRelicService;
import io.harness.cvng.core.services.api.OnboardingService;
import io.harness.datacollection.exception.DataCollectionException;
import io.harness.serializer.JsonUtils;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.inject.Inject;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.assertj.core.util.Sets;

public class NewRelicServiceImpl implements NewRelicService {
  private static final List<String> NEW_RELIC_ENDPOINTS =
      Arrays.asList("https://insights-api.newrelic.com/", "https://insights-api.eu.newrelic.com/");

  @Inject private OnboardingService onboardingService;

  @Override
  public List<String> getNewRelicEndpoints() {
    return NEW_RELIC_ENDPOINTS;
  }

  @Override
  public List<NewRelicApplication> getNewRelicApplications(String accountId, String connectorIdentifier,
      String orgIdentifier, String projectIdentifier, String filter, String tracingId) {
    DataCollectionRequest request = NewRelicApplicationFetchRequest.builder()
                                        .type(DataCollectionRequestType.NEWRELIC_APPS_REQUEST)
                                        .filter(filter)
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
    Type type = new TypeToken<List<NewRelicApplication>>() {}.getType();
    List<NewRelicApplication> newRelicApplications = gson.fromJson(JsonUtils.asJson(response.getResult()), type);
    return newRelicApplications;
  }

  @Override
  public MetricPackValidationResponse validateData(String accountId, String connectorIdentifier, String orgIdentifier,
      String projectIdentifier, String appName, String appId, List<MetricPackDTO> metricPacks) {
    try {
      DataCollectionRequest request = NewRelicMetricPackValidationRequest.builder()
                                          .type(DataCollectionRequestType.NEWRELIC_VALIDATION_REQUEST)
                                          .applicationName(appName)
                                          .applicationId(appId)
                                          .metricPackDTOSet(Sets.newHashSet(metricPacks))
                                          .build();
      OnboardingRequestDTO onboardingRequestDTO = OnboardingRequestDTO.builder()
                                                      .dataCollectionRequest(request)
                                                      .connectorIdentifier(connectorIdentifier)
                                                      .accountId(accountId)
                                                      .orgIdentifier(orgIdentifier)
                                                      .tracingId("")
                                                      .projectIdentifier(projectIdentifier)
                                                      .build();

      OnboardingResponseDTO response = onboardingService.getOnboardingResponse(accountId, onboardingRequestDTO);
      final Gson gson = new Gson();
      Type type = new TypeToken<List<MetricValidationResponse>>() {}.getType();
      List<MetricValidationResponse> metricValidationResponseList =
          gson.fromJson(JsonUtils.asJson(response.getResult()), type);
      MetricPackValidationResponse validationResponse = MetricPackValidationResponse.builder()
                                                            .overallStatus(ThirdPartyApiResponseStatus.SUCCESS)
                                                            .metricValidationResponses(metricValidationResponseList)
                                                            .build();
      validationResponse.updateStatus();
      return validationResponse;
    } catch (DataCollectionException ex) {
      return MetricPackValidationResponse.builder().overallStatus(ThirdPartyApiResponseStatus.FAILED).build();
    }
  }

  @Override
  public MonitoringSourceImportStatus createMonitoringSourceImportStatus(
      List<CVConfig> cvConfigsGroupedByMonitoringSource, int totalNumberOfEnvironments) {
    // TODO: Is this even needed ? Nothing in the mock.
    return null;
  }
}
