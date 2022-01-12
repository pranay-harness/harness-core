package io.harness.cvng.core.services.impl;

import io.harness.cvng.beans.DataCollectionRequest;
import io.harness.cvng.beans.DataCollectionRequestType;
import io.harness.cvng.beans.MetricPackDTO;
import io.harness.cvng.beans.ThirdPartyApiResponseStatus;
import io.harness.cvng.beans.dynatrace.DynatraceMetricPackValidationRequest;
import io.harness.cvng.beans.dynatrace.DynatraceServiceDetailsRequest;
import io.harness.cvng.beans.dynatrace.DynatraceServiceListRequest;
import io.harness.cvng.core.beans.MetricPackValidationResponse;
import io.harness.cvng.core.beans.OnboardingRequestDTO;
import io.harness.cvng.core.beans.OnboardingResponseDTO;
import io.harness.cvng.core.beans.dynatrace.DynatraceServiceDTO;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.services.api.DynatraceService;
import io.harness.cvng.core.services.api.OnboardingService;
import io.harness.delegate.beans.connector.dynatrace.DynatraceConnectorDTO;
import io.harness.serializer.JsonUtils;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.inject.Inject;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DynatraceServiceImpl implements DynatraceService {
  @Inject private OnboardingService onboardingService;

  @Override
  public List<DynatraceServiceDTO> getAllServices(
      ProjectParams projectParams, String connectorIdentifier, String filter, String tracingId) {
    DataCollectionRequest<DynatraceConnectorDTO> request =
        DynatraceServiceListRequest.builder().type(DataCollectionRequestType.DYNATRACE_SERVICE_LIST).build();

    Type type = new TypeToken<List<DynatraceServiceDTO>>() {}.getType();
    return performRequestAndGetDataResult(request, type, projectParams, connectorIdentifier, tracingId);
  }

  @Override
  public DynatraceServiceDTO getServiceDetails(
      ProjectParams projectParams, String connectorIdentifier, String serviceEntityId, String tracingId) {
    DataCollectionRequest<DynatraceConnectorDTO> request =
        DynatraceServiceDetailsRequest.builder()
            .serviceId(serviceEntityId)
            .type(DataCollectionRequestType.DYNATRACE_SERVICE_DETAILS)
            .build();

    Type type = new TypeToken<DynatraceServiceDTO>() {}.getType();
    return performRequestAndGetDataResult(request, type, projectParams, connectorIdentifier, tracingId);
  }

  @Override
  public Set<MetricPackValidationResponse> validateData(ProjectParams projectParams, String connectorIdentifier,
      String serviceId, List<MetricPackDTO> metricPacks, String tracingId) {
    Set<MetricPackValidationResponse> metricPackValidationResponses = new HashSet<>();
    metricPacks.forEach(metricPack -> {
      DataCollectionRequest<DynatraceConnectorDTO> request =
          DynatraceMetricPackValidationRequest.builder()
              .serviceId(serviceId)
              .metricPack(metricPack)
              .type(DataCollectionRequestType.DYNATRACE_VALIDATION_REQUEST)
              .build();

      Type type = new TypeToken<List<MetricPackValidationResponse.MetricValidationResponse>>() {}.getType();
      List<MetricPackValidationResponse.MetricValidationResponse> validationResponses =
          performRequestAndGetDataResult(request, type, projectParams, connectorIdentifier, tracingId);

      MetricPackValidationResponse.MetricPackValidationResponseBuilder metricPackValidationResponseBuilder =
          MetricPackValidationResponse.builder()
              .metricPackName(metricPack.getIdentifier())
              .metricValidationResponses(validationResponses);

      MetricPackValidationResponse metricPackValidationResponse =
          metricPackValidationResponseBuilder.overallStatus(ThirdPartyApiResponseStatus.SUCCESS).build();
      metricPackValidationResponse.updateStatus();
      metricPackValidationResponses.add(metricPackValidationResponse);
    });

    return metricPackValidationResponses;
  }

  @Override
  public void checkConnectivity(
      String accountId, String orgIdentifier, String projectIdentifier, String connectorIdentifier, String tracingId) {
    getAllServices(ProjectParams.builder()
                       .accountIdentifier(accountId)
                       .orgIdentifier(orgIdentifier)
                       .projectIdentifier(projectIdentifier)
                       .build(),
        connectorIdentifier, null, tracingId);
  }

  private <T> T performRequestAndGetDataResult(DataCollectionRequest<DynatraceConnectorDTO> dataCollectionRequest,
      Type type, ProjectParams projectParams, String connectorIdentifier, String tracingId) {
    OnboardingRequestDTO onboardingRequestDTO = OnboardingRequestDTO.builder()
                                                    .dataCollectionRequest(dataCollectionRequest)
                                                    .connectorIdentifier(connectorIdentifier)
                                                    .accountId(projectParams.getAccountIdentifier())
                                                    .orgIdentifier(projectParams.getOrgIdentifier())
                                                    .projectIdentifier(projectParams.getProjectIdentifier())
                                                    .tracingId(tracingId)
                                                    .build();

    OnboardingResponseDTO response =
        onboardingService.getOnboardingResponse(projectParams.getAccountIdentifier(), onboardingRequestDTO);
    return new Gson().fromJson(JsonUtils.asJson(response.getResult()), type);
  }
}
