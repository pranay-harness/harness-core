package io.harness.cvng.core.services.impl;

import io.harness.cvng.beans.DataCollectionRequest;
import io.harness.cvng.beans.DataCollectionRequestType;
import io.harness.cvng.beans.dynatrace.DynatraceServiceListRequest;
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
import java.util.List;

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
        DynatraceServiceListRequest.builder().type(DataCollectionRequestType.DYNATRACE_SERVICE_DETAILS).build();

    Type type = new TypeToken<DynatraceServiceDTO>() {}.getType();
    return performRequestAndGetDataResult(request, type, projectParams, connectorIdentifier, tracingId);
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
